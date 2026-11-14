// API v1 is distinct from the old cookie API and canonical envelope 1.0.
// This generated OpenAPI document describes this implemented migration slice.
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {MODES,outputSchema} from '../apps/second-mind/server/core/engine.mjs';
const ref=name=>({$ref:'#/components/schemas/'+name});
const str=(maxLength)=>({type:'string',...(maxLength?{maxLength}:{})});
const uuid={type:'string',format:'uuid'}, revision={type:'integer',format:'int64',minimum:1};
const obj=(properties,required=Object.keys(properties))=>({type:'object',properties,required,additionalProperties:false});
const array=items=>({type:'array',items});
const nullable=schema=>({anyOf:[schema,{type:'null'}]});
const input={title:{...str(120),minLength:1},decision:{...str(16000),minLength:1},objective:str(2000),context:str(12000),source_notes:str(10000),mode:{enum:MODES.map(mode=>mode.id),type:'string',default:'challenge'}};
const resultSchemas=MODES.map(mode=>outputSchema(mode.id));
const schemas={
 Error:obj({error:str(),request_id:uuid}),
 Workspace:obj({id:uuid,name:str(120),role:{type:'string',enum:['OWNER','ADMIN','MEMBER','SUPPORT']}}),
 WorkspaceInput:obj({name:{...str(120),minLength:1}}),
 MemberInput:obj({subject:{...str(200),minLength:1},role:{type:'string',enum:['ADMIN','MEMBER','SUPPORT']},active:{type:'boolean'}}),
 DecisionInput:obj(input,['title','decision']),
 DecisionReplace:obj({...input,expected_revision:revision},['title','decision','expected_revision']),
 Decision:obj({id:uuid,workspace_id:uuid,revision,...input}),
 RunRequest:obj({expected_revision:revision}),
 LegacyOutput:{oneOf:resultSchemas,description:'Pinned flat legacy alpha.1 output. This is NOT SM-KNOW-06 canonical envelope 1.0.'},
 Run:obj({id:uuid,workspace_id:uuid,decision_id:uuid,input_revision:revision,status:{type:'string',enum:['queued','running','cancellation_requested','succeeded','failed','cancelled','reconciliation_required']},output_contract:{const:'legacy-alpha.1',type:'string'},result:nullable(ref('LegacyOutput')),error:nullable(str()),input_tokens:nullable({type:'integer',format:'int64',minimum:0}),output_tokens:nullable({type:'integer',format:'int64',minimum:0})}),
 Usage:obj({runs:{type:'integer',minimum:0},input_tokens:nullable({type:'integer',minimum:0}),output_tokens:nullable({type:'integer',minimum:0}),uncertain_runs:{type:'integer',minimum:0}}),
 Me:obj({subject:str(200),workspaces:array(ref('Workspace'))}),
 Modes:obj({items:array(obj({id:{type:'string',enum:MODES.map(x=>x.id)},verdicts:array(str())}))}),
 DecisionList:obj({items:array(ref('Decision')),limit:{const:100,type:'integer'}}),
 Health:obj({status:{const:'ok',type:'string'},version:{const:'0.4.0-alpha.1',type:'string'}}),
 Ok:obj({ok:{const:true,type:'boolean'}})
};
const content=schema=>({'application/json':{schema}});
const errors=Object.fromEntries([400,401,403,404,409,413,415,429,500].map(status=>[status,{description:'Stable redacted error; no token, SQL or provider body.',content:content(ref('Error'))}]));
const param=name=>({in:'path',name,required:true,schema:uuid});
const operation=(id,response,status=200,body=null,parameters=[])=>({operationId:id,parameters,responses:{[status]:{description:'Success',content:content(response)},...errors},...(body?{requestBody:{required:true,content:content(body)}}:{})});
const ws='/workspaces/{workspace}', dec=ws+'/decisions', run=ws+'/runs/{run}';
const w=[param('workspace')], d=[...w,param('decision')], r=[...w,param('run')];
const paths={
 '/health':{get:{...operation('health',ref('Health')),security:[],description:'Liveness only; not proof of database or AI readiness.'}},
 '/openapi.json':{get:{...operation('openapi',{type:'object',additionalProperties:true}),security:[]}},
 '/me':{get:operation('me',ref('Me'))},
 '/modes':{get:operation('modes',ref('Modes'))},
 '/workspaces':{post:operation('createWorkspace',ref('Workspace'),201,ref('WorkspaceInput'))},
 [ws+'/members']:{post:{...operation('setMember',ref('Ok'),200,ref('MemberInput'),w),description:'OWNER only. Changes apply on the next request, even for an existing token. Owner transfer is intentionally not exposed.'}},
 [dec]:{get:operation('listDecisions',ref('DecisionList'),200,null,w),post:operation('createDecision',ref('Decision'),201,ref('DecisionInput'),w)},
 [dec+'/{decision}']:{get:operation('getDecision',ref('Decision'),200,null,d),put:{...operation('replaceDecision',ref('Decision'),200,ref('DecisionReplace'),d),description:'Full replacement with optimistic revision. Active/uncertain runs prevent input mutation.'}},
 [dec+'/{decision}/runs']:{post:{...operation('enqueueRun',ref('Run'),202,ref('RunRequest'),[...d,{name:'Idempotency-Key',in:'header',required:true,schema:{type:'string',pattern:'^[A-Za-z0-9_-]{8,120}$'}}]),description:'Atomic per-member/workspace UTC-day attempt reservation. Repeated key+input revision returns the original run; changed payload conflicts. Cancellation does not promise a refund.'}},
 [run]:{get:operation('getRun',ref('Run'),200,null,r)},
 [run+'/cancel']:{post:{...operation('cancelRun',ref('Run'),200,null,r),description:'Queued => cancelled. Running => cancellation_requested; actual provider cost may remain. Late output is discarded.'}},
 [ws+'/admin/usage']:{get:{...operation('adminUsage',ref('Usage'),200,null,w),description:'OWNER/ADMIN aggregates only; no private decision output. Unknown token totals are null, never invented zero or currency.'}}
};
const api={openapi:'3.1.0',info:{title:'White Hat / Second Mind Kotlin API',version:'0.4.0-alpha.1',description:'Migration slice; legacy UI cookie auth remains separate. JWT access token must include exp, sub, API audience and token_use=access.'},servers:[{url:'/api/v1'}],security:[{BearerAccess:[]}],paths,components:{securitySchemes:{BearerAccess:{type:'http',scheme:'bearer',bearerFormat:'JWT access token'}},schemas}};
const output=JSON.stringify(api,null,2)+'\n';
const path=fileURLToPath(new URL('../contracts/openapi/openapi.json',import.meta.url));
if(process.argv.includes('--write')){mkdirSync(fileURLToPath(new URL('../contracts/openapi/',import.meta.url)),{recursive:true});writeFileSync(path,output);}
else if(readFileSync(path,'utf8')!==output)throw Error('API_CONTRACT_DRIFT');
console.log('OpenAPI migration contract verified.');
