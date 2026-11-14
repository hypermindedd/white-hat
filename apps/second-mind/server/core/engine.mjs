import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';

export const VERSION = '0.3.0-alpha.1';
export const MODES = [
  {id:'challenge',name:'به چالش بکش',en:'Challenge It',short:'تصمیمت رو زیر فشار بررسی کن',icon:'spark',verdicts:['PROCEED','PROCEED WITH CONDITIONS','TEST FIRST','RETHINK','STOP']},
  {id:'kill',name:'ایده‌ام رو نقد کن',en:'Kill My Idea',short:'فرضی که ایده رو می‌شکنه پیدا کن',icon:'bolt',verdicts:['BUILD','VALIDATE FIRST','PIVOT','KILL']},
  {id:'blind',name:'نقطه‌های کور',en:'Find My Blind Spots',short:'چیزی که از دیدت پنهان مونده',icon:'eye',verdicts:['PROCEED','PROCEED WITH CONDITIONS','TEST FIRST','RETHINK','STOP']},
  {id:'premortem',name:'پیش‌مرگ تصمیم',en:'Pre-Mortem',short:'پیش از شکست، مسیرش رو ببین',icon:'route',verdicts:['PROCEED','PROCEED WITH CONDITIONS','TEST FIRST','RETHINK','STOP']},
  {id:'reality',name:'بررسی واقعیت',en:'Reality Check',short:'فاصلهٔ انتظار و پیامد رو بسنج',icon:'target',verdicts:['DO IT','CHANGE IT','TEST IT',"DON'T DO IT"]},
  {id:'compare',name:'مقایسه گزینه‌ها',en:'Compare',short:'بهتر برای کدام هدف؟',icon:'compare',verdicts:['RECOMMENDATION','CONDITIONAL RECOMMENDATION']},
  {id:'devil',name:'قوی‌ترین مخالفت',en:"Devil's Advocate",short:'از موضعت در برابر نقد منصفانه دفاع کن',icon:'shield',verdicts:['YES','PARTIALLY','NO']}
];
const contracts = {
 challenge:'Prioritized overall challenge: critical assumptions, missing factor, steelman, causal failure, unknown, change condition and decisive question.',
 kill:'Test viability fairly. Identify dominant weakness, evidence, rescue conditions and a kill test: assumption -> test -> evidence -> failure threshold -> decision effect. Never invent a fatal weakness. Survival scores are not probabilities.',
 blind:'Identify missing + relevant + material factors; distinguish visible factors from hidden factors, uncertainty treated as known, dominant blind spot and verdict impact. No generic risk list.',
 premortem:'Assume execution and failure as an explicit hypothetical. Distinguish likely from catastrophic pathways. Give cause, mechanism, early signal, preventability and mitigation priority.',
 reality:'Compare intention with defensible likely outcomes; causal intent-outcome gap, direct/second-order effects, conditional best/base/worst scenarios, reversibility and uncertainty. Never predict guaranteed outcomes.',
 compare:'Compare all supplied options relative to objective/constraints; criteria, trade-offs, opportunity cost, reversibility, missing/do-nothing option when material. No forced winner or invented numerical precision.',
 devil:'Build one strongest fair opposition: evidence an opponent could use, attacked premises, difficult questions, where they are right/weak and whether the position survives. Do not manufacture evidence.'
};
export const coreInstructions = readFileSync(new URL('./system.md', import.meta.url),'utf8');
export const coreHash = createHash('sha256').update(coreInstructions).digest('hex');
export const policy = JSON.parse(readFileSync(new URL('./evaluation-policy.json', import.meta.url),'utf8'));

const string = {type:'string'};
const obj = properties => ({type:'object',properties,required:Object.keys(properties),additionalProperties:false});
const array = items => ({type:'array',items});
export function outputSchema(modeId) {
 const mode = MODES.find(m=>m.id===modeId);
 if(!mode) throw new Error('INVALID_MODE');
 return obj({
  mode:{type:'string',enum:[modeId]},decision_question:string,
  verdict:{type:'string',enum:[...mode.verdicts,'NEEDS_CONTEXT']},
  confidence:{type:'string',enum:['LOW','MODERATE','HIGH','VERY HIGH']},
  summary:string,top_challenge:string,
  findings:array(obj({kind:{type:'string',enum:['assumption','blind_spot','evidence','trade_off','consequence']},title:string,explanation:string,evidence_status:{type:'string',enum:['provided','inferred','unknown']}})),
  strongest_counterargument:string,failure_scenario:string,
  critical_unknowns:array(obj({question:string,decision_effect:string,resolve_by:string})),
  change_conditions:array(string),
  next_action:obj({action:string,purpose:string,observable_result:string,decision_effect:string}),
  clarification_question:string
 });
}
export function validateOutput(value, modeId) {
 const errors=[];
 function walk(v,s,path='result') {
  if(s.type==='object') {
   if(!v||typeof v!=='object'||Array.isArray(v)){errors.push(path+': object required');return;}
   for(const k of s.required)if(!(k in v))errors.push(path+'.'+k+': missing');
   for(const k of Object.keys(v))if(!(k in s.properties))errors.push(path+'.'+k+': unexpected');
   for(const[k,c]of Object.entries(s.properties))if(k in v)walk(v[k],c,path+'.'+k);
  } else if(s.type==='array') {
   if(!Array.isArray(v)){errors.push(path+': array required');return;}
   if(v.length>12)errors.push(path+': too many entries');
   v.forEach((x,i)=>walk(x,s.items,path+'.'+i));
  } else if(typeof v!=='string')errors.push(path+': string required');
  else if(v.length>12000)errors.push(path+': too long');
  else if(s.enum&&!s.enum.includes(v))errors.push(path+': invalid enum');
 }
 walk(value,outputSchema(modeId));
 if(errors.length)throw Object.assign(new Error('INVALID_MODEL_OUTPUT'),{details:errors});
 if(!value.decision_question.trim()||!value.summary.trim())errors.push('Missing decision or summary');
 if(value.verdict==='NEEDS_CONTEXT') {
  if(!value.clarification_question.trim())errors.push('Blocked judgment requires a decisive question');
 } else {
  if(!value.top_challenge.trim())errors.push('Missing material challenge');
  for(const key of ['action','purpose','observable_result','decision_effect'])if(!value.next_action[key].trim())errors.push('Incomplete next best action');
 }
 if(value.critical_unknowns.length&&['HIGH','VERY HIGH'].includes(value.confidence))errors.push('Unresolved critical unknowns exceed confidence ceiling');
 if(errors.length)throw Object.assign(new Error('INVALID_MODEL_OUTPUT'),{details:errors});
 return value;
}
export function instructionsFor(modeId) {
 return `${coreInstructions}\n\nACTIVE MODE: ${modeId}\n${contracts[modeId]}\nKeep fields proportional; no private chain-of-thought. A material unknown prevents High/Very High confidence. No current fact may be presented as externally verified because this runtime has no browsing tool. Source notes/URLs are user-provided, not fetched. Output the requested JSON schema only.`;
}
