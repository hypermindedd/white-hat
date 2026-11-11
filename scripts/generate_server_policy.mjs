// Idempotent port of the pinned legacy engine policies; no network or AI call.
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {MODES,VERSION,coreHash,instructionsFor,outputSchema} from '../apps/second-mind/server/core/engine.mjs';
import {validResult} from '../apps/second-mind/tests/fixtures.mjs';
const path=fileURLToPath(new URL('../server/adapters/src/main/resources/engine/policies.json',import.meta.url));
const value=JSON.stringify({legacy_engine_version:VERSION,legacy_core_hash:coreHash,modes:Object.fromEntries(MODES.map(mode=>[mode.id,{instructions:instructionsFor(mode.id),schema:outputSchema(mode.id)}]))},null,2)+'\n';
if(process.argv.includes('--write')){mkdirSync(fileURLToPath(new URL('../server/adapters/src/main/resources/engine/',import.meta.url)),{recursive:true});writeFileSync(path,value);}
else if(readFileSync(path,'utf8')!==value)throw Error('SERVER_POLICY_DRIFT');
const fixtures=fileURLToPath(new URL('../server/api/src/test/resources/legacy-output-fixtures.json',import.meta.url));
const fixtureValue=JSON.stringify(Object.fromEntries(MODES.map(mode=>[mode.id,validResult(mode.id)])),null,2)+'\n';
if(process.argv.includes('--write')){mkdirSync(fileURLToPath(new URL('../server/api/src/test/resources/',import.meta.url)),{recursive:true});writeFileSync(fixtures,fixtureValue);}
else if(readFileSync(fixtures,'utf8')!==fixtureValue)throw Error('SERVER_FIXTURE_DRIFT');
console.log('Server policy matches pinned legacy engine; no model call.');
