import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {generateKeyPairSync,sign,createHash} from 'node:crypto';
import {createBff} from '../server.mjs';

const {privateKey,publicKey}=generateKeyPairSync('rsa',{modulusLength:2048});
const jwk={...publicKey.export({format:'jwk'}),kid:'local-key',alg:'RS256',use:'sig'};
const rotatedPair=generateKeyPairSync('rsa',{modulusLength:2048});
const rotatedKey={privateKey:rotatedPair.privateKey,jwk:{...rotatedPair.publicKey.export({format:'jwk'}),kid:'rotated-key',alg:'RS256',use:'sig'}};
const toJson=(res,status,value)=>{res.writeHead(status,{'Content-Type':'application/json'});res.end(JSON.stringify(value));};
const listen=server=>new Promise(resolve=>server.listen(0,'127.0.0.1',()=>resolve('http://127.0.0.1:'+server.address().port)));
const close=server=>new Promise((resolve,reject)=>server.close(error=>error?reject(error):resolve()));
const encode=data=>Buffer.from(JSON.stringify(data)).toString('base64url');
function fixtureToken(issuer,nonce,sub='subject-42',signing={privateKey,jwk}){
 const h=encode({alg:'RS256',typ:'JWT',kid:signing.jwk.kid}),p=encode({iss:issuer,sub,aud:'whitehat-web',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,nonce});
 const sig=sign('RSA-SHA256',Buffer.from(h+'.'+p),signing.privateKey).toString('base64url');return [h,p,sig].join('.');
}
async function fixture(){
 const access=[],state={auth:null,grant:0,revoke:0,badSignature:false,offset:0,tokenStatus:0,jwksRequests:0,signing:{privateKey,jwk},keys:[jwk]};let issuer;
 const idp=http.createServer(async(req,res)=>{
  const url=new URL(req.url,'http://127.0.0.1');
  if(url.pathname==='/.well-known/openid-configuration')return toJson(res,200,{issuer,authorization_endpoint:issuer+'/authorize',token_endpoint:issuer+'/token',jwks_uri:issuer+'/jwks',revocation_endpoint:issuer+'/revoke'});
  if(url.pathname==='/jwks'){state.jwksRequests++;return toJson(res,200,{keys:state.keys});}
  if(url.pathname==='/authorize'){state.auth=Object.fromEntries(url.searchParams);res.writeHead(302,{Location:url.searchParams.get('redirect_uri')+'?state='+url.searchParams.get('state')+'&code=fixture-code'});res.end();return;}
  if(url.pathname==='/revoke'){state.revoke++;return toJson(res,200,{});}
  if(url.pathname==='/token'){
   let raw='';for await(const c of req)raw+=c;const form=new URLSearchParams(raw);state.grant++;
   if(state.tokenStatus)return toJson(res,state.tokenStatus,{error:state.tokenStatus===400?'invalid_grant':'temporarily_unavailable',error_description:'synthetic-provider-private-detail'});
   if(form.get('grant_type')==='authorization_code'){
    const challenge=createHash('sha256').update(form.get('code_verifier')).digest('base64url');
    if(form.get('code')!=='fixture-code'||challenge!==state.auth.code_challenge)return toJson(res,400,{error:'bad_pkce'});
   }
   if(form.get('grant_type')==='refresh_token'&&form.get('refresh_token')!=='refresh-fixture')return toJson(res,400,{error:'bad_refresh'});
   let idToken=fixtureToken(issuer,state.auth.nonce,'subject-42',state.signing);if(state.badSignature)idToken=idToken.slice(0,-2)+'aa';
   return toJson(res,200,{access_token:'access-'+state.grant,token_type:'Bearer',expires_in:35,refresh_token:'refresh-fixture',id_token:idToken});
  }
  return toJson(res,404,{error:'not_found'});
 });issuer=await listen(idp);
 const ws='11111111-1111-4111-8111-111111111111',decision='22222222-2222-4222-8222-222222222222';
 const api=http.createServer((req,res)=>{
  access.push({method:req.method,url:req.url,auth:req.headers.authorization,csrf:req.headers['x-csrf-token']});
  if(req.url==='/api/v1/me')return toJson(res,200,{subject:'subject-42',workspaces:[{id:ws,name:'Demo',role:'OWNER'}]});
  if(req.url==='/api/v1/modes')return toJson(res,200,{items:[{id:'challenge',verdicts:['PROCEED']} ]});
  if(req.url==='/api/v1/workspaces')return toJson(res,201,{id:ws,name:'Demo',role:'OWNER'});
  if(req.url===`/api/v1/workspaces/${ws}/decisions`)return toJson(res,200,{items:[]});
  if(req.url===`/api/v1/workspaces/${ws}/runs/${decision}`)return toJson(res,200,{id:decision,status:'reconciliation_required',input_tokens:null,output_tokens:13});
  if(req.method==='GET'&&req.url===`/api/v1/workspaces/${ws}/admin/pilot-usage`)return toJson(res,200,{reconciled:true,confirmed_usd_micros:null});
  if(req.url===`/api/v1/workspaces/${ws}/admin/usage`)return toJson(res,403,{error:'FORBIDDEN',request_id:'fixture-request'});
  return toJson(res,404,{error:'NOT_FOUND'});
 });const apiUrl=await listen(api);
 const env={WH_WEB_ORIGIN:'http://127.0.0.1:3030',WH_API_BASE:apiUrl,WH_OIDC_ISSUER:issuer,WH_OIDC_CLIENT_ID:'whitehat-web',WH_DEV_INSECURE_HTTP:'1'};
 const {server}=createBff({env,clock:()=>Date.now()+state.offset});const actualOrigin=await listen(server);
 // Callback must use the actual test listener origin; reset as the fixture's public base.
 await close(server);const ownEnv={...env,WH_WEB_ORIGIN:actualOrigin};const {server:app}=createBff({env:ownEnv,clock:()=>Date.now()+state.offset});await new Promise(resolve=>app.listen(Number(new URL(actualOrigin).port),'127.0.0.1',resolve));
 return {origin:actualOrigin,app,idp,api,access,state,ws,decision,async stop(){await Promise.all([close(app),close(api),close(idp)]);}};
}
async function login(f){
 const start=await fetch(f.origin+'/auth/login',{redirect:'manual'});assert.equal(start.status,302);
 const flow=start.headers.get('set-cookie').split(';')[0];const authorize=await fetch(start.headers.get('location'),{redirect:'manual'});
 const callback=await fetch(authorize.headers.get('location'),{headers:{cookie:flow},redirect:'manual'});
 return {start,flow,callback,sessionCookie:callback.headers.get('set-cookie')?.match(/wh_session_dev=[^;]+/)?.[0]};
}
test('requires secure origins outside explicitly enabled local development',()=>{
 assert.throws(()=>createBff({env:{WH_WEB_ORIGIN:'http://example.com',WH_OIDC_ISSUER:'https://id.example.com',WH_OIDC_CLIENT_ID:'web'}}),/HTTPS_WEB_REQUIRED/);
 assert.throws(()=>createBff({env:{WH_WEB_ORIGIN:'https://example.com',WH_OIDC_ISSUER:'http://bad.example.com',WH_OIDC_CLIENT_ID:'web'}}),/HTTPS_IDP_REQUIRED/);
});
test('OIDC PKCE / HttpOnly cookie / CSRF / allowlisted Kotlin proxy / revocation',async()=>{
 const f=await fixture();try{
  const {start,callback,sessionCookie}=await login(f);assert.match(start.headers.get('location'),/code_challenge_method=S256/);
  assert.equal(callback.status,303);assert.ok(sessionCookie);assert.match(callback.headers.get('set-cookie'),/HttpOnly/);
  const sess=await fetch(f.origin+'/bff/session',{headers:{cookie:sessionCookie}});assert.equal(sess.status,200);
  const {csrf,subject}=await sess.json();assert.equal(subject,'subject-42');assert.ok(csrf);
  const me=await fetch(f.origin+'/bff/api/v1/me',{headers:{cookie:sessionCookie}});assert.equal(me.status,200);assert.equal((await me.json()).workspaces[0].role,'OWNER');
  assert.match(f.access.at(-1).auth,/^Bearer access-1$/);assert.equal(me.headers.get('cache-control'),'no-store');
  f.state.offset=10_000;const refreshed=await fetch(f.origin+'/bff/api/v1/modes',{headers:{cookie:sessionCookie}});assert.equal(refreshed.status,200);assert.equal(f.state.grant,2);
  const noCsrf=await fetch(f.origin+'/bff/api/v1/workspaces',{method:'POST',headers:{cookie:sessionCookie,origin:f.origin,'content-type':'application/json'},body:'{"name":"Demo"}'});
  assert.equal(noCsrf.status,403);assert.equal((await noCsrf.json()).error,'CSRF_REJECTED');
  const wrongOrigin=await fetch(f.origin+'/bff/api/v1/workspaces',{method:'POST',headers:{cookie:sessionCookie,origin:'https://attacker.example','x-csrf-token':csrf,'content-type':'application/json'},body:'{"name":"Demo"}'});
  assert.equal(wrongOrigin.status,403);
  const post=await fetch(f.origin+'/bff/api/v1/workspaces',{method:'POST',headers:{cookie:sessionCookie,origin:f.origin,'x-csrf-token':csrf,'content-type':'application/json'},body:'{"name":"Demo"}'});
  assert.equal(post.status,201);assert.equal((await post.json()).id,f.ws);
  const unknown=await fetch(f.origin+'/bff/api/v1/openapi.json',{headers:{cookie:sessionCookie}});assert.equal(unknown.status,404);
  const denied=await fetch(f.origin+`/bff/api/v1/workspaces/${f.ws}/admin/usage`,{headers:{cookie:sessionCookie}});assert.equal(denied.status,403);assert.equal((await denied.json()).request_id,'fixture-request');
  const pilot=await fetch(f.origin+`/bff/api/v1/workspaces/${f.ws}/admin/pilot-usage`,{headers:{cookie:sessionCookie}});
  assert.equal(pilot.status,200);assert.equal(pilot.headers.get('cache-control'),'no-store');
  assert.deepEqual(await pilot.json(),{reconciled:true,confirmed_usd_micros:null});
  const forwarded=f.access.length;
  const pilotMutation=await fetch(f.origin+`/bff/api/v1/workspaces/${f.ws}/admin/pilot-usage`,
   {method:'PUT',headers:{cookie:sessionCookie,origin:f.origin,'x-csrf-token':csrf}});
  assert.equal(pilotMutation.status,404);assert.equal(f.access.length,forwarded);
  const uncertain=await fetch(f.origin+`/bff/api/v1/workspaces/${f.ws}/runs/${f.decision}`,{headers:{cookie:sessionCookie}});const details=await uncertain.json();assert.equal(details.input_tokens,null);assert.equal(details.status,'reconciliation_required');
  const logout=await fetch(f.origin+'/bff/logout',{method:'POST',headers:{cookie:sessionCookie,origin:f.origin,'x-csrf-token':csrf}});assert.equal(logout.status,200);assert.equal((await logout.json()).revocation,'confirmed');assert.equal(f.state.revoke,1);
  assert.equal((await fetch(f.origin+'/bff/session',{headers:{cookie:sessionCookie}})).status,401);
 }finally{await f.stop();}
});
test('rejects callback state replay and tampered ID token',async()=>{
 const f=await fixture();try{
  const start=await fetch(f.origin+'/auth/login',{redirect:'manual'});const auth=await fetch(start.headers.get('location'),{redirect:'manual'});const url=auth.headers.get('location'),flow=start.headers.get('set-cookie').split(';')[0];
  const wrong=await fetch(url,{redirect:'manual'});assert.equal(wrong.status,400);
  // Wrong cookie consumes pending state; callback replay must fail even if cookie is later supplied.
  const replay=await fetch(url,{headers:{cookie:flow},redirect:'manual'});assert.equal(replay.status,400);
  f.state.badSignature=true;const result=await login(f);assert.equal(result.callback.status,401);assert.equal(result.sessionCookie,undefined);
 }finally{await f.stop();}
});

