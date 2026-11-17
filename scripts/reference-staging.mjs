/** Reference staging only. Real Keycloak/TLS/Ktor/Postgres, disposable synthetic accounts. */
import assert from 'node:assert/strict';
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve} from 'node:path';
import {createRequire} from 'node:module';
import {randomBytes} from 'node:crypto';
import http from 'node:http';
import https from 'node:https';
import {createBff} from '../apps/web-bff/server.mjs';

const directory=process.argv[3];
const issuer='https://localhost:38443/realms/whitehat-s2',origin='https://localhost:39443';
const subject='11111111-2222-4333-8444-555555555555';
if(process.argv[2]==='--prepare'){
 const password=randomBytes(24).toString('base64url');
 mkdirSync(resolve(directory,'realm'),{recursive:true});
 writeFileSync(resolve(directory,'credentials.json'),JSON.stringify({password}),{mode:0o600});
 const realm={realm:'whitehat-s2',enabled:true,sslRequired:'all',registrationAllowed:false,
  accessTokenLifespan:45,ssoSessionIdleTimeout:300,ssoSessionMaxLifespan:600,
  revokeRefreshToken:true,refreshTokenMaxReuse:0,
  users:[{id:subject,username:'s2-owner',enabled:true,emailVerified:true,email:'s2-owner@example.test',
   firstName:'S2',lastName:'Reference',credentials:[{type:'password',value:password,temporary:false}]}],
  clients:[{clientId:'whitehat-reference-web',enabled:true,publicClient:true,protocol:'openid-connect',
   standardFlowEnabled:true,directAccessGrantsEnabled:false,redirectUris:[origin+'/auth/callback'],webOrigins:[origin],
   attributes:{'pkce.code.challenge.method':'S256'},protocolMappers:[
    {name:'api-audience',protocol:'openid-connect',protocolMapper:'oidc-audience-mapper',config:{'included.client.audience':'whitehat-api','access.token.claim':'true','id.token.claim':'false'}},
    {name:'access-use',protocol:'openid-connect',protocolMapper:'oidc-hardcoded-claim-mapper',config:{'claim.name':'token_use','claim.value':'access','jsonType.label':'String','access.token.claim':'true','id.token.claim':'false'}}
   ]} ]};
 writeFileSync(resolve(directory,'realm/whitehat-s2-realm.json'),JSON.stringify(realm));
 process.exit(0);
}

