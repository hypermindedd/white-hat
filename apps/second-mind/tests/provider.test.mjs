import test from 'node:test';
import assert from 'node:assert/strict';
import {OpenAIProvider} from '../server/provider.mjs';
import {validateOutput,MODES} from '../server/core/engine.mjs';
import {validResult} from './fixtures.mjs';
const challenge={mode:'challenge',decision:'یک تصمیم آزمون',objective:'هدف آزمون',context:'',source_notes:''};

test('Responses adapter sends strict schema/store:false and accepts only completed structured output',async()=>{
 let sent;const provider=new OpenAIProvider({apiKey:'test-only',model:'model-test',transport:async(url,request)=>{sent={url,request,body:JSON.parse(request.body)};return new Response(JSON.stringify({status:'completed',id:'fixture-id',model:'model-test',usage:{input_tokens:5,output_tokens:8},output:[{type:'message',content:[{type:'output_text',text:JSON.stringify(validResult())}]}]}),{status:200});}});
 const r=await provider.analyze({challenge});assert.equal(sent.url,'https://api.openai.com/v1/responses');assert.equal(sent.body.store,false);assert.equal(sent.body.text.format.strict,true);assert.equal(sent.body.text.format.schema.additionalProperties,false);assert.equal(r.usage.output_tokens,8);assert.equal(r.result.mode,'challenge');
});
test('provider authentication errors never expose remote error body or secrets',async()=>{
 const provider=new OpenAIProvider({apiKey:'secret-test',model:'model-test',transport:async()=>new Response('private error detail',{status:401})});await assert.rejects(provider.analyze({challenge}),/^Error: PROVIDER_AUTH_FAILED$/);
});
test('missing provider config does not call transport',async()=>{
 let called=false;const provider=new OpenAIProvider({transport:async()=>{called=true;}});await assert.rejects(provider.analyze({challenge}),/NOT_CONFIGURED/);assert.equal(called,false);
});
test('refusal and incomplete output cannot be promoted to valid judgment',async()=>{
 for(const response of [{status:'incomplete',output:[]},{status:'completed',output:[{type:'message',content:[{type:'refusal',refusal:'no'}]}]}]){const provider=new OpenAIProvider({apiKey:'test',model:'model-test',transport:async()=>new Response(JSON.stringify(response),{status:200})});await assert.rejects(provider.analyze({challenge}),/MODEL_RESPONSE_INCOMPLETE|MODEL_REFUSED/);}
});
test('confidence ceilings, mode vocabulary, malformed fields and missing next actions are enforced',()=>{
 for(const mode of MODES)assert.equal(validateOutput(validResult(mode.id),mode.id).mode,mode.id);
 const high=validResult();high.confidence='HIGH';assert.throws(()=>validateOutput(high,'challenge'),/INVALID_MODEL_OUTPUT/);
 const wrong=validResult('kill');wrong.verdict='PROCEED';assert.throws(()=>validateOutput(wrong,'kill'),/INVALID_MODEL_OUTPUT/);
 const noAction=validResult();noAction.next_action.action='';assert.throws(()=>validateOutput(noAction,'challenge'),/INVALID_MODEL_OUTPUT/);
 const extra=validResult();extra.private_reasoning='hidden';assert.throws(()=>validateOutput(extra,'challenge'),/INVALID_MODEL_OUTPUT/);
});
