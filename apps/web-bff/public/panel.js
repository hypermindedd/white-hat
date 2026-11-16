const app=document.querySelector('#app'),toast=document.querySelector('#toast');
const names={challenge:'چالش',kill:'تصمیم حذف یا ساخت',blind:'نقاط کور',premortem:'پیش‌مرگ',reality:'سنجش واقعیت',compare:'مقایسه',devil:'وکیل مدافع مخالف'};
const errorTexts={PILOT_LIMIT_REACHED:'سهمیهٔ ماهانهٔ پایلوت این فضا تکمیل شده است.',ANALYSIS_PAUSED:'ثبت تحلیل‌های جدید در این فضا موقتاً متوقف است.',AUTH_REQUIRED:'نشست فعال نیست؛ وارد شو.',SESSION_EXPIRED:'نشست منقضی شده؛ دوباره وارد شو.',SESSION_REAUTH_REQUIRED:'اعتبار دسترسی تغییر کرده؛ ورود مجدد لازمه.',CSRF_REJECTED:'نشست یا فرم نامعتبره؛ صفحه رو دوباره بارگذاری کن.',ORIGIN_REJECTED:'مبدأ درخواست پذیرفته نشد.',FORBIDDEN:'دسترسی مجاز نیست.',PERMISSION_DENIED:'دسترسی مجاز نیست.',NOT_FOUND:'منبع پیدا نشد یا اجازه مشاهده نداری.',COMPARE_OBJECTIVE_REQUIRED:'برای مقایسه، هدف تصمیم را وارد کن.',ACTIVE_RUN_EXISTS:'برای این تصمیم یک اجرای باز وجود دارد. وضعیت آن را بررسی کن.',REVISION_CONFLICT:'این تصمیم در جای دیگه تغییر کرده؛ اطلاعات رو تازه‌سازی کن.',IDEMPOTENCY_CONFLICT:'شناسه درخواست با عملیات قبلی ناسازگاره.',PROVIDER_NOT_CONFIGURED:'ارائه‌دهنده AI هنوز در سرور تنظیم نشده.',DAILY_LIMIT_REACHED:'سهمیه روزانه تکمیل شده.',RECONCILIATION_REQUIRED:'وضعیت هزینه/اجرا نامطمئنه؛ نیازمند بررسی اپراتور، بدون تکرار خودکار.'};
const permissionLabels={REVIEW_RUN_INCIDENTS:'پیگیری اجراها',READ_RUN_METRICS:'شاخص‌های عملیات',REVIEW_QUALITY_FEEDBACK:'بازبینی کیفیت'};
const safe=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const nullable=v=>v==null?'نامشخص':new Intl.NumberFormat('fa-IR').format(v);
let decisionLoad=0;
const state={session:null,me:null,modes:[],workspace:null,decisions:[],selected:null,run:null,screen:'overview',poll:null,busy:false};
const path=s=>'/bff/api/v1'+s;
const api=async(url,{method='GET',data,idempotency}={})=>{
 const response=await fetch(url,{method,credentials:'same-origin',headers:{...(data?{'Content-Type':'application/json'}:{}),...(method!=='GET'?{'X-CSRF-Token':state.session?.csrf||'','Origin':location.origin}:{}),...(idempotency?{'Idempotency-Key':idempotency}:{})},...(data?{body:JSON.stringify(data)}:{})});
 let body;try{body=await response.json();}catch{throw new Error('پاسخ سرور معتبر نیست.');}
 if(!response.ok){if(response.status===401&&state.session){state.session=null;renderLogin();}const e=new Error(errorTexts[body.error]||`خطای ${response.status}: ${body.error||'UNKNOWN'}`);e.code=body.error;e.status=response.status;throw e;}
 return body;
};
const showError=e=>{toast.textContent=e.message||String(e);clearTimeout(showError.timer);showError.timer=setTimeout(()=>toast.textContent='',6500);};
function renderLogin(){clearInterval(state.poll);app.innerHTML=`<main class="wh-auth"><div class="wh-mark">WHITE HAT · SECOND MIND</div><h1>اتاق تصمیم</h1><p>برای دسترسی به فضای کاری و تحلیل‌ها، با ارائه‌دهندهٔ هویت سازمان وارد شو.</p><a href="/auth/login">ورود امن با OIDC</a><p><small>هیچ رمز یا توکن دسترسی در مرورگر نگهداری نمی‌شه.</small></p></main>`;}
function navigation(role){
 const items=[['overview','داشبورد']];
 if(role&&role!=='SUPPORT')items.push(['new','تصمیم تازه'],['history','تصمیم‌ها'],['runs','اجراهای من']);
 if(role==='OWNER')items.push(['members','اعضا و نقش‌ها']);
 if(role==='OWNER'||role==='ADMIN')items.push(['usage','گزارش مصرف']);
 if(role==='OWNER'||role==='SUPPORT')items.push(['incidents','پیگیری اجراها'],['operations','شاخص‌های عملیات'],['quality','صف کیفیت']);
 return items;
}
function shell(){const role=state.me.workspaces.find(w=>w.id===state.workspace)?.role;
 const workspaceOptions=state.me.workspaces.map(w=>`<option value="${safe(w.id)}" ${state.workspace===w.id?'selected':''}>${safe(w.name)} — ${safe(w.role)}</option>`).join('');
 app.innerHTML=`<div class="wh-shell"><aside class="wh-aside"><div class="wh-mark">WHITE HAT <small>SECOND MIND · KOTLIN API v1</small></div><nav class="wh-nav" aria-label="بخش‌های سامانه">${navigation(role).map(([k,v])=>`<button data-screen="${k}" aria-current="${state.screen===k?'page':'false'}">${v}</button>`).join('')}</nav><div class="wh-side-bottom">هویت: <b dir="ltr">${safe(state.me.subject)}</b><button id="logout">خروج امن</button></div></aside><main class="wh-main"><header class="wh-top"><div><h1>${safe(({overview:'نمای کلی',new:'تصمیم جدید',history:'آرشیو تصمیم‌ها',runs:'سابقهٔ تحلیل‌ها',incidents:'پنل پشتیبانی',operations:'شاخص‌های عملیات',quality:'صف کیفیت',detail:'جزئیات و اجرای تصمیم',edit:'ویرایش تصمیم',members:'اعضا و مجوزها',usage:'مصرف سازمانی'})[state.screen]||'دستیار تصمیم')}</h1><p>موتور مشترک تصمیم‌گیری · داده‌ها و مجوزها در Kotlin API</p></div><label class="wh-field">فضای کاری <select id="workspaces" class="wh-input"><option value="">یک فضا انتخاب کن</option>${workspaceOptions}</select></label></header><div id="content" aria-live="polite"></div></main></div>`;
 document.querySelector('#workspaces').onchange=async e=>{const workspace=e.target.value||null;state.workspace=workspace;
 state.selected=null;state.run=null;state.decisions=[];state.screen='overview';
 try{await loadDecisions();}catch(error){if(state.workspace===workspace)showError(error);}
 if(state.session&&state.workspace===workspace){render();document.querySelector('#workspaces').focus();}};
 document.querySelector('#logout').onclick=async()=>{try{await api('/bff/logout',{method:'POST'});}catch(e){showError(e);}finally{state.session=null;renderLogin();}};
 document.querySelectorAll('[data-screen]').forEach(b=>b.onclick=async()=>{state.screen=b.dataset.screen;state.selected=null;state.run=null;render();
  document.querySelector('nav [aria-current="page"]')?.focus();});
}
function render(){clearInterval(state.poll);if(!state.session){renderLogin();return;}shell();const root=document.querySelector('#content');
 if(state.screen==='overview')overview(root);
 else if(state.screen==='new'||state.screen==='edit')decisionForm(root);
 else if(state.screen==='history')history(root);
 else if(state.screen==='detail')detail(root);
 else if(state.screen==='members')members(root);
 else if(state.screen==='usage')usage(root);
 else if(state.screen==='runs')runHistory(root);
 else if(state.screen==='incidents')incidents(root);
 else if(state.screen==='operations')operations(root);
 else if(state.screen==='quality')quality(root);
}
function overview(root){
 if(state.me.workspaces.find(w=>w.id===state.workspace)?.role==='SUPPORT'){
  root.innerHTML='<section class="wh-panel"><h2>فضای پشتیبانی</h2><p>از پیگیری اجراها، شاخص‌های عملیات و صف کیفیت در حدود مجوزهای همین فضا استفاده کن.</p></section>';
  return;
 }
 root.innerHTML=`<section class="wh-panel"><h2>یک هسته، هفت زاویه دید</h2><p class="wh-muted">هفت Mode از قرارداد Kotlin خوانده می‌شن. حکم نهایی فقط با پاسخ معتبر API صادر می‌شه.</p><div class="wh-grid">${state.modes.map(m=>`<div class="wh-item"><b>${safe(names[m.id]||m.id)}</b><p class="wh-muted" dir="ltr">${safe(m.id)}</p></div>`).join('')}</div></section><section class="wh-panel"><h2>فضاهای کاری</h2>${state.me.workspaces.length?`<p>${state.me.workspaces.map(w=>safe(w.name)).join(' · ')}</p>`:'<p class="wh-muted">هنوز فضای کاری وجود نداره.</p>'}<form id="create-workspace"><label class="wh-field" for="workspace-name">نام فضای جدید</label><input class="wh-input" id="workspace-name" maxlength="120" required><button type="submit">ایجاد فضای کاری</button></form></section><section class="wh-panel"><h2>تصمیم‌های من</h2><p>${nullable(state.decisions.length)} تصمیم در این فضا</p><div class="wh-actions"><button data-screen="new" ${!state.workspace?'disabled':''}>تصمیم تازه</button><button data-screen="history" ${!state.workspace?'disabled':''}>مشاهده آرشیو</button></div></section>`;
 document.querySelector('#create-workspace').onsubmit=async e=>{e.preventDefault();try{const w=await api(path('/workspaces'),{method:'POST',data:{name:e.target.querySelector('input').value.trim()}});await loadMe();state.workspace=w.id;await loadDecisions();render();}catch(e){showError(e);}};
 root.querySelectorAll('[data-screen]').forEach(b=>b.onclick=()=>{state.screen=b.dataset.screen;render();});
}
const fields=d=>`<label class="wh-field">عنوان <input name="title" class="wh-input" required maxlength="120" value="${safe(d?.title||'')}"></label><label class="wh-field">موضوع تصمیم <textarea name="decision" class="wh-input" required maxlength="16000" rows="5">${safe(d?.decision||'')}</textarea></label><label class="wh-field">هدف <textarea name="objective" class="wh-input" maxlength="2000" rows="2">${safe(d?.objective||'')}</textarea></label><label class="wh-field">زمینه <textarea name="context" class="wh-input" maxlength="12000" rows="3">${safe(d?.context||'')}</textarea></label><label class="wh-field">یادداشت منابع <textarea name="source_notes" class="wh-input" maxlength="10000" rows="2">${safe(d?.source_notes||'')}</textarea></label><label class="wh-field">Mode <select name="mode" class="wh-input">${state.modes.map(m=>`<option value="${safe(m.id)}" ${m.id===(d?.mode||'challenge')?'selected':''}>${safe(names[m.id]||m.id)}</option>`).join('')}</select></label>`;
function decisionForm(root){if(!state.workspace||state.me.workspaces.find(w=>w.id===state.workspace)?.role==='SUPPORT'){root.innerHTML='<p class="wh-warning">برای ثبت تصمیم، فضای کاری با دسترسی مجاز انتخاب کن.</p>';return;}
 const d=state.screen==='edit'?state.selected:null;
 root.innerHTML=`<section class="wh-panel"><h2>${d?'ویرایش با کنترل revision':'ثبت تصمیم'}</h2><form id="decision-form">${fields(d)}<div class="wh-actions"><button type="submit">${d?'ذخیره ویرایش':'ثبت تصمیم'}</button><button type="button" id="form-cancel">انصراف</button></div></form></section>`;
 document.querySelector('#form-cancel').onclick=()=>{state.screen=d?'detail':'history';render();};
 document.querySelector('#decision-form').onsubmit=async e=>{e.preventDefault();if(state.busy)return;state.busy=true;const btn=e.target.querySelector('[type=submit]');btn.disabled=true;
 const input=Object.fromEntries(new FormData(e.target));try{const saved=await api(path(`/workspaces/${state.workspace}/decisions${d?'/'+d.id:''}`),{method:d?'PUT':'POST',data:d?{...input,expected_revision:d.revision}:input});state.selected=saved;state.run=null;await loadDecisions();state.screen='detail';}catch(error){showError(error);btn.disabled=false;return;}finally{state.busy=false;}render();};
}
function history(root){if(!state.workspace){root.innerHTML='<p class="wh-warning">یک فضای کاری انتخاب کن.</p>';return;}
 root.innerHTML=`<section class="wh-panel"><h2>تصمیم‌های قابل مشاهده</h2><p class="wh-muted">سرور فقط تصمیم‌هایی رو نمایش می‌ده که اجازه خواندنشون رو داری.</p>${state.decisions.length?`<div class="wh-grid">${state.decisions.map(d=>`<button class="wh-item" data-decision="${safe(d.id)}"><b>${safe(d.title)}</b><p>${safe(names[d.mode]||d.mode)} · نسخه ${nullable(d.revision)}</p></button>`).join('')}</div>`:'<p class="wh-muted">هنوز تصمیمی ثبت نشده.</p>'}</section>`;
 root.querySelectorAll('[data-decision]').forEach(b=>b.onclick=async()=>{try{state.selected=await api(path(`/workspaces/${state.workspace}/decisions/${b.dataset.decision}`));state.run=null;state.screen='detail';render();}catch(e){showError(e);}});
}
const terminal=new Set(['succeeded','failed','cancelled','reconciliation_required']);
function detail(root){const d=state.selected;if(!d){state.screen='history';return history(root);}root.innerHTML=`<section class="wh-panel"><h2>${safe(d.title)} <span class="wh-state">revision ${nullable(d.revision)}</span></h2><p>${safe(d.decision)}</p><p class="wh-muted">${safe(names[d.mode]||d.mode)}</p><div class="wh-actions"><button id="edit">ویرایش</button><button id="analyze" ${state.busy?'disabled':''}>اجرای تحلیل</button></div></section><section class="wh-panel" id="run-panel"><h2>آخرین اجرای این نشست</h2>${renderRun()}</section>`;
 root.querySelector('#edit').onclick=()=>{state.screen='edit';render();};
 root.querySelector('#analyze').onclick=async e=>{if(state.busy)return;state.busy=true;e.target.disabled=true;try{state.run=await api(path(`/workspaces/${state.workspace}/decisions/${d.id}/runs`),{method:'POST',data:{expected_revision:d.revision},idempotency:crypto.randomUUID()});}catch(error){showError(error);e.target.disabled=false;return;}finally{state.busy=false;}render();};
 bindRun(root);if(state.run&&!terminal.has(state.run.status))state.poll=setInterval(()=>pollRun().catch(showError),3000);
}
function renderRun(){const r=state.run;if(!r)return '<p class="wh-muted">هنوز اجرایی انتخاب نشده. برای بازیابی یک اجرا، «اجراهای من» را باز کن.</p>';
 return `<p>وضعیت: <span class="wh-state">${safe(r.status)}</span></p>${r.status==='reconciliation_required'?'<p class="wh-warning">نتیجه نامطمئن است؛ تحلیل مجدد به‌شکل خودکار اجرا نمی‌شه. اپراتور باید receipt و هزینه رو بررسی کنه.</p>':''}${r.error?`<p class="wh-error">${safe(r.error)}</p>`:''}<p>توکن ورودی: ${nullable(r.input_tokens)} | توکن خروجی: ${nullable(r.output_tokens)}</p>${r.result!=null?`<pre class="wh-result">${safe(JSON.stringify(r.result,null,2))}</pre>`:''}${terminal.has(r.status)?'':`<div class="wh-actions"><button id="cancel-run">درخواست لغو</button><button id="refresh-run">به‌روزرسانی</button></div>`}`;
}
async function bindFeedback(root){
 const run=state.run, workspace=state.workspace;
 if(run?.status!=='succeeded'||root.querySelector('#run-feedback'))return;
 try{
   const settings=await api(path(`/workspaces/${workspace}/feedback-settings`));
   if(!root.isConnected||root.querySelector('#run-feedback')||state.run?.id!==run.id)return;
   const section=document.createElement('section');
   section.id='run-feedback';
   section.innerHTML=`<form><label>امتیاز داوطلبانه <select name="rating" required><option value="">انتخاب کن</option>${[1,2,3,4,5].map(n=>`<option>${n}</option>`).join('')}</select></label><button type="submit">ثبت امتیاز برای بازبینی کیفیت</button></form><button type="button">پس‌گرفتن امتیاز</button>`;
   if(!settings.enabled)section.querySelector('form').hidden=true;
   const target=path(`/workspaces/${workspace}/feedback/${run.id}`);
   section.querySelector('form').onsubmit=async event=>{
     event.preventDefault();
     try{await api(target,{method:'PUT',data:{rating:Number(new FormData(event.target).get('rating'))}});toast.textContent='امتیاز ثبت شد.';}catch(error){showError(error);}
   };
   section.querySelector('[type=button]').onclick=async()=>{
     try{await api(target+'/withdraw',{method:'POST',data:{}});toast.textContent='امتیاز حذف شد.';}catch(error){showError(error);}
   };
   root.append(section);
 }catch(error){if(error.status!==403)showError(error);}
}
function bindRun(root){
 bindFeedback(root.querySelector('#run-panel')||root);
root.querySelector('#cancel-run')?.addEventListener('click',async()=>{try{state.run=await api(path(`/workspaces/${state.workspace}/runs/${state.run.id}/cancel`),{method:'POST',data:{}});render();}catch(e){showError(e);}});
 root.querySelector('#refresh-run')?.addEventListener('click',()=>pollRun().catch(showError));}
