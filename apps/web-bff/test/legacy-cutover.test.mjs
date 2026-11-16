import test from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {copyFileSync,mkdtempSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {execFileSync} from 'node:child_process';
import {Client} from 'pg';
import {openStore} from '../../second-mind/server/store.mjs';
import {applyLegacyRehearsal,planLegacyCutover} from '../tools/legacy-cutover.mjs';

function fixture(t){
 const dir=mkdtempSync(join(tmpdir(),'legacy-whitehat-'));
 t.after(()=>rmSync(dir,{recursive:true,force:true}));
 const db=openStore(dir);
 const users=[randomUUID(),randomUUID(),randomUUID()], decisions=users.map(()=>randomUUID());
 const stamp='2026-01-01T00:00:00Z';
 const user=db.prepare('INSERT INTO users VALUES(?,?,?,?,?,?,?)');
 const decision=db.prepare('INSERT INTO challenges VALUES(?,?,?,?,?,?,?,?,?,?,?)');
 const run=db.prepare('INSERT INTO runs(id,challenge_id,user_id,idempotency_key,request_hash,status,engine_version,core_hash,day_key,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)');
 for(const [index,id] of users.entries()){
  user.run(id,'synthetic-'+index,'Synthetic','credential-placeholder','member',index===1?0:1,stamp);
  decision.run(decisions[index],id,'Synthetic','Private synthetic','Objective','Context','Notes','challenge','draft',stamp,stamp);
  run.run(randomUUID(),decisions[index],id,'synthetic-key','hash','completed','legacy','hash','2026-01-01',stamp);
 }
 db.exec('PRAGMA wal_checkpoint(TRUNCATE)');db.close();
 const path=join(dir,'snapshot.sqlite');
 copyFileSync(join(dir,'second-mind.sqlite'),path);
 return {dir,path,manifest:{workspace:{id:randomUUID(),name:'C2 isolated rehearsal'},
  accounts:[{legacy_id:users[0],subject:'synthetic-owner',role:'OWNER'},
            {legacy_id:users[1],subject:'synthetic-member',role:'MEMBER'}]}};
}

test('Legacy cutover accounts, decisions and historical runs reconcile without credentials',t=>{
 const {path,manifest}=fixture(t);
 const plan=planLegacyCutover(path,manifest);
 assert.deepEqual(plan.counts,{source:{accounts:3,decisions:3,runs:3},accepted:{accounts:2,decisions:2,archivedRuns:2}});
 assert.deepEqual(plan.quarantine.map(r=>r.entity),['account','decision','run']);
 assert.equal(plan.memberships[1].active,false);
 assert.equal(JSON.stringify(plan).includes('credential-placeholder'),false);
 assert.equal(plan.archivedRuns[0].legacy_status,'completed');
});

test('Legacy cutover rejects an ambiguous identity mapping and missing owner',t=>{
 const {path,manifest}=fixture(t);
 const accounts=manifest.accounts.map(row=>({...row,subject:'same-id'}));
 assert.throws(()=>planLegacyCutover(path,{...manifest,accounts}),/one-to-one/);
 assert.throws(()=>planLegacyCutover(path,{...manifest,accounts:manifest.accounts.slice(1)}),/active OWNER/);
 const twoOwners=manifest.accounts.map(row=>({...row,role:'OWNER'}));
 assert.throws(()=>planLegacyCutover(path,{...manifest,accounts:twoOwners}),/active OWNER/);
});

const origin=process.env.WH_C2_ORIGIN_DB, restored=process.env.WH_C2_RESTORED_DB, pg=process.env.WH_C2_PG_BIN;
test('Isolated PostgreSQL rehearsal preserves committed changes and tenant boundaries', {skip:!origin||!restored||!pg}, async t=>{
 const {path,manifest,dir}=fixture(t);
 const plan=planLegacyCutover(path,manifest);
 const source=new Client({connectionString:origin}), target=new Client({connectionString:restored});
 const workspace=plan.workspace.id;
 await source.connect();await target.connect();
 t.after(()=>Promise.all([source.end(),target.end()]));
 const visible=async c=>(await c.query('SELECT id, revision, objective FROM decisions WHERE workspace_id=$1 ORDER BY id',[workspace])).rows;
 await source.query('BEGIN');await applyLegacyRehearsal(source,plan);
 assert.equal((await visible(source)).length,2);
 await source.query('ROLLBACK');
 assert.equal((await visible(source)).length,0);
 await source.query('BEGIN');await applyLegacyRehearsal(source,plan);await source.query('COMMIT');
 const d=plan.decisions[0].id;
 await source.query('UPDATE decisions SET objective=$1,revision=2 WHERE workspace_id=$2 AND id=$3',['Post-cutover write',workspace,d]);
 const dump=join(dir,'after-write.dump');
 execFileSync(join(pg,'pg_dump'),['--format=custom','--file='+dump,origin]);
 execFileSync(join(pg,'pg_restore'),['--exit-on-error','--single-transaction','--no-owner','--no-privileges','--dbname='+restored,dump]);
 assert.deepEqual(await visible(target),await visible(source));
 assert.equal((await visible(target)).find(row=>row.id===d).revision,'2');
 assert.equal((await target.query('SELECT count(*)::int n FROM memberships WHERE workspace_id=$1',[workspace])).rows[0].n,2);
 assert.equal((await target.query('SELECT count(*)::int n FROM runs WHERE workspace_id=$1',[workspace])).rows[0].n,0);
 await target.query('CREATE ROLE rehearsal_reader NOLOGIN NOBYPASSRLS; GRANT SELECT ON decisions TO rehearsal_reader');
 await target.query('BEGIN');await target.query('SET LOCAL ROLE rehearsal_reader');
 await target.query("SELECT set_config('whitehat.workspace',$1,true)",[randomUUID()]);
 assert.equal((await visible(target)).length,0);
 await target.query("SELECT set_config('whitehat.workspace',$1,true)",[workspace]);
 assert.equal((await visible(target)).length,2);
 await target.query('ROLLBACK');
 assert.equal(planLegacyCutover(path,manifest).sourceSha256,plan.sourceSha256);
 t.diagnostic(JSON.stringify({source:plan.counts.source,accepted:plan.counts.accepted,quarantined:plan.quarantine.length,
  rollback_after_insert:true,restore_after_committed_write:true,independent_clusters:true,rls:true}));
});
