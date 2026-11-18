/** White Hat web BFF. Transport only: Kotlin API remains sole business/data authority. */
import http from 'node:http';
import { createMemorySessionStore, createPostgresSessionStore } from './session-store.mjs';
import { randomBytes, createHash, createPublicKey, verify, timingSafeEqual } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const rnd = () => randomBytes(32).toString('base64url');
const hash = value => createHash('sha256').update(value).digest('hex');
const json = (res, status, body, extra={}) => reply(res,status,JSON.stringify(body),{'Content-Type':'application/json; charset=utf-8',...extra});
const reply = (res,status,body,headers={})=>{res.writeHead(status,headers);res.end(body);};
const fail = (status, code) => Object.assign(new Error(code), {status,code});
const eq = (a,b) => {const x=Buffer.from(String(a||'')),y=Buffer.from(String(b||''));return x.length===y.length && timingSafeEqual(x,y);};
const parseCookies = header => Object.fromEntries(String(header||'').split(';').map(v=>v.trim().split('=').slice(0,2)).filter(v=>v.length===2));
const decodePart = value => JSON.parse(Buffer.from(value,'base64url').toString('utf8'));
const uuid = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}';
const allowed = [
 ['GET',/^\/api\/v1\/(me|modes)$/], ['POST',/^\/api\/v1\/workspaces$/],
 ['GET',new RegExp('^/api/v1/workspaces/'+uuid+'/admin/plan-preview$')],
 ['POST',new RegExp('^/api/v1/workspaces/'+uuid+'/admin/plan-preview$')],
 ['GET',/^\/api\/v1\/workspaces\/[^/]+\/decisions$/], ['POST',/^\/api\/v1\/workspaces\/[^/]+\/decisions$/],
 ['GET',/^\/api\/v1\/workspaces\/[^/]+\/decisions\/[^/]+$/], ['PUT',/^\/api\/v1\/workspaces\/[^/]+\/decisions\/[^/]+$/],
 ['POST',/^\/api\/v1\/workspaces\/[^/]+\/decisions\/[^/]+\/runs$/],
 ['GET',/^\/api\/v1\/workspaces\/[^/]+\/runs\/[^/]+$/],
 ['POST',/^\/api\/v1\/workspaces\/[^/]+\/runs\/[^/]+\/cancel$/],
 ['GET',/^\/api\/v1\/workspaces\/[^/]+\/admin\/usage$/],
 ['GET',new RegExp('^/api/v1/workspaces/'+uuid+'/analysis-settings$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/analysis-settings$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/admin/costs/'+uuid+'/corrections/'+uuid+'$')],
 ['GET',new RegExp('^/api/v1/workspaces/'+uuid+'/feedback-retention$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/feedback-retention$')],
 ['POST',new RegExp('^/api/v1/workspaces/'+uuid+'/feedback-retention/purge$')],
 ['GET',new RegExp('^/api/v1/workspaces/'+uuid+'/support-roles$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/(support-roles/'+uuid+'|support-role-assignment)$')],
 ['GET',new RegExp('^/api/v1/workspaces/'+uuid+'/(operations-metrics|feedback-settings|quality-queue|admin/costs|admin/pilot-usage)$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/(feedback-settings|feedback/'+uuid+'|quality-queue/'+uuid+'/review|admin/costs/'+uuid+')$')],
 ['POST',new RegExp('^/api/v1/workspaces/'+uuid+'/feedback/'+uuid+'/withdraw$')],
 ['GET',new RegExp('^/api/v1/workspaces/'+uuid+'/(members|runs|incidents)$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/operator-permissions$')],
 ['PUT',new RegExp('^/api/v1/workspaces/'+uuid+'/incidents/'+uuid+'$')],
 ['POST',/^\/api\/v1\/workspaces\/[^/]+\/members$/]
];
const voicePath = new RegExp('^/api/v1/workspaces/'+uuid+'/voice-pilot(?:/'+uuid+'/(?:chunk|finish|cancel))?$');
const allowedPath = (method,path) => allowed.some(([m,r])=>m===method&&r.test(path)) &&
  path.split('/').every(p=>!p.includes('%')&&!p.includes('..')) &&
  path.split('/').filter((p,i)=>i===4||i===6||i===8).every(p=>!p||p==='decisions'||p==='runs'||p==='members'||p==='admin'||p==='usage'||p==='plan-preview'||p==='pilot-usage'||p==='incidents'||p==='operator-permissions'||p==='costs'||p==='corrections'||p==='review'||p==='withdraw'||p==='purge'||new RegExp('^'+uuid+'$').test(p));

