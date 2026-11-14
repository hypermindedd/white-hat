import {outputSchema,instructionsFor,validateOutput} from './core/engine.mjs';

export class OpenAIProvider {
 constructor({apiKey='',model='',timeoutMs=90000,transport=fetch}={}){this.apiKey=apiKey;this.model=model;this.timeoutMs=timeoutMs;this.transport=transport;}
 configured(){return Boolean(this.apiKey&&this.model);}
 configure({apiKey,model}){if(apiKey!==undefined)this.apiKey=apiKey;if(model!==undefined)this.model=model;}
 async analyze({challenge,history=[]}){
  if(!this.configured())throw new Error('PROVIDER_NOT_CONFIGURED');
  const input={decision:challenge.decision,objective:challenge.objective,context:challenge.context,source_notes:challenge.source_notes,conversation:history,active_mode:challenge.mode};
  const response=await this.transport('https://api.openai.com/v1/responses',{
   method:'POST',signal:AbortSignal.timeout(this.timeoutMs),
   headers:{'Authorization':`Bearer ${this.apiKey}`,'Content-Type':'application/json'},
   body:JSON.stringify({model:this.model,store:false,instructions:instructionsFor(challenge.mode),input:JSON.stringify(input),max_output_tokens:5000,text:{format:{type:'json_schema',name:'second_mind_decision',strict:true,schema:outputSchema(challenge.mode)}}})
  });
  if(!response.ok)throw new Error(response.status===429?'PROVIDER_RATE_LIMIT':response.status===401?'PROVIDER_AUTH_FAILED':'PROVIDER_UNAVAILABLE');
  const body=await response.json();
  if(body.status!=='completed')throw new Error('MODEL_RESPONSE_INCOMPLETE');
  const content=(body.output||[]).filter(x=>x.type==='message').flatMap(x=>x.content||[]);
  if(content.some(x=>x.type==='refusal'))throw new Error('MODEL_REFUSED');
  const text=content.filter(x=>x.type==='output_text').map(x=>x.text).join('');
  let result;try{result=JSON.parse(text);}catch{throw new Error('INVALID_MODEL_OUTPUT');}
  validateOutput(result,challenge.mode);
  return {result,usage:{input_tokens:body.usage?.input_tokens??null,output_tokens:body.usage?.output_tokens??null},provider_request_id:body.id??null,model:body.model||this.model};
 }
}