test('path-scoped OIDC issuer discovers its own suffix metadata endpoint',async()=>{
 const issuer='http://127.0.0.1:8181/realms/whitehat',seen=[];
 const env={WH_WEB_ORIGIN:'http://127.0.0.1:3030',WH_API_BASE:'http://127.0.0.1:8081',WH_OIDC_ISSUER:issuer,WH_OIDC_CLIENT_ID:'whitehat-web',WH_DEV_INSECURE_HTTP:'1'};
 const httpClient=async url=>{seen.push(String(url));return new Response(JSON.stringify({issuer,authorization_endpoint:issuer+'/authorize',token_endpoint:issuer+'/token',jwks_uri:issuer+'/jwks'}),{status:200,headers:{'content-type':'application/json'}});};
 const {server}=createBff({env,httpClient});const actual=await listen(server);
 try {const response=await fetch(actual+'/auth/login',{redirect:'manual'});assert.equal(response.status,302);assert.equal(seen[0],issuer+'/.well-known/openid-configuration');}
 finally{await close(server);}
});

test('accepts a newly published signing key before the JWKS cache expires',async()=>{
 const f=await fixture();try{
  assert.equal((await login(f)).callback.status,303);assert.equal(f.state.jwksRequests,1);
  f.state.signing=rotatedKey;f.state.keys=[rotatedKey.jwk];
  const rotated=await login(f);assert.equal(rotated.callback.status,303);assert.ok(rotated.sessionCookie);
  assert.equal(f.state.jwksRequests,2);
 }finally{await f.stop();}
});

