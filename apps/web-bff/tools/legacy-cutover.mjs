/** Prepare a SQLite snapshot for an isolated PostgreSQL cutover rehearsal. */
import {DatabaseSync} from 'node:sqlite';
import {createHash} from 'node:crypto';
import {existsSync,readFileSync,statSync} from 'node:fs';

const uuid=/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
const modes=new Set(['challenge','kill','blind','premortem','reality','compare','devil']);
const roles=new Set(['OWNER','ADMIN','MEMBER','SUPPORT']);
const hash=path=>createHash('sha256').update(readFileSync(path)).digest('hex');
const text=(value,max)=>typeof value==='string'&&value.length<=max;
const hasJournal=path=>(statSync(path+'-wal',{throwIfNoEntry:false})?.size??0)>0||existsSync(path+'-journal');
const time=value=>typeof value==='string'&&/(?:Z|[+-]\d{2}:\d{2})$/.test(value)&&Number.isFinite(Date.parse(value));

export function planLegacyCutover(path, manifest){
 if(hasJournal(path))throw new Error('Use a closed SQLite backup without a WAL or journal');
 const {workspace,accounts}=manifest;
 if(!uuid.test(workspace?.id??'')||!text(workspace.name,120)||!workspace.name.trim()||!Array.isArray(accounts))
  throw new Error('Invalid workspace mapping');
 if(accounts.some(a=>!uuid.test(a.legacy_id??'')||!text(a.subject,200)||!a.subject.trim()||!roles.has(a.role)))
  throw new Error('Invalid explicit identity mapping');
 if(new Set(accounts.map(a=>a.legacy_id)).size!==accounts.length||new Set(accounts.map(a=>a.subject)).size!==accounts.length)
  throw new Error('Identity mappings must be one-to-one');
 const before=hash(path);
 const db=new DatabaseSync(path,{readOnly:true});
 try{
  if(db.prepare('PRAGMA integrity_check').get().integrity_check!=='ok')throw new Error('SQLite integrity check failed');
  if(db.prepare('PRAGMA foreign_key_check').get())throw new Error('SQLite foreign key check failed');
  const users=db.prepare('SELECT id,active FROM users ORDER BY id').all();
  const owners=new Map(users.map(row=>[row.id,row.active===1]));
  if(accounts.some(a=>!owners.has(a.legacy_id)))throw new Error('Mapping references an absent legacy account');
  const mapped=new Map(accounts.map(a=>[a.legacy_id,{subject:a.subject,role:a.role,active:owners.get(a.legacy_id)}]));
  const workspaceOwners=[...mapped.values()].filter(a=>a.role==='OWNER');
  if(workspaceOwners.length!==1||!workspaceOwners[0].active)
   throw new Error('Exactly one mapped active OWNER is required');
  const quarantine=[];
  for(const user of users)if(!mapped.has(user.id))quarantine.push({entity:'account',id:user.id,reason:'unmapped_subject'});
  const decisions=[];
  const legacyDecisions=db.prepare('SELECT id,user_id,title,decision,objective,context,source_notes,mode,created_at,updated_at FROM challenges ORDER BY id').all();
  for(const d of legacyDecisions){
   const account=mapped.get(d.user_id);
   const hasValidData=uuid.test(d.id)&&modes.has(d.mode)&&text(d.title,120)&&d.title.trim()&&text(d.decision,16000)&&d.decision.trim()
    &&text(d.objective,2000)&&text(d.context,12000)&&text(d.source_notes,10000)&&time(d.created_at)&&time(d.updated_at);
   const reason=!account?'unmapped_subject':!hasValidData?'invalid_decision':null;
   if(reason){quarantine.push({entity:'decision',id:d.id,reason});continue;}
   const {user_id,...decision}=d;
   decisions.push({...decision,subject:account.subject});
  }
  const exported=new Map(decisions.map(d=>[d.id,d.subject]));
  const runs=db.prepare('SELECT id,challenge_id,user_id,status,created_at,completed_at FROM runs ORDER BY id').all();
  const archived=[];
  for(const r of runs){
   const account=mapped.get(r.user_id);
   if(!account||!exported.has(r.challenge_id)||exported.get(r.challenge_id)!==account.subject){
    quarantine.push({entity:'run',id:r.id,reason:'unmapped_run_owner_or_decision'});continue;
   }
   archived.push({id:r.id,decision_id:r.challenge_id,subject:account.subject,legacy_status:r.status,
    created_at:r.created_at,completed_at:r.completed_at});
  }
  if(hash(path)!==before||hasJournal(path))throw new Error('SQLite backup changed during planning');
  return {
   sourceSha256:before,workspace,memberships:[...mapped.values()],decisions,archivedRuns:archived,quarantine,
   counts:{source:{accounts:users.length,decisions:legacyDecisions.length,runs:runs.length},
    accepted:{accounts:mapped.size,decisions:decisions.length,archivedRuns:archived.length}}
  };
 }finally{db.close();}
}

/** Caller owns transaction boundaries and provides a dedicated, disposable database. */
export async function applyLegacyRehearsal(client,plan){
 const {id,name}=plan.workspace;
 await client.query("SELECT set_config('whitehat.workspace',$1,true)",[id]);
 await client.query('INSERT INTO workspaces(id,name) VALUES ($1,$2)',[id,name]);
 for(const a of plan.memberships){
  await client.query('INSERT INTO memberships(workspace_id,subject,role,active) VALUES($1,$2,$3,$4)',
   [id,a.subject,a.role,a.active]);
 }
 for(const d of plan.decisions){
  await client.query(
   'INSERT INTO decisions(workspace_id,id,owner_subject,title,decision,objective,context,source_notes,mode,created_at,updated_at) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)',
   [id,d.id,d.subject,d.title,d.decision,d.objective,d.context,d.source_notes,d.mode,d.created_at,d.updated_at]);
 }
}
