import http from 'node:http';
import {randomUUID} from 'node:crypto';
import {readFileSync,existsSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname,join,extname} from 'node:path';
import {openStore} from './store.mjs';
import {hashPassword,verifyPassword,createSession,sessionFrom,sha} from './auth.mjs';
import {MODES,VERSION,coreHash,validateOutput,policy} from './core/engine.mjs';
import {OpenAIProvider} from './provider.mjs';

const root=dirname(dirname(fileURLToPath(import.meta.url)));
const now=()=>new Date().toISOString();
const fail=(status,code)=>Object.assign(new Error(code),{status});
const publicUser=u=>({id:u.id,username:u.username,display_name:u.display_name,role:u.role,...('active'in u?{active:Boolean(u.active),created_at:u.created_at}:{})});
const fields=['title','decision','objective','context','source_notes','mode'];
function text(value,max,required=false){if(typeof value!=='string'||value.length>max||(required&&!value.trim()))throw fail(400,'INVALID_INPUT');return value.trim();}
function validateChallenge(body){
 const output={title:text(body.title||'',120),decision:text(body.decision,16000,true),objective:text(body.objective||'',2000),context:text(body.context||'',12000),source_notes:text(body.source_notes||'',10000),mode:body.mode||'challenge'};
 if(!MODES.some(m=>m.id===output.mode))throw fail(400,'INVALID_MODE');
 if(!output.title)output.title=output.decision.slice(0,70);
 return output;
}
function dayKey(){return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Tehran',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());}

export function createApplication({dataDir,provider=new OpenAIProvider({apiKey:process.env.OPENAI_API_KEY,model:process.env.SECOND_MIND_MODEL}),publicOrigin=process.env.APP_ORIGIN||'',secureCookies=false}={}){
 const db=openStore(dataDir),jobs=new Set(),loginAttempts=new Map();
 if(!provider.model)provider.model=db.prepare("SELECT value FROM settings WHERE key='model'").get()?.value||'';
 const audit=(actor,action,target=null,metadata={})=>db.prepare('INSERT INTO audit VALUES(?,?,?,?,?,?)').run(randomUUID(),actor,action,target,JSON.stringify(metadata),now());
 const json=(res,status,value,headers={})=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store',...headers});res.end(JSON.stringify(value));};
 const cookie=token=>`sm_session=${token}; Path=/; HttpOnly; SameSite=Strict; Max-Age=43200${secureCookies?'; Secure':''}`;
 async function body(req){let n=0,parts=[];for await(const part of req){n+=part.length;if(n>140000)throw fail(413,'INPUT_TOO_LARGE');parts.push(part);}try{const value=JSON.parse(Buffer.concat(parts).toString()||'{}');if(!value||typeof value!=='object'||Array.isArray(value))throw new Error();return value;}catch{throw fail(400,'INVALID_JSON');}}
 function originAllowed(req){
  const host=req.headers.host||'',origin=req.headers.origin;
  const configured=typeof publicOrigin==='function'?publicOrigin():publicOrigin;
  if(configured)return origin===new URL(configured).origin;
  if(!/^(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/.test(host))return false;
  return origin===`http://${host}`;
 }
 function requireOwner(user,id){const item=db.prepare('SELECT * FROM challenges WHERE id=? AND user_id=?').get(id,user.id);if(!item)throw fail(404,'NOT_FOUND');return item;}
 function challengeDetail(user,id){
  const challenge=requireOwner(user,id);
  const runs=db.prepare('SELECT * FROM runs WHERE challenge_id=? ORDER BY created_at,id').all(id).map(r=>({...r,result:r.result_json?JSON.parse(r.result_json):null,result_json:undefined,request_hash:undefined,idempotency_key:undefined}));
  const messages=db.prepare('SELECT id,role,content,created_at FROM messages WHERE challenge_id=? ORDER BY created_at,rowid').all(id);
  return {...challenge,runs,messages,feedback:db.prepare('SELECT useful,note FROM feedback WHERE challenge_id=?').get(id)||null};
 }
 function requireAdmin(user){if(user.role!=='admin')throw fail(403,'ADMIN_REQUIRED');}
 const settings=()=>({daily_limit:Number(db.prepare("SELECT value FROM settings WHERE key='daily_limit'").get().value),model:provider.model||'',provider_configured:provider.configured(),key_storage:'server_memory_or_environment',web_retrieval_enabled:false,attachment_extraction_enabled:false});

 async function launchRun(user,challenge,key,followup=''){
  if(!/^[A-Za-z0-9_-]{8,120}$/.test(key||''))throw fail(400,'IDEMPOTENCY_KEY_REQUIRED');
  const requestHash=sha(JSON.stringify({id:challenge.id,decision:challenge.decision,objective:challenge.objective,context:challenge.context,source_notes:challenge.source_notes,mode:challenge.mode,followup}));
  const existing=db.prepare('SELECT * FROM runs WHERE user_id=? AND idempotency_key=?').get(user.id,key);
  if(existing){if(existing.request_hash!==requestHash)throw fail(409,'IDEMPOTENCY_CONFLICT');return {run_id:existing.id,status:existing.status,reused:true};}
  if(!provider.configured())throw fail(409,'PROVIDER_NOT_CONFIGURED');
  if(challenge.status==='running')throw fail(409,'ANALYSIS_ALREADY_RUNNING');
  if(challenge.mode==='compare'&&!challenge.objective.trim())throw fail(400,'COMPARE_OBJECTIVE_REQUIRED');
  const consumed=db.prepare("SELECT COUNT(*) AS count FROM runs WHERE user_id=? AND day_key=?").get(user.id,dayKey()).count;
  if(consumed>=settings().daily_limit)throw fail(429,'DAILY_LIMIT_REACHED');
  const runId=randomUUID(),created=now();
  db.exec('BEGIN IMMEDIATE');
  try{
   db.prepare('INSERT INTO runs(id,challenge_id,user_id,idempotency_key,request_hash,status,engine_version,core_hash,day_key,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)').run(runId,challenge.id,user.id,key,requestHash,'running',VERSION,coreHash,dayKey(),created);
   db.prepare("UPDATE challenges SET status='running',updated_at=? WHERE id=?").run(created,challenge.id);
   if(followup)db.prepare('INSERT INTO messages VALUES(?,?,?,?,?)').run(randomUUID(),challenge.id,'user',followup,created);
   audit(user.id,'analysis.started',runId,{challenge_id:challenge.id,mode:challenge.mode});db.exec('COMMIT');
  }catch(error){db.exec('ROLLBACK');throw error;}
  const history=db.prepare('SELECT role,content FROM messages WHERE challenge_id=? ORDER BY created_at DESC,rowid DESC LIMIT 12').all(challenge.id).reverse();
  const job=(async()=>{
   try{
    const response=await provider.analyze({challenge,history});
    validateOutput(response.result,challenge.mode);
    const raw=JSON.stringify(response.result),completed=now();
    db.exec('BEGIN IMMEDIATE');
    try{
     db.prepare("UPDATE runs SET status='completed',result_json=?,model=?,provider_request_id=?,input_tokens=?,output_tokens=?,completed_at=? WHERE id=?").run(raw,response.model||provider.model||'unknown',response.provider_request_id||null,response.usage?.input_tokens??null,response.usage?.output_tokens??null,completed,runId);
     db.prepare("UPDATE challenges SET status='completed',updated_at=? WHERE id=?").run(completed,challenge.id);
     db.prepare('INSERT INTO messages VALUES(?,?,?,?,?)').run(randomUUID(),challenge.id,'assistant',raw,completed);
     audit(user.id,'analysis.completed',runId,{mode:challenge.mode});db.exec('COMMIT');
    }catch(error){db.exec('ROLLBACK');throw error;}
   }catch(error){
    const known=['PROVIDER_NOT_CONFIGURED','PROVIDER_RATE_LIMIT','PROVIDER_AUTH_FAILED','PROVIDER_UNAVAILABLE','MODEL_RESPONSE_INCOMPLETE','MODEL_REFUSED','INVALID_MODEL_OUTPUT'];
    const code=known.includes(error.message)?error.message:(error.name==='TimeoutError'?'PROVIDER_TIMEOUT':'ANALYSIS_FAILED');
    db.prepare("UPDATE runs SET status='failed',error_code=?,completed_at=? WHERE id=?").run(code,now(),runId);
    db.prepare("UPDATE challenges SET status='failed',updated_at=? WHERE id=?").run(now(),challenge.id);
    audit(user.id,'analysis.failed',runId,{error_code:code});
   }
  })();
  jobs.add(job);job.finally(()=>jobs.delete(job));
  return {run_id:runId,status:'running',reused:false};
 }

 async function handler(req,res){
  res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','no-referrer');res.setHeader('X-Frame-Options','DENY');
  res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
  const requestId=randomUUID();res.setHeader('X-Request-Id',requestId);
  try{
   const url=new URL(req.url,'http://localhost'),path=url.pathname,method=req.method;
   if(!['GET','HEAD','POST','PATCH','DELETE'].includes(method))throw fail(405,'METHOD_NOT_ALLOWED');
   if(['POST','PATCH','DELETE'].includes(method)&&!originAllowed(req))throw fail(403,'ORIGIN_REJECTED');
   if(path==='/api/health')return json(res,200,{status:'ok',version:VERSION,setup_required:db.prepare('SELECT COUNT(*) AS n FROM users').get().n===0});
   if(path==='/api/setup'&&method==='POST'){
    if(db.prepare('SELECT COUNT(*) AS n FROM users').get().n>0)throw fail(409,'ALREADY_INITIALIZED');
    const b=await body(req),username=text(b.username,32,true).toLowerCase(),name=text(b.display_name,80,true);
    if(!/^[a-z0-9][a-z0-9_.-]{2,31}$/.test(username))throw fail(400,'INVALID_USERNAME');
    const hash=await hashPassword(b.password),id=randomUUID();
    // Hashing yields to the event loop; recheck under transaction to prevent dual first admins.
    db.exec('BEGIN IMMEDIATE');
    try{
     if(db.prepare('SELECT COUNT(*) AS n FROM users').get().n)throw fail(409,'ALREADY_INITIALIZED');
     db.prepare('INSERT INTO users VALUES(?,?,?,?,?,?,?)').run(id,username,name,hash,'admin',1,now());
     audit(id,'workspace.initialized',id);db.exec('COMMIT');
    }catch(error){db.exec('ROLLBACK');throw error;}
    const session=createSession(db,id);
    return json(res,201,{user:{id,username,display_name:name,role:'admin'},csrf:session.csrf},{'Set-Cookie':cookie(session.token)});
   }
   if(path==='/api/login'&&method==='POST'){
    const b=await body(req),username=text(b.username,32,true).toLowerCase(),bucket=`${req.socket.remoteAddress}:${username}`;
    const attempt=loginAttempts.get(bucket)||{n:0,until:Date.now()+15*60*1000};
    if(attempt.until<Date.now()){attempt.n=0;attempt.until=Date.now()+15*60*1000;}
    if(attempt.n>=8)throw fail(429,'LOGIN_RATE_LIMIT');
    attempt.n++;loginAttempts.set(bucket,attempt);
    if(loginAttempts.size>1000)for(const[k,v]of loginAttempts)if(v.until<Date.now())loginAttempts.delete(k);
    const user=db.prepare('SELECT * FROM users WHERE username=? AND active=1').get(username);
    const valid=await verifyPassword(b.password,user?.password_hash||'scrypt:00000000000000000000000000000000:'+ '0'.repeat(128));
    if(!user||!valid)throw fail(401,'INVALID_CREDENTIALS');
    loginAttempts.delete(bucket);const session=createSession(db,user.id);audit(user.id,'session.login',user.id);
    return json(res,200,{user:publicUser(user),csrf:session.csrf},{'Set-Cookie':cookie(session.token)});
   }
   if(path.startsWith('/api/')){
    const user=sessionFrom(db,req);if(!user)throw fail(401,'AUTH_REQUIRED');
    if(['POST','PATCH','DELETE'].includes(method)&&req.headers['x-csrf-token']!==user.csrf)throw fail(403,'CSRF_REJECTED');
    if(path==='/api/me'&&method==='GET')return json(res,200,{user:publicUser(user),csrf:user.csrf,modes:MODES,settings:settings(),version:VERSION});
    if(path==='/api/logout'&&method==='POST'){db.prepare('DELETE FROM sessions WHERE token_hash=?').run(user.token_hash);return json(res,200,{ok:true},{'Set-Cookie':'sm_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0'});}
    if(path==='/api/challenges'&&method==='GET'){
     const q=(url.searchParams.get('q')||'').slice(0,200),mode=url.searchParams.get('mode')||'';
     const items=db.prepare('SELECT id,title,mode,status,created_at,updated_at FROM challenges WHERE user_id=? AND (title LIKE ? OR decision LIKE ?) AND (?=\'\' OR mode=?) ORDER BY updated_at DESC,rowid DESC LIMIT 200').all(user.id,`%${q}%`,`%${q}%`,mode,mode);
     return json(res,200,{items});
    }
    if(path==='/api/challenges'&&method==='POST'){
     const c=validateChallenge(await body(req)),id=randomUUID(),t=now();
     db.prepare('INSERT INTO challenges VALUES(?,?,?,?,?,?,?,?,?,?,?)').run(id,user.id,...fields.map(k=>c[k]),'draft',t,t);
     audit(user.id,'challenge.created',id,{mode:c.mode});return json(res,201,{id});
    }
    const match=path.match(/^\/api\/challenges\/([a-f0-9-]+)(?:\/(analyze|followups|export|feedback|mode))?$/);
    if(match){
     const id=match[1],action=match[2],challenge=requireOwner(user,id);
     if(!action&&method==='GET')return json(res,200,challengeDetail(user,id));
     if(!action&&method==='PATCH'){
      if(challenge.status==='running')throw fail(409,'ANALYSIS_ALREADY_RUNNING');
      const c=validateChallenge({...challenge,...await body(req)});
      // Existing analyses are versioned by run. Input edit invalidates the current completed status.
      db.prepare("UPDATE challenges SET title=?,decision=?,objective=?,context=?,source_notes=?,mode=?,status='draft',updated_at=? WHERE id=?").run(...fields.map(k=>c[k]),now(),id);
      db.prepare('DELETE FROM messages WHERE challenge_id=?').run(id);audit(user.id,'challenge.updated',id,{conversation_reset:true});return json(res,200,{ok:true});
     }
     if(!action&&method==='DELETE'){
      if(challenge.status==='running')throw fail(409,'ANALYSIS_ALREADY_RUNNING');
      db.prepare('DELETE FROM challenges WHERE id=? AND user_id=?').run(id,user.id);audit(user.id,'challenge.deleted',id);return json(res,200,{ok:true});
     }
     if(action==='export'&&method==='GET')return json(res,200,challengeDetail(user,id),{'Content-Disposition':`attachment; filename="second-mind-${id}.json"`});
     if(action==='mode'&&method==='POST'){
      if(challenge.status==='running')throw fail(409,'ANALYSIS_ALREADY_RUNNING');
      const b=await body(req);if(!MODES.some(m=>m.id===b.mode))throw fail(400,'INVALID_MODE');
      if(b.mode===challenge.mode)return json(res,200,{ok:true,unchanged:true});
      db.prepare("UPDATE challenges SET mode=?,status='draft',updated_at=? WHERE id=?").run(b.mode,now(),id);
      audit(user.id,'challenge.mode_changed',id,{from:challenge.mode,to:b.mode});return json(res,200,{ok:true});
     }
     if((action==='analyze'||action==='followups')&&method==='POST'){
      let followup='';if(action==='followups')followup=text((await body(req)).message,10000,true);
      const result=await launchRun(user,challenge,req.headers['idempotency-key'],followup);return json(res,202,result);
     }
     if(action==='feedback'&&method==='POST'){
      const b=await body(req);if(typeof b.useful!=='boolean')throw fail(400,'INVALID_INPUT');
      db.prepare('INSERT INTO feedback VALUES(?,?,?,?,?) ON CONFLICT(challenge_id) DO UPDATE SET useful=excluded.useful,note=excluded.note,created_at=excluded.created_at').run(id,user.id,b.useful?1:0,text(b.note||'',2000),now());return json(res,200,{ok:true});
     }
    }
    if(path==='/api/stats'&&method==='GET'){
     const counts=db.prepare("SELECT COUNT(*) total,SUM(status='completed') completed,SUM(status='draft') drafts,SUM(status='failed') failed FROM challenges WHERE user_id=?").get(user.id);
     const usage=db.prepare('SELECT SUM(input_tokens) input_tokens,SUM(output_tokens) output_tokens,COUNT(*) runs FROM runs WHERE user_id=? AND day_key=?').get(user.id,dayKey());
     return json(res,200,{...counts,...usage,daily_limit:settings().daily_limit,provider_configured:provider.configured(),day_key:dayKey()});
    }
    if(path==='/api/evaluation-policy'&&method==='GET')return json(res,200,policy);
    if(path.startsWith('/api/admin/')){
     requireAdmin(user);
     if(path==='/api/admin/settings'&&method==='GET')return json(res,200,settings());
     if(path==='/api/admin/settings'&&method==='PATCH'){
      const b=await body(req);
      if(Object.keys(b).some(k=>!['daily_limit','model','api_key'].includes(k)))throw fail(400,'INVALID_INPUT');
      if(b.daily_limit!==undefined&&(!Number.isInteger(b.daily_limit)||b.daily_limit<1||b.daily_limit>1000))throw fail(400,'INVALID_INPUT');
      if(b.model!==undefined&&!/^[a-zA-Z0-9._:-]{1,120}$/.test(b.model))throw fail(400,'INVALID_MODEL');
      if(b.api_key!==undefined&&(typeof b.api_key!=='string'||b.api_key.length>1000||/[\r\n]/.test(b.api_key)))throw fail(400,'INVALID_API_KEY');
      if(b.daily_limit!==undefined)db.prepare("UPDATE settings SET value=? WHERE key='daily_limit'").run(String(b.daily_limit));
      if(b.model!==undefined||b.api_key!==undefined){
       provider.configure({model:b.model,apiKey:b.api_key});
       if(b.model!==undefined)db.prepare("INSERT INTO settings VALUES('model',?) ON CONFLICT(key) DO UPDATE SET value=excluded.value").run(b.model);
      }
      audit(user.id,'settings.updated',null,{keys:Object.keys(b).filter(k=>k!=='api_key'),credential_changed:b.api_key!==undefined});return json(res,200,settings());
     }
     if(path==='/api/admin/users'&&method==='GET')return json(res,200,{users:db.prepare('SELECT id,username,display_name,role,active,created_at FROM users ORDER BY created_at').all().map(publicUser)});
     if(path==='/api/admin/users'&&method==='POST'){
      const b=await body(req),username=text(b.username,32,true).toLowerCase();
      if(!/^[a-z0-9][a-z0-9_.-]{2,31}$/.test(username)||!['member','admin'].includes(b.role||'member'))throw fail(400,'INVALID_INPUT');
      const name=text(b.display_name,80,true),hash=await hashPassword(b.password),id=randomUUID();
      try{db.prepare('INSERT INTO users VALUES(?,?,?,?,?,?,?)').run(id,username,name,hash,b.role||'member',1,now());}catch(e){if(String(e.message).includes('UNIQUE'))throw fail(409,'USERNAME_EXISTS');throw e;}
      audit(user.id,'user.created',id);return json(res,201,{id});
     }
     const userMatch=path.match(/^\/api\/admin\/users\/([a-f0-9-]+)$/);
     if(userMatch&&method==='PATCH'){
      const target=db.prepare('SELECT * FROM users WHERE id=?').get(userMatch[1]);if(!target)throw fail(404,'NOT_FOUND');
      const b=await body(req);if(typeof b.active!=='boolean')throw fail(400,'INVALID_INPUT');
      if(target.role==='admin'&&!b.active&&db.prepare("SELECT COUNT(*) n FROM users WHERE role='admin' AND active=1").get().n<=1)throw fail(409,'LAST_ADMIN_REQUIRED');
      db.prepare('UPDATE users SET active=? WHERE id=?').run(b.active?1:0,target.id);
      if(!b.active)db.prepare('DELETE FROM sessions WHERE user_id=?').run(target.id);
      audit(user.id,'user.status_changed',target.id,{active:b.active});return json(res,200,{ok:true});
     }
     if(path==='/api/admin/runs'&&method==='GET')return json(res,200,{runs:db.prepare('SELECT r.id,r.challenge_id,r.status,r.error_code,r.model,r.input_tokens,r.output_tokens,r.created_at,u.display_name,e.weighted_score FROM runs r JOIN users u ON u.id=r.user_id LEFT JOIN evaluations e ON e.run_id=r.id ORDER BY r.created_at DESC,r.rowid DESC LIMIT 100').all()});
     const reviewMatch=path.match(/^\/api\/admin\/runs\/([a-f0-9-]+)$/);
     if(reviewMatch&&method==='GET'){
      const run=db.prepare('SELECT r.*,c.decision,c.objective FROM runs r JOIN challenges c ON c.id=r.challenge_id WHERE r.id=?').get(reviewMatch[1]);
      if(!run)throw fail(404,'NOT_FOUND');
      audit(user.id,'evaluation.output_viewed',run.id);
      return json(res,200,{id:run.id,decision:run.decision,objective:run.objective,result:run.result_json?JSON.parse(run.result_json):null,status:run.status});
     }
     const evalMatch=path.match(/^\/api\/admin\/evaluations\/([a-f0-9-]+)$/);
     if(evalMatch&&method==='POST'){
      const run=db.prepare("SELECT * FROM runs WHERE id=? AND status='completed'").get(evalMatch[1]);if(!run)throw fail(404,'NOT_FOUND');
      const b=await body(req),scores=b.scores,evidence=b.evidence;
      for(const d of policy.dimensions){if(!Number.isInteger(scores?.[d.id])||scores[d.id]<1||scores[d.id]>5||typeof evidence?.[d.id]!=='string'||!evidence[d.id].trim()||evidence[d.id].length>2000)throw fail(400,'SCORE_EVIDENCE_REQUIRED');}
      const weighted=policy.dimensions.reduce((s,d)=>s+scores[d.id]*d.weight_percent/100,0),critical=policy.critical_dimensions.every(id=>scores[id]>=3);
      db.prepare('INSERT INTO evaluations VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(run_id) DO UPDATE SET evaluator_id=excluded.evaluator_id,scores_json=excluded.scores_json,evidence_json=excluded.evidence_json,weighted_score=excluded.weighted_score,critical_pass=excluded.critical_pass,created_at=excluded.created_at').run(randomUUID(),run.id,user.id,JSON.stringify(scores),JSON.stringify(evidence),weighted,critical?1:0,now());
      audit(user.id,'evaluation.recorded',run.id,{critical_pass:critical});return json(res,200,{weighted_score:weighted,critical_pass:critical,release_approved:false});
     }
     if(path==='/api/admin/audit'&&method==='GET')return json(res,200,{events:db.prepare('SELECT action,target_id,metadata_json,created_at FROM audit ORDER BY created_at DESC,rowid DESC LIMIT 100').all()});
     if(path==='/api/admin/health'&&method==='GET')return json(res,200,{version:VERSION,database:'sqlite',migration:1,provider_configured:provider.configured(),model:provider.model||null,engine_hash:coreHash,active_jobs:jobs.size,uptime_seconds:Math.floor(process.uptime()),release_status:'ALPHA_NOT_COMMERCIAL_RELEASE',native_installers_built:false,backup_restore_drill_completed:false});
    }
    throw fail(404,'NOT_FOUND');
   }
   if(!['GET','HEAD'].includes(method))throw fail(405,'METHOD_NOT_ALLOWED');
   const staticFiles={'/':'index.html','/index.html':'index.html','/app.js':'app.js','/style.css':'style.css','/manifest.webmanifest':'manifest.webmanifest','/icon.svg':'icon.svg','/sw.js':'sw.js','/font.woff2':'font.woff2'};
   const file=staticFiles[path];if(!file||!existsSync(join(root,'public',file)))throw fail(404,'NOT_FOUND');
   const type={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.webmanifest':'application/manifest+json','.woff2':'font/woff2'}[extname(file)];
   res.writeHead(200,{'Content-Type':type,'Cache-Control':'no-cache'});res.end(method==='HEAD'?undefined:readFileSync(join(root,'public',file)));
  }catch(error){
   const status=error.status||(error.message==='PASSWORD_LENGTH'?400:500);
   json(res,status,{error:status===500?'INTERNAL_ERROR':error.message,request_id:requestId});
  }
 }
 const server=http.createServer(handler);server.requestTimeout=15000;server.headersTimeout=10000;
 let closing;
 return {server,db,provider,close(){return closing??=(async()=>{await Promise.allSettled([...jobs]);await new Promise(resolve=>server.close(resolve));db.close();})();}};
}