test('unknown signing keys fail closed and cannot trigger unbounded JWKS reloads',async()=>{
 const f=await fixture();try{
  assert.equal((await login(f)).callback.status,303);
  f.state.signing=rotatedKey;
  for(let i=0;i<3;i++){
   const rejected=await login(f);assert.equal(rejected.callback.status,401);assert.equal(rejected.sessionCookie,undefined);
  }
  assert.equal(f.state.jwksRequests,2);
 }finally{await f.stop();}
});

test('transient refresh failure preserves the session for recovery and forwards no stale token',async()=>{
 const f=await fixture();try{
  const {sessionCookie}=await login(f);
  for(const status of [503,429]){
   f.state.offset+=10_000;f.state.tokenStatus=status;
   const before=f.access.length;
   const unavailable=await fetch(f.origin+'/bff/api/v1/me',{headers:{cookie:sessionCookie}});
   assert.equal(unavailable.status,503);
   const body=await unavailable.json();assert.equal(body.error,'IDENTITY_PROVIDER_UNAVAILABLE');
   assert.equal(JSON.stringify(body).includes('synthetic-provider-private-detail'),false);
   assert.equal(f.access.length,before);
   f.state.tokenStatus=0;
   const recovered=await fetch(f.origin+'/bff/api/v1/me',{headers:{cookie:sessionCookie}});
   assert.equal(recovered.status,200);assert.equal(f.access.length,before+1);
  }
 }finally{await f.stop();}
});

test('rejected refresh credentials invalidate the session permanently',async()=>{
 const f=await fixture();try{
  const {sessionCookie}=await login(f);f.state.offset=10_000;f.state.tokenStatus=400;
  assert.equal((await fetch(f.origin+'/bff/session',{headers:{cookie:sessionCookie}})).status,401);
  f.state.tokenStatus=0;
  assert.equal((await fetch(f.origin+'/bff/session',{headers:{cookie:sessionCookie}})).status,401);
 }finally{await f.stop();}
});