export function createBff({env=process.env, httpClient=fetch, clock=()=>Date.now(), rateClock=()=>Date.now(), sessionStore}={}) {
  const origin = new URL(env.WH_WEB_ORIGIN || 'https://localhost:3030');
  const upstream = new URL(env.WH_API_BASE || 'http://127.0.0.1:8081');
  const issuer = env.WH_OIDC_ISSUER;
  const clientId = env.WH_OIDC_CLIENT_ID;
  const voiceEnabled = env.WH_VOICE_PILOT==='1';
  const localHttp = env.WH_DEV_INSECURE_HTTP==='1';
  if (!issuer||!clientId) throw new Error('OIDC_CONFIG_REQUIRED');
  if (origin.pathname!=='/'||origin.search||origin.hash) throw new Error('INVALID_WEB_ORIGIN');
  if (origin.protocol!=='https:' && !(localHttp&&origin.hostname==='127.0.0.1'&&origin.protocol==='http:')) throw new Error('HTTPS_WEB_REQUIRED');
  if (upstream.protocol!=='https:' && !['127.0.0.1','localhost'].includes(upstream.hostname)) throw new Error('PRIVATE_API_OR_HTTPS_REQUIRED');
  const issuerUrl = new URL(issuer);
  if (issuerUrl.protocol!=='https:' && !(localHttp&&issuerUrl.hostname==='127.0.0.1'&&issuerUrl.protocol==='http:')) throw new Error('HTTPS_IDP_REQUIRED');
  const secure = origin.protocol==='https:';
  const cookieName = secure?'__Host-wh_session':'wh_session_dev';
  const flowName = secure?'__Host-wh_login':'wh_login_dev';
  const cookie = (name,value,age=0) => `${name}=${value}; Path=/; HttpOnly; SameSite=Lax${secure?'; Secure':''}${age?`; Max-Age=${age}`:'; Max-Age=0'}`;
  const dsn=env.WH_WEB_SESSION_DATABASE_URL, keys=env.WH_WEB_SESSION_KEYS;
  if (!sessionStore && (!dsn || !keys) && (!localHttp || dsn || keys)) throw new Error('DURABLE_SESSION_STORE_REQUIRED');
  const store=sessionStore ?? (dsn ? createPostgresSessionStore(dsn,keys) : createMemorySessionStore());
  const request = async (url,opts={},timeout=10000) => httpClient(url,{...opts,redirect:'error',signal:AbortSignal.timeout(timeout)});
  const readJson = async response => {if(!response.ok) throw fail(502,'IDENTITY_PROVIDER_UNAVAILABLE');const body=await response.json();if(!body||typeof body!=='object')throw fail(502,'IDENTITY_PROVIDER_INVALID');return body;};
  let discoveryCache, jwksCache, jwksPending;
  let lastJwksRefresh=-Infinity;
  async function discovery(){
    if(discoveryCache && discoveryCache.expires>clock())return discoveryCache.value;
    const base=new URL(issuer);const path=base.pathname.replace(/\/$/,'');
    const wellKnown=new URL(path+'/.well-known/openid-configuration',base.origin);
    const data=await readJson(await request(wellKnown));
    if(data.issuer!==issuer)throw fail(502,'OIDC_ISSUER_MISMATCH');
    for(const key of ['authorization_endpoint','token_endpoint','jwks_uri']) {
      const u=new URL(data[key]);if(u.protocol!=='https:'&&!(localHttp&&u.protocol==='http:'&&u.hostname==='127.0.0.1'))throw fail(502,'OIDC_ENDPOINT_REJECTED');
    }
    discoveryCache={value:data,expires:clock()+300000};return data;
  }
  async function jwks(config,refresh=false){
    if(jwksPending)return jwksPending;
    if(jwksCache&&((!refresh&&jwksCache.expires>clock())||
       (refresh&&clock()-lastJwksRefresh<30000)))return jwksCache.value;
    // A new kid may mean provider rotation. Bound forced reloads and share in-flight reads.
    if(refresh)lastJwksRefresh=clock();
    jwksPending=(async()=>{
      const data=await readJson(await request(config.jwks_uri));
      if(!Array.isArray(data.keys))throw fail(502,'OIDC_JWKS_INVALID');
      jwksCache={value:data.keys,expires:clock()+300000};return data.keys;
    })();
    try{return await jwksPending;}finally{jwksPending=undefined;}
  }
  async function verifyIdToken(raw,nonce,config){
    if(typeof raw!=='string'||raw.length>12000)throw fail(401,'OIDC_ID_TOKEN_REQUIRED');
    const parts=raw.split('.');if(parts.length!==3)throw fail(401,'OIDC_TOKEN_INVALID');
    let header,claims;try{header=decodePart(parts[0]);claims=decodePart(parts[1]);}catch{throw fail(401,'OIDC_TOKEN_INVALID');}
    if(header.alg!=='RS256'||typeof header.kid!=='string')throw fail(401,'OIDC_ALGORITHM_REJECTED');
    const keys=await jwks(config);
    const matching=keys=>keys.find(k=>k&&k.kid===header.kid&&k.kty==='RSA'&&(!k.use||k.use==='sig')&&(!k.alg||k.alg==='RS256'));
    const jwk=matching(keys)||matching(await jwks(config,true));
    if(!jwk)throw fail(401,'OIDC_KEY_NOT_FOUND');
    let valid=false;try{valid=verify('RSA-SHA256',Buffer.from(parts[0]+'.'+parts[1]),createPublicKey({key:jwk,format:'jwk'}),Buffer.from(parts[2],'base64url'));}catch{}
    const seconds=Math.floor(clock()/1000);
    const aud=Array.isArray(claims.aud)?claims.aud:[claims.aud];
    if(!valid||claims.iss!==issuer||!aud.includes(clientId)||(aud.length>1&&claims.azp!==clientId)||
       typeof claims.sub!=='string'||!claims.sub||!Number.isFinite(claims.exp)||claims.exp<=seconds||
       !Number.isFinite(claims.iat)||claims.iat>seconds+60||(nonce!==null&&!eq(claims.nonce,nonce)))throw fail(401,'OIDC_ID_TOKEN_INVALID');
    return claims;
  }
  async function exchange(config,params){
    const headers={'Content-Type':'application/x-www-form-urlencoded'};
    if(env.WH_OIDC_CLIENT_SECRET){headers.Authorization='Basic '+Buffer.from(clientId+':'+env.WH_OIDC_CLIENT_SECRET).toString('base64');}
    const response=await request(config.token_endpoint,{method:'POST',headers,body:new URLSearchParams(params)});
    if(params.grant_type==='refresh_token'&&[400,401,403].includes(response.status))throw fail(401,'SESSION_EXPIRED');
    if(response.status===429||response.status>=500)throw fail(503,'IDENTITY_PROVIDER_UNAVAILABLE');
    const body=await readJson(response);
    if(body.token_type?.toLowerCase()!=='bearer'||typeof body.access_token!=='string'||!Number.isInteger(body.expires_in)||body.expires_in<1)throw fail(502,'OIDC_TOKEN_RESPONSE_INVALID');
    return body;
  }
  const sessionId = req => {
    const raw=parseCookies(req.headers.cookie)[cookieName];
    return raw ? hash(raw) : null;
  };
  async function accessSession(req, mutating=false, logout=false) {
    const id=sessionId(req);
    if(!id)throw fail(logout?403:401,logout?'CSRF_REJECTED':'AUTH_REQUIRED');
    const result=await store.transact(id,clock(),async session=>{
      if(!session) return {value:{error:logout?'CSRF_REJECTED':'AUTH_REQUIRED'}};
      if(mutating&&!eq(req.headers['x-csrf-token'],session.csrf))throw fail(403,'CSRF_REJECTED');
      if(logout)return {value:{session}};
      if(session.expires-clock()>30000)return {session,value:{session}};
      if(!session.refresh)return {value:{error:'SESSION_EXPIRED'}};
      try {
        const config=await discovery();
        const token=await exchange(config,{grant_type:'refresh_token',refresh_token:session.refresh,client_id:clientId});
        if(token.id_token){
          const claims=await verifyIdToken(token.id_token,null,config);
          if(claims.sub!==session.subject)throw fail(401,'OIDC_SUBJECT_CHANGED');
        }
        const updated={...session,access:token.access_token,expires:clock()+token.expires_in*1000,
          refresh:token.refresh_token||session.refresh};
        return {session:updated,value:{session:updated}};
      } catch(error) {
        if(error.status===401)return {value:{error:'SESSION_EXPIRED'}};
        // A provider outage must not erase durable credentials or forward an expired token.
        return {session,value:{error:'IDENTITY_PROVIDER_UNAVAILABLE',status:503}};
      }
    });
    if(result.error)throw fail(result.status||(logout?403:401),result.error);
    return result.session;
  }
  const securityHeaders={
    'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer',
    'X-Frame-Options':'DENY','Content-Security-Policy':"default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'"
  };
  async function throttle(res,key,limit) {
    const now=rateClock();
    if(!store.allowRequest || await store.allowRequest(hash(key),now,limit))return;
    res.setHeader('Retry-After',String(Math.ceil((60000-now%60000)/1000)));
    throw fail(429,'REQUEST_RATE_LIMITED');
  }
  async function handle(req,res){
    Object.entries(securityHeaders).forEach(([k,v])=>res.setHeader(k,v));
    const url=new URL(req.url,origin), path=url.pathname, method=req.method;
    res.setHeader('Permissions-Policy','camera=(), microphone='+(voiceEnabled&&path==='/voice-pilot'?'(self)':'()'));
    const originCheck=()=>{if(req.headers.origin!==origin.origin)throw fail(403,'ORIGIN_REJECTED');};
    if(path==='/health'&&method==='GET')return json(res,200,{status:'ok',service:'web-bff',idp_configured:true});
    if(path==='/auth/login'&&method==='GET'){
      await throttle(res,'login:'+req.socket.remoteAddress,40);
      const config=await discovery(),state=rnd(),verifier=rnd(),nonce=rnd();
      const challenge=createHash('sha256').update(verifier).digest('base64url');
      if(!await store.createFlow(hash(state),{verifier,nonce,expires:clock()+300000},clock()))throw fail(429,'LOGIN_CAPACITY_REACHED');
      const target=new URL(config.authorization_endpoint);
      new URLSearchParams({response_type:'code',client_id:clientId,redirect_uri:origin.origin+'/auth/callback',scope:'openid profile email',state,nonce,code_challenge:challenge,code_challenge_method:'S256'}).forEach((value,key)=>target.searchParams.set(key,value));
      res.writeHead(302,{'Location':target.toString(),'Set-Cookie':cookie(flowName,state,300)});return res.end();
    }
    if(path==='/auth/callback'&&method==='GET'){
      const state=url.searchParams.get('state')||'',code=url.searchParams.get('code')||'',flow=parseCookies(req.headers.cookie)[flowName];
      res.setHeader('Set-Cookie',cookie(flowName,''));
      const item=await store.takeFlow(hash(state),clock());
      if(!flow||!eq(flow,state)||!item||item.expires<clock()||!code||url.searchParams.has('error'))throw fail(400,'LOGIN_STATE_INVALID');
      const config=await discovery();
      const token=await exchange(config,{grant_type:'authorization_code',code,redirect_uri:origin.origin+'/auth/callback',client_id:clientId,code_verifier:item.verifier});
      const claims=await verifyIdToken(token.id_token,item.nonce,config),sid=rnd(),key=hash(sid);
      await store.createSession(key,{subject:claims.sub,nonce:item.nonce,access:token.access_token,refresh:token.refresh_token||null,expires:clock()+token.expires_in*1000,csrf:rnd(),created:clock()},clock());
      res.writeHead(303,{'Location':'/','Set-Cookie':[cookie(flowName,''),cookie(cookieName,sid,28800)]});return res.end();
    }
    if(path==='/bff/session'&&method==='GET'){
      const s=await accessSession(req);
      return json(res,200,{subject:s.subject,csrf:s.csrf});
    }
    if(path==='/bff/logout'&&method==='POST'){
      originCheck();const s=await accessSession(req,true,true);
      res.setHeader('Set-Cookie',cookie(cookieName,''));
      let revocation='unavailable';
      if(s.refresh){try{const config=await discovery();if(config.revocation_endpoint){const endpoint=new URL(config.revocation_endpoint);if(endpoint.protocol==='https:'||(localHttp&&endpoint.protocol==='http:'&&endpoint.hostname==='127.0.0.1')){const headers={'Content-Type':'application/x-www-form-urlencoded'};if(env.WH_OIDC_CLIENT_SECRET)headers.Authorization='Basic '+Buffer.from(clientId+':'+env.WH_OIDC_CLIENT_SECRET).toString('base64');const response=await request(endpoint,{method:'POST',headers,body:new URLSearchParams({token:s.refresh,token_type_hint:'refresh_token',client_id:clientId})});revocation=response.ok?'confirmed':'failed';}}}catch{revocation='failed';}}
      return json(res,200,{ok:true,revocation});
    }
    if(path.startsWith('/bff/api/v1/')){
      if(!sessionId(req))throw fail(401,'AUTH_REQUIRED');
      const apiPath=path.slice(4);
      const voiceAllowed=voiceEnabled&&voicePath.test(apiPath)&&
        (method==='POST'||(method==='GET'&&apiPath.endsWith('/voice-pilot')));
      if((!allowedPath(method,apiPath)&&!voiceAllowed)||url.search)throw fail(404,'API_PATH_UNSUPPORTED');
      const mutating=!['GET','HEAD'].includes(method);
      if(mutating)originCheck();
      const s=await accessSession(req,mutating);
      await throttle(res,'api:'+s.subject,180);
      const access=s.access;let body;
      if(mutating){
        if(!(req.headers['content-type']||'').startsWith('application/json'))throw fail(415,'JSON_REQUIRED');
        const parts=[];let total=0;
        for await(const chunk of req){total+=chunk.length;if(total>140000)throw fail(413,'INPUT_TOO_LARGE');parts.push(chunk);}
        body=Buffer.concat(parts);
      }
      const headers={Authorization:'Bearer '+access,Accept:'application/json'};
      if(mutating)headers['Content-Type']='application/json';
      if(req.headers['idempotency-key'])headers['Idempotency-Key']=String(req.headers['idempotency-key']).slice(0,200);
      const target=new URL(apiPath,upstream);
      const response=await request(target,{method,headers,...(mutating?{body}:{})},voiceAllowed?15000:10000);
      if(response.status===401){
        await store.transact(sessionId(req),clock(),session=>({session:session?.access===access?undefined:session}));
        throw fail(401,'SESSION_REAUTH_REQUIRED');
      }
      const contentType=response.headers.get('content-type')||'';
      if(!contentType.includes('application/json'))throw fail(502,'API_NON_JSON_RESPONSE');
      const text=await response.text();if(Buffer.byteLength(text)>2000000)throw fail(502,'API_RESPONSE_TOO_LARGE');
      return reply(res,response.status,text,{'Content-Type':'application/json; charset=utf-8'});
    }
    if(method!=='GET'&&method!=='HEAD')throw fail(405,'METHOD_NOT_ALLOWED');
    const assets={
      '/': ['public/index.html','text/html; charset=utf-8'],
      '/index.html':['public/index.html','text/html; charset=utf-8'],
      '/panel.js':['public/panel.js','text/javascript; charset=utf-8'],
      '/panel.css':['public/panel.css','text/css; charset=utf-8'],
      '/style.css':['../second-mind/public/style.css','text/css; charset=utf-8'],
      '/icon.svg':['../second-mind/public/icon.svg','image/svg+xml']
    };
    if(voiceEnabled){
      assets['/voice-pilot']=['public/voice-pilot.html','text/html; charset=utf-8'];
      assets['/voice-pilot.mjs']=['public/voice-pilot.mjs','text/javascript; charset=utf-8'];
      assets['/voice-pcm.mjs']=['public/voice-pcm.mjs','text/javascript; charset=utf-8'];
    }
    if(!assets[path])throw fail(404,'NOT_FOUND');
    const [file,type]=assets[path];const contents=await readFile(fileURLToPath(new URL(file,import.meta.url)));
    return reply(res,200,method==='HEAD'?'':contents,{'Content-Type':type});
  }
  const server=http.createServer((req,res)=>handle(req,res).catch(e=>{
    if(res.headersSent){res.destroy();return;}
    json(res,e.status||500,{error:e.code||'BFF_INTERNAL_ERROR',request_id:rnd().slice(0,16)});
  }));
  server.once('close',()=>{store.close?.().catch(()=>{});});
  return {server,handle};
}

if(process.argv[1]&&fileURLToPath(import.meta.url)===process.argv[1]){
  const {server}=createBff();const port=Number(process.env.WH_WEB_PORT||3030);
  server.listen(port,process.env.WH_WEB_HOST||'127.0.0.1',()=>console.log(`White Hat BFF listening on ${port}`));
}