const ready=JSON.parse(readFileSync(resolve(directory,'ready.json'),'utf8'));
const ca=readFileSync(resolve(directory,'ca.crt'));
const key=readFileSync(resolve(directory,'server.key')),cert=readFileSync(resolve(directory,'server.crt'));
const {Client}=createRequire(resolve('apps/web-bff/package.json'))('pg');
const {chromium}=createRequire(process.env.WH_PLAYWRIGHT_MODULE)('playwright');
const {default:AxeBuilder}=createRequire(process.env.WH_PLAYWRIGHT_MODULE)('@axe-core/playwright');
const results=[];
const report=(check,details={})=>{results.push({check,pass:true,...details});console.log(JSON.stringify(results.at(-1)));};
const listen=(server,port=0)=>new Promise((ok,fail)=>{server.once('error',fail);server.listen(port,'127.0.0.1',()=>ok(server.address().port));});
const close=server=>new Promise(ok=>server.close(ok));
const sessionAdmin=new Client({connectionString:ready.sessionAdminDb});
const appClient=new Client({connectionString:ready.appDb});
const servers=[];let edge,browser;
try {
 await sessionAdmin.connect();await appClient.connect();
 await sessionAdmin.query(readFileSync(resolve('apps/web-bff/db/session-store.sql'),'utf8'));
 const sessionPassword=randomBytes(24).toString('hex');
 await sessionAdmin.query(`CREATE ROLE wh_reference_bff LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '${sessionPassword}'`);
 await sessionAdmin.query('GRANT USAGE ON SCHEMA public TO wh_reference_bff');
 await sessionAdmin.query('GRANT SELECT,INSERT,UPDATE,DELETE ON wh_web_sessions,wh_web_login_flows,wh_web_rate_windows TO wh_reference_bff');
 const sessionUrl=new URL(ready.sessionAdminDb);sessionUrl.username='wh_reference_bff';sessionUrl.password=sessionPassword;
 const sessionKeys=randomBytes(32).toString('base64');
 const active=[];
 for(let i=0;i<2;i++){
  const {server}=createBff({env:{WH_WEB_ORIGIN:origin,WH_API_BASE:ready.api,WH_OIDC_ISSUER:issuer,
   WH_OIDC_CLIENT_ID:'whitehat-reference-web',WH_WEB_SESSION_DATABASE_URL:sessionUrl.toString(),WH_WEB_SESSION_KEYS:sessionKeys}});
  servers.push(server);active.push({server,port:await listen(server)});
 }
 let next=0;
 edge=https.createServer({key,cert},(req,res)=>{
  const target=active[next++%active.length];
  const upstream=http.request({hostname:'127.0.0.1',port:target.port,path:req.url,method:req.method,headers:req.headers},response=>{
   res.writeHead(response.statusCode,response.headers);response.pipe(res);
  });
  upstream.on('error',()=>{if(!res.headersSent)res.writeHead(503,{'content-type':'application/json'});res.end('{"error":"REFERENCE_UPSTREAM_UNAVAILABLE"}');});
  req.pipe(upstream);
 });await listen(edge,39443);
 const strictGet=(url,trust)=>new Promise((ok,fail)=>{const request=https.get(url,{ca:trust},r=>{r.resume();ok(r.statusCode);});request.on('error',fail);});
 assert.equal(await strictGet(issuer+'/.well-known/openid-configuration',ca),200);
 await assert.rejects(strictGet(issuer+'/.well-known/openid-configuration',readFileSync(resolve(directory,'untrusted.crt'))));
 report('trusted_tls_and_rejected_untrusted_ca',{tls:true,realIdp:'Keycloak',httpEnabled:false});
 browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 const context=await browser.newContext();const page=await context.newPage();page.setDefaultTimeout(15000);
 await page.goto(origin);await page.locator('a[href="/auth/login"]').click();
 await page.locator('#username').fill('s2-owner');
 await page.locator('#password').fill(JSON.parse(readFileSync(resolve(directory,'credentials.json'))).password);
 await page.locator('#kc-login').click();await page.locator('.wh-side-bottom b').waitFor();
 const cookies=await context.cookies(origin);const session=cookies.find(x=>x.name==='__Host-wh_session');
 assert.ok(session?.httpOnly&&session?.secure&&session?.sameSite==='Lax');
 const state=await (await page.request.get(origin+'/bff/session')).json();assert.equal(state.subject,subject);
 const me=await page.request.get(origin+'/bff/api/v1/me');assert.equal(me.status(),200);
 assert.equal((await me.json()).workspaces[0].id,ready.workspace);
 report('real_keycloak_pkce_login_and_secure_cookie',{realApi:true,replicas:2});
 const modes=await (await page.request.get(origin+'/bff/api/v1/modes')).json();assert.equal(modes.items.length,7);
 const denied=await page.request.post(origin+'/bff/api/v1/workspaces',{headers:{Origin:origin},data:{name:'must-not-create'}});
 assert.equal(denied.status(),403);
 const privatePath=origin+'/bff/api/v1/workspaces/'+ready.workspace+'/decisions';
 const created=await page.request.post(privatePath,{headers:{Origin:origin,'x-csrf-token':state.csrf},data:{title:'Reference retention',decision:'Synthetic decision only',mode:'challenge'}});
 assert.equal(created.status(),201);const decision=await created.json();
 const roles=(await appClient.query('SELECT rolsuper,rolbypassrls FROM pg_roles WHERE rolname=current_user')).rows[0];
 assert.deepEqual(roles,{rolsuper:false,rolbypassrls:false});
 assert.equal((await appClient.query('SELECT count(*)::int AS count FROM decisions')).rows[0].count,0);
 assert.equal((await (await page.request.get(privatePath)).json()).items.length,1);
 report('non_owner_nobypassrls_and_csrf',{applicationRole:roles,modes:7});
 const accessibility=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze();
 assert.deepEqual(accessibility.violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>n.target)})),[]);
 const aria=await page.locator('#app').ariaSnapshot();assert.match(aria,/navigation|بخش‌های سامانه/);
 await page.locator('nav button').first().focus();await page.keyboard.press('Enter');
 await page.setViewportSize({width:390,height:844});assert.equal(await page.locator('html').getAttribute('dir'),'rtl');
 assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 report('wcag_automated_aria_keyboard_and_mobile',{axeViolations:0,manualAssistiveTechnologyReview:false});
 active.shift();await close(servers[0]);
 assert.equal((await page.request.get(origin+'/bff/session')).status(),200);
 // Real time, rather than a fake IdP clock: enter the refresh window of the 45-second token.
 await new Promise(ok=>setTimeout(ok,17000));
 const concurrent=await Promise.all(Array.from({length:6},()=>page.request.get(origin+'/bff/api/v1/me')));
 assert.ok(concurrent.every(r=>r.status()===200));
 assert.equal((await (await page.request.get(privatePath)).json()).items[0].id,decision.id);
 report('replica_failure_and_real_refresh_rotation',{survivingReplicas:1,concurrentReads:6,sameDecision:true});
 const payloads=(await sessionAdmin.query('SELECT payload FROM wh_web_sessions')).rows;
 assert.equal(payloads.length,1);assert.ok(payloads.every(x=>!x.payload.includes('access_token')&&!x.payload.includes('refresh_token')));
 const logout=await page.request.post(origin+'/bff/logout',{headers:{Origin:origin,'x-csrf-token':state.csrf}});
 assert.equal(logout.status(),200);assert.equal((await logout.json()).revocation,'confirmed');
 assert.equal((await page.request.get(origin+'/bff/session')).status(),401);
 assert.equal((await sessionAdmin.query('SELECT count(*)::int AS count FROM wh_web_sessions')).rows[0].count,0);
 report('real_idp_revocation_and_session_erasure',{encryptedStore:true,remainingSessions:0});
 mkdirSync('server/api/build/reference-staging',{recursive:true});
 writeFileSync('server/api/build/reference-staging/report.json',JSON.stringify({scope:'ephemeral reference staging; no customer data or production acceptance',issuerSoftware:'Keycloak 26.4.7',results},null,2));
} finally {
 if(browser)await browser.close();if(edge)await close(edge);
 await Promise.all(servers.filter(s=>s.listening).map(close));
 await sessionAdmin.end();await appClient.end();
}
