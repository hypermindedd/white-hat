import test from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {Client} from 'pg';
import {createMemorySessionStore,createPostgresSessionStore} from '../session-store.mjs';

const ttl=8*60*60*1000;
const id=()=>randomBytes(32).toString('hex');
const key=()=>randomBytes(32).toString('base64');
const session=now=>({created:now,access:'synthetic-access',refresh:'synthetic-refresh',generation:0});
const read=(store,sid,now)=>store.transact(sid,now,async s=>({session:s,value:s}));

test('Local session fixture consumes login state once and expires at the boundary',async()=>{
 const store=createMemorySessionStore(),sid=id(),now=1000;
 await store.createFlow(sid,{expires:now+10},now);
 const results=await Promise.all([store.takeFlow(sid,now),store.takeFlow(sid,now)]);
 assert.equal(results.filter(Boolean).length,1);
 await store.createFlow(sid,{expires:now+10},now);
 assert.equal(await store.takeFlow(sid,now+10),undefined);
 await store.createSession(sid,session(now),now);
 assert.ok(await read(store,sid,now+ttl-1));
 assert.equal(await read(store,sid,now+ttl),undefined);
});

test('Local session fixture serializes updates and recovers after a rejected update',async()=>{
 const store=createMemorySessionStore(),sid=id(),now=1000;
 await store.createSession(sid,session(now),now);
 await Promise.all(Array.from({length:8},()=>store.transact(sid,now,async s=>{
  await Promise.resolve();
  return {session:{...s,generation:s.generation+1}};
 })));
 assert.equal((await read(store,sid,now)).generation,8);
 await assert.rejects(store.transact(sid,now,async()=>{throw new Error('synthetic failure');}));
 assert.equal((await read(store,sid,now)).generation,8);
 await store.transact(sid,now,async()=>({value:'revoked'}));
 assert.equal(await read(store,sid,now),undefined);
});

const database=process.env.WH_SESSION_TEST_DB;
test('PostgreSQL session security across independent service instances',
 {skip:!database},async t=>{
 const admin=new Client({connectionString:database});
 await admin.connect();
 const schema='session_test_'+randomBytes(8).toString('hex');
 const stores=[];
 t.after(async()=>{
  try {await Promise.all(stores.map(store=>store.close()));}
  finally {try {await admin.query(`DROP SCHEMA IF EXISTS ${schema} CASCADE`);} finally {await admin.end();}}
 });
 await admin.query(`CREATE SCHEMA ${schema}`);
 await admin.query(`SET search_path TO ${schema}`);
 await admin.query(readFileSync(new URL('../db/session-store.sql',import.meta.url),'utf8'));
 const url=new URL(database);
 url.searchParams.set('options','-c search_path='+schema);
 const oldKey=key(),newKey=key();
 const open=keys=>{const store=createPostgresSessionStore(url.toString(),keys);stores.push(store);return store;};
 const first=open(oldKey),second=open(oldKey),now=1000;

 await t.test('login state is encrypted and atomically consumed across instances',async()=>{
  const sid=id();
  await first.createFlow(sid,{expires:now+100,verifier:'synthetic-pkce'},now);
  const raw=(await admin.query('SELECT payload FROM wh_web_login_flows WHERE id=$1',[sid])).rows[0].payload;
  assert.equal(raw.includes('synthetic-pkce'),false);
  const values=await Promise.all([first.takeFlow(sid,now),second.takeFlow(sid,now)]);
  assert.equal(values.filter(Boolean).length,1);
  assert.equal(values.find(Boolean).verifier,'synthetic-pkce');
  await first.createFlow(sid,{expires:now},now);
  assert.equal(await second.takeFlow(sid,now),undefined);
 });
 await t.test('concurrent token updates are serialized with no lost rotation',async()=>{
  const sid=id();await first.createSession(sid,session(now),now);
  await Promise.all(Array.from({length:12},(_,i)=>(i%2?first:second).transact(sid,now,async s=>{
   await Promise.resolve();return {session:{...s,generation:s.generation+1}};
  })));
  assert.equal((await read(second,sid,now)).generation,12);
 });
 await t.test('restart reads persisted state and revocation reaches every instance',async()=>{
  const sid=id();await first.createSession(sid,session(now),now);
  const restarted=open(oldKey);
  assert.equal((await read(restarted,sid,now)).access,'synthetic-access');
  await second.transact(sid,now,async()=>({value:'revoked'}));
  assert.equal(await read(first,sid,now),undefined);
  assert.equal(await read(restarted,sid,now),undefined);
 });
 await t.test('key rollover reads old ciphertext then rewrites under the new key',async()=>{
  const sid=id();await first.createSession(sid,session(now),now);
  const rotating=open(newKey+','+oldKey),newOnly=open(newKey);
  assert.equal((await read(rotating,sid,now)).generation,0);
  await assert.rejects(read(newOnly,sid,now),/SESSION_PAYLOAD_INVALID/);
  await rotating.transact(sid,now,async s=>({session:{...s,generation:1}}));
  assert.equal((await read(newOnly,sid,now)).generation,1);
  await assert.rejects(read(first,sid,now),/SESSION_PAYLOAD_INVALID/);
 });
 await t.test('tampered and transplanted ciphertext fails closed',async()=>{
  const a=id(),b=id();
  await first.createSession(a,session(now),now);await first.createSession(b,session(now),now);
  const raw=(await admin.query('SELECT payload FROM wh_web_sessions WHERE id=$1',[a])).rows[0].payload;
  assert.equal(raw.includes('synthetic-access'),false);
  await admin.query('UPDATE wh_web_sessions SET payload=$1 WHERE id=$2',[raw,b]);
  await assert.rejects(read(second,b,now),/SESSION_PAYLOAD_INVALID/);
  const bytes=Buffer.from(raw,'base64');bytes[15]^=1;
  await admin.query('UPDATE wh_web_sessions SET payload=$1 WHERE id=$2',[bytes.toString('base64'),a]);
  await assert.rejects(read(second,a,now),/SESSION_PAYLOAD_INVALID/);
 });
 await t.test('expiry, failed transaction rollback and shared abuse limit',async()=>{
  const sid=id();await first.createSession(sid,session(now),now);
  await assert.rejects(first.transact(sid,now,async()=>{throw new Error('synthetic failure');}));
  assert.equal((await read(second,sid,now+ttl-1)).generation,0);
  assert.equal(await read(first,sid,now+ttl),undefined);
  const bucket=id();
  const results=await Promise.all(Array.from({length:12},(_,i)=>(i%2?first:second).allowRequest(bucket,now,5)));
  assert.equal(results.filter(Boolean).length,5);
  assert.equal(await second.allowRequest(bucket,60000,5),true);
 });
});