async function pollRun(){if(!state.run||!state.workspace)return;const id=state.run.id;const r=await api(path(`/workspaces/${state.workspace}/runs/${id}`));if(!state.run||state.run.id!==id)return;state.run=r;if(state.screen==='detail'){const el=document.querySelector('#run-panel');if(el){el.innerHTML='<h2>آخرین اجرای این نشست</h2>'+renderRun();bindRun(el);}}if(terminal.has(r.status))clearInterval(state.poll);}
async function members(root){const w=state.me.workspaces.find(x=>x.id===state.workspace);if(!w){root.innerHTML='<p class="wh-warning">یک فضای کاری انتخاب کن.</p>';return;}
 if(w.role!=='OWNER'){root.innerHTML='<p class="wh-warning">تغییر عضویت و مدیریت قالب‌های نقش در اختیار OWNER است.</p>';return;}
 root.innerHTML=`<section class="wh-panel"><h2>عضویت و نقش‌ها</h2><p class="wh-warning">نقش‌ها از PostgreSQL کنترل می‌شن؛ پشتیبان پیش‌فرض به متن تصمیم‌ها یا گزارش مالی دسترسی نداره. دسترسی پیگیری اجراها برای هر پشتیبان جداگانه تنظیم می‌شود.</p><form id="member-form"><label class="wh-field">شناسه subject در IdP <input name="subject" class="wh-input" required maxlength="200"></label><label class="wh-field">نقش <select name="role" class="wh-input"><option>MEMBER</option><option>ADMIN</option><option>SUPPORT</option></select></label><label class="wh-field"><input type="checkbox" name="active" checked> عضویت فعال</label><button type="submit">ثبت تغییر عضویت</button></form></section><section class="wh-panel" id="member-list" aria-label="فهرست اعضا"></section><section class="wh-panel" id="support-roles"></section>`;
 root.querySelector('form').onsubmit=async e=>{e.preventDefault();const f=new FormData(e.target);try{await api(path(`/workspaces/${state.workspace}/members`),{method:'POST',data:{subject:f.get('subject'),role:f.get('role'),active:f.has('active')}});toast.textContent='تغییر عضویت در سرور ثبت شد.';await members(root);}catch(error){showError(error);}};
 const list=root.querySelector('#member-list');
 try {
   const [{items},{items:roles}]=await Promise.all([
     api(path(`/workspaces/${w.id}/members`)),api(path(`/workspaces/${w.id}/support-roles`))
   ]);
   list.innerHTML=`<h2>فهرست اعضا</h2><ul>${items.map(m=>`<li><bdi>${safe(m.subject)}</bdi> · ${safe(m.role)} · ${m.active?'فعال':'غیرفعال'} ${m.active&&m.role==='SUPPORT'?Object.entries(permissionLabels).map(([permission,label])=>{
     const enabled=(m.permissions||[]).includes(permission);
     return `<button data-subject="${safe(m.subject)}" data-permission="${permission}" data-enable="${!enabled}">${enabled?'لغو':'اعطا'} · ${label}</button>`;
   }).join(' ')+` <label>نقش پشتیبان <select data-assign="${safe(m.subject)}"><option value="">بدون نقش سفارشی</option>${roles.map(role=>`<option value="${safe(role.id)}" ${role.id===m.support_role_id?'selected':''}>${safe(role.name)}</option>`).join('')}</select></label>`:''}</li>`).join('')}</ul>`;
   for(const btn of list.querySelectorAll('button'))btn.onclick=async()=>{
     btn.disabled=true;
     try {
       await api(path(`/workspaces/${w.id}/operator-permissions`),{method:'PUT',data:{subject:btn.dataset.subject,permission:btn.dataset.permission,enabled:btn.dataset.enable==='true'}});
       await members(root);
     } catch(error){btn.disabled=false;showError(error);}
   };
   for(const select of list.querySelectorAll('[data-assign]'))select.onchange=async()=>{
     select.disabled=true;
     try {
       await api(path(`/workspaces/${w.id}/support-role-assignment`),{method:'PUT',data:{subject:select.dataset.assign,role_id:select.value||null}});
       await members(root);
     } catch(error){showError(error);await members(root);}
   };
   const section=root.querySelector('#support-roles');
   section.innerHTML=`<h2>نقش‌های سفارشی پشتیبانی</h2><form><label>نقش <select name="role"><option value="">نقش تازه</option>${roles.map(role=>`<option value="${safe(role.id)}">${safe(role.name)}</option>`).join('')}</select></label><label>نام نقش <input class="wh-input" name="name" maxlength="60" required></label><fieldset><legend>مجوزهای نقش</legend>${Object.entries(permissionLabels).map(([id,label])=>`<label><input type="checkbox" name="permission" value="${id}"> ${label}</label>`).join(' ')}</fieldset><button type="submit">ذخیرهٔ نقش</button></form>`;
   const form=section.querySelector('form');
   form.elements.role.onchange=()=>{
     const role=roles.find(item=>item.id===form.elements.role.value);
     form.elements.name.value=role?.name||'';
     for(const box of form.querySelectorAll('[name=permission]'))box.checked=role?.permissions.includes(box.value)||false;
   };
   form.onsubmit=async event=>{
     event.preventDefault();
     const submit=form.querySelector('button');
     const data=new FormData(form), id=data.get('role')||crypto.randomUUID();
     submit.disabled=true;
     try {
       await api(path(`/workspaces/${w.id}/support-roles/${id}`),{method:'PUT',data:{name:data.get('name'),permissions:data.getAll('permission')}});
       await members(root);
     }catch(error){submit.disabled=false;showError(error);}
   };
 } catch(error){list.textContent='فهرست اعضا در دسترس نیست.';showError(error);}
}

