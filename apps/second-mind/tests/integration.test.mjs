import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {randomUUID} from 'node:crypto';
import {createApplication} from '../server/app.mjs';
import {FixtureProvider,validResult} from './fixtures.mjs';

const password='test-only-password-42';
async function fixture(t,provider=new FixtureProvider()){
 const dir=mkdtempSync(join(tmpdir(),'second-mind-test-')),app=createApplication({dataDir:dir,provider}),extraApps=[];
 await new Promise(r=>app.server.listen(0,'127.0.0.1',r));const origin=`http://127.0.0.1:${app.server.address().port}`;
 t.after(async()=>{await app.close();for(const extra of extraApps)await extra.close();rmSync(dir,{recursive:true,force:true});});
 const client=()=>({cookie:'',csrf:'',async request(path,{method='GET',data,headers={},originOverride}={}){
  const res=await fetch(origin+path,{method,headers:{Origin:originOverride??origin,...(this.cookie?{Cookie:this.cookie}:{}),...(this.csrf?{'X-CSRF-Token':this.csrf}:{}),...(data?{'Content-Type':'application/json'}:{}),...headers},...(data?{body:JSON.stringify(data)}:{})});
  const cookie=res.headers.get('set-cookie');if(cookie)this.cookie=cookie.split(';')[0];const body=(res.headers.get('content-type')||'').includes('json')?await res.json():await res.text();if(body.csrf)this.csrf=body.csrf;return {status:res.status,body,headers:res.headers};
 }});
 const admin=client();const setup=await admin.request('/api/setup',{method:'POST',data:{username:'founder',display_name:'مدیر آزمون',password}});assert.equal(setup.status,201);assert.match(setup.headers.get('set-cookie'),/HttpOnly/);assert.match(setup.headers.get('set-cookie'),/SameSite=Strict/);
 return {app,provider,dir,origin,admin,client,extraApps};
}
async function draft(client,overrides={}){const r=await client.request('/api/challenges',{method:'POST',data:{title:'تصمیم آزمون',decision:'برای محصول جدید قبل از دریافت پرداخت ساخت کامل را آغاز کنیم؟',objective:'درآمد تکرارشونده با زیان محدود',mode:'challenge',...overrides}});assert.equal(r.status,201);return r.body.id;}
async function finished(client,id){for(let i=0;i<100;i++){const r=await client.request('/api/challenges/'+id);if(r.body.status!=='running')return r.body;await new Promise(r=>setTimeout(r,5));}throw new Error('Fixture job did not finish');}
const analyze=(client,id,key=randomUUID())=>client.request(`/api/challenges/${id}/analyze`,{method:'POST',headers:{'Idempotency-Key':key}});

