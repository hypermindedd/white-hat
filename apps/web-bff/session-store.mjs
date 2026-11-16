import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';
import pg from 'pg';

const loginLimit = 500;
const validId = /^[a-f0-9]{64}$/;
const sessionTtl = 8 * 60 * 60 * 1000;

export function createMemorySessionStore() {
  const flows = new Map(), sessions = new Map(), chains = new Map();
  return {
    async createFlow(id,flow,now) {
      for (const [key,value] of flows) if (value.expires<=now) flows.delete(key);
      if (flows.size>=loginLimit) return false;
      flows.set(id,flow); return true;
    },
    async takeFlow(id,now) {
      const flow=flows.get(id); flows.delete(id);
      return flow?.expires>now ? flow : undefined;
    },
    async createSession(id,session,now) {
      for (const [key,value] of sessions) if (value.created+sessionTtl<=now) sessions.delete(key);
      sessions.set(id,session);
    },
    async transact(id,now,update) {
      const previous=chains.get(id) || Promise.resolve();
      const next=previous.catch(()=>{}).then(async()=>{
        const session=sessions.get(id);
        const result=await update(session?.created+sessionTtl>now ? {...session} : undefined);
        if (result.session) sessions.set(id,result.session);
        else sessions.delete(id);
        return result.value;
      });
      chains.set(id,next);
      try { return await next; } finally { if(chains.get(id)===next) chains.delete(id); }
    }
  };
}

export function createPostgresSessionStore(connectionString, encryptedKeys) {
  const encodedKeys=encryptedKeys.split(',');
  const keys=encodedKeys.map(value=>Buffer.from(value,'base64'));
  if (keys.some((key,index)=>key.length!==32 || key.toString('base64')!==encodedKeys[index])) throw new Error('SESSION_KEYS_INVALID');
  const pool=new pg.Pool({connectionString,max:8,connectionTimeoutMillis:3000,
    statement_timeout:14000,idle_in_transaction_session_timeout:35000});
  // A dropped idle socket is discarded by the pool. The next operation still fails closed.
  pool.on('error',()=>{});
  const aad=(kind,id)=>Buffer.from('whitehat-web-session:v1:'+kind+':'+id);
  function seal(kind,id,value) {
    const nonce=randomBytes(12),cipher=createCipheriv('aes-256-gcm',keys[0],nonce);
    cipher.setAAD(aad(kind,id));
    return Buffer.concat([nonce,cipher.update(JSON.stringify(value)),cipher.final(),cipher.getAuthTag()]).toString('base64');
  }
  function open(kind,id,raw) {
    const bytes=Buffer.from(raw,'base64');
    for(const key of keys) {
      try {
        const decipher=createDecipheriv('aes-256-gcm',key,bytes.subarray(0,12));
        decipher.setAAD(aad(kind,id)); decipher.setAuthTag(bytes.subarray(-16));
        return JSON.parse(Buffer.concat([decipher.update(bytes.subarray(12,-16)),decipher.final()]).toString());
      } catch {}
    }
    throw new Error('SESSION_PAYLOAD_INVALID');
  }
  async function transaction(block) {
    const client=await pool.connect();
    let broken=false;
    try {
      await client.query('BEGIN');
      const result=await block(client);
      await client.query('COMMIT');
      return result;
    } catch(error) {
      try { await client.query('ROLLBACK'); } catch { broken=true; }
      throw error;
    } finally { client.release(broken); }
  }
  return {
    close:()=>pool.end(),
    async allowRequest(id,now,limit) {
      const end=now-now%60000+60000;
      const result=await pool.query(`INSERT INTO wh_web_rate_windows VALUES ($1,$2,1)
        ON CONFLICT(id) DO UPDATE SET attempts=CASE
          WHEN wh_web_rate_windows.expires_at < EXCLUDED.expires_at THEN 1
          ELSE wh_web_rate_windows.attempts+1 END,
          expires_at=EXCLUDED.expires_at
        WHERE wh_web_rate_windows.expires_at<EXCLUDED.expires_at OR
          (wh_web_rate_windows.expires_at=EXCLUDED.expires_at AND wh_web_rate_windows.attempts<$3)`,
      [id,end,limit]);
      return result.rowCount===1;
    },
    createFlow:(id,flow,now)=>transaction(async client=>{
      await client.query('SELECT pg_advisory_xact_lock(90502117)');
      await client.query('DELETE FROM wh_web_rate_windows WHERE expires_at <= $1',[now]);
      await client.query('DELETE FROM wh_web_login_flows WHERE expires_at <= $1',[now]);
      const {rows:[row]}=await client.query('SELECT count(*)::int AS count FROM wh_web_login_flows');
      if(row.count>=loginLimit)return false;
      await client.query('INSERT INTO wh_web_login_flows VALUES ($1,$2,$3)',[id,flow.expires,seal('flow',id,flow)]);
      return true;
    }),
    async takeFlow(id,now) {
      const {rows:[row]}=await pool.query('DELETE FROM wh_web_login_flows WHERE id=$1 RETURNING *',[id]);
      return row?.expires_at>now ? open('flow',id,row.payload) : undefined;
    },
    createSession:(id,session,now)=>transaction(async client=>{
      await client.query('DELETE FROM wh_web_sessions WHERE expires_at <= $1',[now]);
      await client.query('INSERT INTO wh_web_sessions VALUES ($1,$2,$3)',[id,session.created+sessionTtl,seal('session',id,session)]);
    }),
    transact:(id,now,update)=>transaction(async client=>{
      if (!validId.test(id)) throw new Error('SESSION_ID_INVALID');
      const {rows:[row]}=await client.query('SELECT * FROM wh_web_sessions WHERE id=$1 FOR UPDATE',[id]);
      const session=row?.expires_at>now ? open('session',id,row.payload) : undefined;
      const result=await update(session);
      if(row && !result.session) await client.query('DELETE FROM wh_web_sessions WHERE id=$1',[id]);
      if(result.session && result.session!==session) {
        await client.query('UPDATE wh_web_sessions SET payload=$2 WHERE id=$1',[id,seal('session',id,result.session)]);
      }
      return result.value;
    })
  };
}