async function runHistory(root){
 const workspace=state.workspace;
 if(!workspace){root.textContent='ابتدا یک فضای کاری انتخاب کن.';return;}
 root.textContent='در حال دریافت اجراها…';
 try{
   const {items}=await api(path(`/workspaces/${workspace}/runs`));
   root.innerHTML=`<section class="wh-panel"><h2>۱۰۰ اجرای اخیر من</h2>${items.length?`<ul>${items.map(run=>`<li><button data-run="${safe(run.id)}">${safe(run.status)} · <bdi>${safe(run.id)}</bdi></button></li>`).join('')}</ul>`:'<p>هنوز اجرایی ثبت نشده.</p>'}</section>`;
   for(const btn of root.querySelectorAll('[data-run]'))btn.onclick=async()=>{
     try {
       const run=await api(path(`/workspaces/${workspace}/runs/${btn.dataset.run}`));
       const decision=await api(path(`/workspaces/${workspace}/decisions/${run.decision_id}`));
       if(state.workspace!==workspace||state.screen!=='runs')return;
       state.selected=decision;state.run=run;state.screen='detail';render();
     }catch(error){showError(error);}
   };
 }catch(error){root.textContent='دریافت سابقهٔ اجرا ممکن نیست.';showError(error);}
}

async function incidents(root){
 const workspace=state.workspace;
 root.textContent='در حال دریافت صف پیگیری…';
 try{
   const {items}=await api(path(`/workspaces/${workspace}/incidents`));
   const labels={FOLLOW_UP_REQUIRED:'نیازمند پیگیری',RECEIPT_REVIEWED:'رسید بررسی شد'};
   root.innerHTML=`<section class="wh-panel"><h2>صف اجراهای نامطمئن</h2><p>ثبت پیگیری وضعیت اجرا یا مصرف را تغییر نمی‌دهد.</p>${items.length?`<ul>${items.map(item=>`<li><bdi>${safe(item.run_id)}</bdi> · ${safe(labels[item.disposition]||'بررسی نشده')} <button data-incident="${safe(item.run_id)}" data-value="FOLLOW_UP_REQUIRED">نیاز به پیگیری</button> <button data-incident="${safe(item.run_id)}" data-value="RECEIPT_REVIEWED">ثبت بررسی رسید</button></li>`).join('')}</ul>`:'<p>اجرای نامطمئنی وجود ندارد.</p>'}</section>`;
   for(const btn of root.querySelectorAll('[data-incident]'))btn.onclick=async()=>{
     btn.disabled=true;
     try{
       await api(path(`/workspaces/${workspace}/incidents/${btn.dataset.incident}`),{method:'PUT',data:{disposition:btn.dataset.value}});
       await incidents(root);
     }catch(error){btn.disabled=false;showError(error);}
   };
 }catch(error){root.textContent='صف پیگیری در دسترس نیست.';showError(error);}

}
async function operations(root){
 try{
   const result=await api(path(`/workspaces/${state.workspace}/operations-metrics`));
   const labels={runs:'اجرا',failed_runs:'ناموفق',uncertain_runs:'نامطمئن',latency_samples:'نمونه‌های تکمیل‌شده',p95_end_to_end_ms:'زمان سرتاسری P95 (میلی‌ثانیه)'};
   root.innerHTML=`<section class="wh-panel"><h2>شاخص‌های ۳۰ روز اخیر</h2><ul>${Object.entries(result).map(([key,value])=>`<li>${safe(labels[key])} · ${nullable(value)}</li>`).join('')}</ul></section>`;
 }catch(error){root.textContent='شاخص‌ها در دسترس نیست.';showError(error);}
}
async function quality(root){
 const workspace=state.workspace;
 const target=path(`/workspaces/${workspace}/quality-queue`);
 try{
   const {items}=await api(target);
   root.innerHTML=`<section class="wh-panel"><h2>امتیازهای داوطلبانه</h2>${items.length?`<ul>${items.map(item=>`<li><bdi>${safe(item.run_id)}</bdi> · ${nullable(item.rating)} از ۵ ${item.reviewed?'· بررسی شد':`<button data-review="${safe(item.run_id)}">علامت‌گذاری بررسی</button>`}</li>`).join('')}</ul>`:'<p>صف خالی است.</p>'}</section>`;
   for(const button of root.querySelectorAll('[data-review]'))button.onclick=async()=>{
     button.disabled=true;
     try{await api(`${target}/${button.dataset.review}/review`,{method:'PUT',data:{}});await quality(root);}catch(error){button.disabled=false;showError(error);}
   };
   if(state.me.workspaces.find(w=>w.id===workspace)?.role==='OWNER'){
     const targetSettings=path(`/workspaces/${workspace}/feedback-settings`);
     const {enabled}=await api(targetSettings);
     const toggle=document.createElement('button');toggle.textContent=enabled?'توقف دریافت امتیاز':'فعال‌کردن دریافت امتیاز';
     toggle.onclick=async()=>{
       toggle.disabled=true;
       try{await api(targetSettings,{method:'PUT',data:{enabled:!enabled}});await quality(root);}catch(error){toggle.disabled=false;showError(error);}
     };root.append(toggle);
     const retention=path(`/workspaces/${workspace}/feedback-retention`);
     const {days}=await api(retention);
     if(!root.isConnected)return;
     const form=document.createElement('form');
     form.innerHTML=`<label>نگهداری امتیاز (روز) <input class="wh-input" type="number" name="days" min="1" max="365" required value="${days??''}"></label><button type="submit">ذخیرهٔ دورهٔ نگهداری</button><button type="button" ${days===null?'disabled':''}>حذف امتیازهای منقضی</button>`;
     form.onsubmit=async event=>{
       event.preventDefault();
       try{await api(retention,{method:'PUT',data:{days:Number(form.elements.days.value)}});await quality(root);}catch(error){showError(error);}
     };
     form.querySelector('[type=button]').onclick=async()=>{
       try{
         const result=await api(retention+'/purge',{method:'POST',data:{}});
         toast.textContent=`تعداد امتیازهای حذف‌شده: ${nullable(result.deleted)}`;
         await quality(root);
       }catch(error){showError(error);}
     };
     root.append(form);
   }
 }catch(error){root.textContent='صف کیفیت در دسترس نیست.';showError(error);}
}
async function usage(root){
 const workspace=state.workspace,w=state.me.workspaces.find(x=>x.id===workspace);
 if(!w||!['OWNER','ADMIN'].includes(w.role)){root.textContent='گزارش مصرف برای این فضا در دسترس نیست.';return;}
 const target=path(`/workspaces/${workspace}/admin`);
 root.textContent='در حال دریافت گزارش مصرف…';
 try{
   const [result,cost]=await Promise.all([api(target+'/usage'),api(target+'/costs')]);
   const labels={runs:'اجراها',input_tokens:'توکن ورودی',output_tokens:'توکن خروجی',uncertain_runs:'اجراهای نامطمئن'};
   root.innerHTML=`<section class="wh-panel"><h2>مصرف</h2><ul>${Object.entries(result).map(([key,value])=>`<li>${safe(labels[key])} · ${nullable(value)}</li>`).join('')}</ul><h2>رسیدهای ثبت‌شده در ۳۰ روز اخیر</h2><p>پوشش رسید: ${nullable(cost.confirmed_runs)} از ${nullable(cost.runs)} اجرا</p><p>جمع ثبت‌شده USD: ${cost.confirmed_usd_micros==null?'نامشخص':(cost.confirmed_usd_micros/1e6).toFixed(6)}</p>${w.role==='OWNER'?'<form id="cost-form"><label>شناسه اجرا <input class="wh-input" name="run" required></label><label>هزینه به میلیونیم دلار <input class="wh-input" name="micros" type="number" min="0" max="1000000000000" step="1" required></label><label>شناسهٔ غیرمحرمانهٔ رسید <input class="wh-input" name="reference" maxlength="120" required></label><button type="submit">ثبت مبلغ مستند</button></form>':''}</section>`;
   if(w.role==='OWNER'){
     const setting=path(`/workspaces/${workspace}/analysis-settings`);
     const {paused}=await api(setting);
     if(!root.isConnected)return;
     const controls=document.createElement('section');
     controls.className='wh-panel';
     controls.innerHTML=`<h2>کنترل تحلیل</h2><button type="button">${paused?'ازسرگیری تحلیل‌های جدید':'توقف ثبت تحلیل جدید'}</button>
     <h2>اصلاح رسید</h2><form><label>شناسه اجرا <input class="wh-input" name="run" required></label>
     <label>مبلغ فعلی (میلیونیم دلار) <input class="wh-input" type="number" name="old" min="0" max="1000000000000" required></label>
     <label>مبلغ اصلاح‌شده <input class="wh-input" type="number" name="updated" min="0" max="1000000000000" required></label>
     <label>شناسه رسید <input class="wh-input" name="receipt" maxlength="120" required></label><button>ثبت اصلاح</button></form>`;
     const toggle=controls.querySelector('[type=button]');
     toggle.onclick=async()=>{
       toggle.disabled=true;
       try{await api(setting,{method:'PUT',data:{paused:!paused}});await usage(root);}
       catch(error){toggle.disabled=false;showError(error);}
     };
     const correction=controls.querySelector('form');
     let previousKey, key;
     correction.onsubmit=async event=>{
       event.preventDefault();
       const fields=new FormData(correction),run=String(fields.get('run')).trim();
       if(!/^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(run)){showError(new Error('شناسهٔ اجرا معتبر نیست.'));return;}
       const body={expected_usd_micros:Number(fields.get('old')),usd_micros:Number(fields.get('updated')),receipt_reference:fields.get('receipt')};
       const payload=JSON.stringify([run,body]);
       if(previousKey!==payload){previousKey=payload;key=crypto.randomUUID();}
       const submit=correction.querySelector('button');submit.disabled=true;
       try{await api(target+`/costs/${run}/corrections/${key}`,{method:'PUT',data:body});await usage(root);}
       catch(error){submit.disabled=false;showError(error);}
     };
     root.append(controls);
   }
   const form=root.querySelector('#cost-form');
   if(form)form.onsubmit=async event=>{
     event.preventDefault();
     const data=new FormData(form),run=String(data.get('run')).trim();
     if(!/^[0-9a-f-]{36}$/i.test(run)){showError(new Error('شناسهٔ اجرا معتبر نیست.'));return;}
     const button=form.querySelector('button');button.disabled=true;
     try{await api(target+'/costs/'+run,{method:'PUT',data:{usd_micros:Number(data.get('micros')),receipt_reference:data.get('reference')}});await usage(root);}catch(error){button.disabled=false;showError(error);}
   };
 }catch(error){root.textContent='دریافت گزارش ممکن نیست.';showError(error);}
}
async function loadMe(){const [me,modes]=await Promise.all([api(path('/me')),api(path('/modes'))]);state.me=me;state.modes=modes.items; if(!me.workspaces.some(w=>w.id===state.workspace))state.workspace=me.workspaces[0]?.id||null;}
async function loadDecisions(){const load=++decisionLoad;if(!state.workspace||state.me.workspaces.find(w=>w.id===state.workspace)?.role==='SUPPORT'){state.decisions=[];return;}const workspace=state.workspace;const data=await api(path(`/workspaces/${workspace}/decisions`));if(state.workspace===workspace&&load===decisionLoad)state.decisions=data.items;}
async function boot(){try{state.session=await api('/bff/session');await loadMe();await loadDecisions();render();}catch(e){if(e.status!==401)showError(e);renderLogin();}}
boot();