test('setup creates one admin; password hashed and session protected',async t=>{
 const {admin,app}=await fixture(t);const row=app.db.prepare('SELECT * FROM users').get();assert.notEqual(row.password_hash,password);assert.match(row.password_hash,/^scrypt:/);
 const r=await admin.request('/api/setup',{method:'POST',data:{username:'another',display_name:'نام',password}});assert.equal(r.status,409);
 const me=await admin.request('/api/me');assert.equal(me.body.user.role,'admin');assert.equal(me.body.modes.length,7);assert.equal('password_hash'in me.body.user,false);
});
test('unauthenticated access and cross-origin writes are rejected',async t=>{
 const {client,admin}=await fixture(t);assert.equal((await client().request('/api/stats')).status,401);
 assert.equal((await admin.request('/api/challenges',{method:'POST',data:{},originOverride:'https://attacker.invalid'})).status,403);
 const csrf=admin.csrf;admin.csrf='';assert.equal((await admin.request('/api/challenges',{method:'POST',data:{}})).status,403);admin.csrf=csrf;
});
test('members cannot access another owner or admin routes',async t=>{
 const {admin,client}=await fixture(t);const id=await draft(admin);
 assert.equal((await admin.request('/api/admin/users',{method:'POST',data:{username:'member',display_name:'عضو',password,role:'member'}})).status,201);
 const member=client();assert.equal((await member.request('/api/login',{method:'POST',data:{username:'member',password}})).status,200);
 assert.equal((await member.request('/api/challenges/'+id)).status,404);
 assert.equal((await member.request('/api/challenges/'+id+'/export')).status,404);
 assert.equal((await member.request('/api/admin/settings')).status,403);
 assert.equal((await member.request('/api/challenges')).body.items.length,0);
});
test('drafts persist in SQLite and input edits invalidate old conversational context',async t=>{
 const {admin,app}=await fixture(t);const id=await draft(admin);await analyze(admin,id);await finished(admin,id);
 assert.ok(app.db.prepare('SELECT COUNT(*) n FROM messages').get().n>0);
 assert.equal((await admin.request('/api/challenges/'+id,{method:'PATCH',data:{decision:'بودجه تصحیح‌شده: ۲۰ میلیون'}})).status,200);
 const detail=(await admin.request('/api/challenges/'+id)).body;assert.equal(detail.status,'draft');assert.equal(detail.messages.length,0);assert.equal(detail.runs.length,1);assert.equal(detail.decision,'بودجه تصحیح‌شده: ۲۰ میلیون');
});
test('missing provider never generates a fake analysis',async t=>{
 const {admin,provider}=await fixture(t,new FixtureProvider({configured:false}));const id=await draft(admin);const r=await analyze(admin,id);assert.equal(r.status,409);assert.equal(r.body.error,'PROVIDER_NOT_CONFIGURED');assert.equal(provider.calls.length,0);assert.equal((await admin.request('/api/challenges/'+id)).body.runs.length,0);
});
test('idempotency prevents duplicate model calls and rejects key reuse for changed requests',async t=>{
 const {admin,provider}=await fixture(t,new FixtureProvider({delay:20}));const id=await draft(admin),key=randomUUID();
 const a=await analyze(admin,id,key),b=await analyze(admin,id,key);assert.equal(a.body.run_id,b.body.run_id);assert.equal(b.body.reused,true);
 const second=await draft(admin,{title:'تصمیم دیگر'});assert.equal((await analyze(admin,second,key)).status,409);
 const d=await finished(admin,id);assert.equal(d.status,'completed');assert.equal(d.runs[0].model,'fixture-only-not-live');assert.equal(provider.calls.length,1);assert.equal(d.runs[0].input_tokens,100);
});
test('daily quota is enforced even across separate decisions',async t=>{
 const {admin,provider}=await fixture(t,new FixtureProvider({delay:20}));await admin.request('/api/admin/settings',{method:'PATCH',data:{daily_limit:1}});const a=await draft(admin),b=await draft(admin);assert.equal((await analyze(admin,a)).status,202);assert.equal((await analyze(admin,b)).status,429);await finished(admin,a);assert.equal(provider.calls.length,1);
});
test('failed paid attempts consume request quota and never become successful output',async t=>{
 const bad=validResult();bad.mode='compare';const {admin}=await fixture(t,new FixtureProvider({result:bad}));await admin.request('/api/admin/settings',{method:'PATCH',data:{daily_limit:1}});const id=await draft(admin);await analyze(admin,id);const d=await finished(admin,id);assert.equal(d.status,'failed');assert.equal(d.runs[0].error_code,'INVALID_MODEL_OUTPUT');assert.equal(d.runs[0].result,null);assert.equal((await analyze(admin,id)).status,429);
});
test('followup preserves active decision and records the new user evidence',async t=>{
 const {admin,provider}=await fixture(t);const id=await draft(admin);await analyze(admin,id);await finished(admin,id);
 await admin.request(`/api/challenges/${id}/followups`,{method:'POST',data:{message:'بودجه تصحیح شد؛ فقط ۲۰ میلیون داریم.'},headers:{'Idempotency-Key':randomUUID()}});const d=await finished(admin,id);assert.equal(d.runs.length,2);assert.ok(provider.calls[1].history.some(m=>m.role==='user'&&m.content.includes('۲۰ میلیون')));assert.equal(provider.calls[1].challenge.id,id);
});
test('human evaluations require all 15 evidence entries and cannot approve release',async t=>{
 const {admin}=await fixture(t);const id=await draft(admin);await analyze(admin,id);const d=await finished(admin,id),run=d.runs[0];
 assert.equal((await admin.request(`/api/admin/evaluations/${run.id}`,{method:'POST',data:{scores:{},evidence:{}}})).status,400);
 const scores={},evidence={};for(let i=1;i<=15;i++){const key='D'+String(i).padStart(2,'0');scores[key]=4;evidence[key]='شاهد آزمون انسانی به خروجی ارجاع دارد.';}scores.D03=2;
 const r=await admin.request(`/api/admin/evaluations/${run.id}`,{method:'POST',data:{scores,evidence}});assert.equal(r.status,200);assert.equal(r.body.critical_pass,false);assert.equal(r.body.release_approved,false);assert.ok(r.body.weighted_score>3.7);
 const review=await admin.request(`/api/admin/runs/${run.id}`);assert.equal(review.body.result.verdict,run.result.verdict);
});
test('provider credentials never enter database, audit or client responses',async t=>{
 const {admin,app}=await fixture(t);const secret='test-only-secret-not-a-real-api-key';const r=await admin.request('/api/admin/settings',{method:'PATCH',data:{api_key:secret,model:'model-test'}});assert.equal(r.status,200);assert.equal(JSON.stringify(r.body).includes(secret),false);
 for(const table of ['settings','audit'])assert.equal(JSON.stringify(app.db.prepare(`SELECT * FROM ${table}`).all()).includes(secret),false);
});
test('last admin cannot be disabled; disabled member sessions are revoked',async t=>{
 const {admin,client}=await fixture(t),me=(await admin.request('/api/me')).body.user;assert.equal((await admin.request('/api/admin/users/'+me.id,{method:'PATCH',data:{active:false}})).status,409);
 const r=await admin.request('/api/admin/users',{method:'POST',data:{username:'disabled',display_name:'عضو',password}}),member=client();await member.request('/api/login',{method:'POST',data:{username:'disabled',password}});await admin.request('/api/admin/users/'+r.body.id,{method:'PATCH',data:{active:false}});assert.equal((await member.request('/api/me')).status,401);
});
test('audit is append-only and delete removes owned decision data',async t=>{
 const {admin,app}=await fixture(t);const id=await draft(admin);await analyze(admin,id);await finished(admin,id);assert.throws(()=>app.db.exec('DELETE FROM audit'),/append_only/);assert.throws(()=>app.db.exec("UPDATE audit SET action='x'"),/append_only/);
 const exported=await admin.request('/api/challenges/'+id+'/export');assert.equal(exported.status,200);assert.equal(exported.body.id,id);assert.equal(exported.body.runs.length,1);assert.equal(exported.body.runs[0].result.mode,'challenge');
 assert.equal((await admin.request('/api/challenges/'+id,{method:'DELETE'})).status,200);assert.equal(app.db.prepare('SELECT COUNT(*) n FROM runs').get().n,0);assert.equal(app.db.prepare('SELECT COUNT(*) n FROM messages').get().n,0);assert.equal((await admin.request('/api/challenges/'+id)).status,404);
});
test('bad credentials and invalid modes are refused',async t=>{
 const {admin,client}=await fixture(t);assert.equal((await client().request('/api/login',{method:'POST',data:{username:'founder',password:'incorrect'}})).status,401);
 const r=await admin.request('/api/challenges',{method:'POST',data:{decision:'تصمیم',mode:'hidden-agent'}});assert.equal(r.status,400);
 const id=await draft(admin,{mode:'compare',objective:''});assert.equal((await analyze(admin,id)).body.error,'COMPARE_OBJECTIVE_REQUIRED');
});
test('public shell has security headers and cannot serve database or source files',async t=>{
 const {client}=await fixture(t),guest=client(),r=await guest.request('/');assert.equal(r.status,200);assert.match(r.headers.get('content-security-policy'),/frame-ancestors 'none'/);assert.equal(r.headers.get('x-content-type-options'),'nosniff');assert.equal((await guest.request('/server/core/system.md')).status,404);assert.equal((await guest.request('/second-mind.sqlite')).status,404);
});
test('database and owned drafts survive a server restart',async t=>{
 const f=await fixture(t),id=await draft(f.admin,{title:'ماندگار پس از restart'});await f.app.close();
 const restarted=createApplication({dataDir:f.dir,provider:new FixtureProvider()});
 await new Promise(r=>restarted.server.listen(0,'127.0.0.1',r));f.extraApps.push(restarted);
 const origin=`http://127.0.0.1:${restarted.server.address().port}`;
 const login=await fetch(origin+'/api/login',{method:'POST',headers:{Origin:origin,'Content-Type':'application/json'},body:JSON.stringify({username:'founder',password})});assert.equal(login.status,200);
 const res=await fetch(origin+'/api/challenges/'+id,{headers:{Cookie:login.headers.get('set-cookie').split(';')[0]}});const item=await res.json();assert.equal(item.title,'ماندگار پس از restart');
});
test('invalid combined settings never partially apply the daily limit',async t=>{
 const {admin}=await fixture(t);const before=(await admin.request('/api/admin/settings')).body.daily_limit;
 const res=await admin.request('/api/admin/settings',{method:'PATCH',data:{daily_limit:1,model:'invalid model spaces'}});assert.equal(res.status,400);
 assert.equal((await admin.request('/api/admin/settings')).body.daily_limit,before);
});
test('mode switch preserves the decision and truth context while changing analytic mode',async t=>{
 const {admin,provider}=await fixture(t),id=await draft(admin);await analyze(admin,id);const before=await finished(admin,id);
 assert.equal((await admin.request(`/api/challenges/${id}/mode`,{method:'POST',data:{mode:'premortem'}})).status,200);
 const switched=(await admin.request('/api/challenges/'+id)).body;assert.equal(switched.decision,before.decision);assert.equal(switched.messages.length,before.messages.length);assert.equal(switched.mode,'premortem');
 await analyze(admin,id);const after=await finished(admin,id);assert.equal(after.runs.length,2);assert.equal(provider.calls[1].challenge.mode,'premortem');assert.ok(provider.calls[1].history.some(m=>m.role==='assistant'));
});
