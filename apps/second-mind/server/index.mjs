import {homedir} from 'node:os';
import {join} from 'node:path';
import {existsSync,readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {createApplication} from './app.mjs';

const [major,minor]=process.versions.node.split('.').map(Number);
if(major!==24||minor<15){console.error('SECOND MIND requires Node.js 24.15+ (24.x).');process.exit(1);}
const base=process.platform==='darwin'?join(homedir(),'Library','Application Support'):process.platform==='win32'?(process.env.APPDATA||homedir()):join(homedir(),'.local','share');
const dataDir=process.env.SECOND_MIND_DATA_DIR||join(base,'SecondMind');
const flag=name=>{const i=process.argv.indexOf(name);return i>=0?process.argv[i+1]:undefined;};
const port=Number(flag('--port')||process.env.PORT||4317),host=flag('--host')||process.env.HOST||'127.0.0.1';
const originFile=join(fileURLToPath(new URL('..',import.meta.url)),'.preview-origin');
if(!Number.isInteger(port)||port<1||port>65535){console.error('Invalid port.');process.exit(1);}
if(process.env.NODE_ENV==='production'&&!(process.env.APP_ORIGIN||'').startsWith('https://')){console.error('Production requires an explicit HTTPS APP_ORIGIN and TLS reverse proxy.');process.exit(1);}
if(!['127.0.0.1','localhost','::1'].includes(host)&&!process.env.APP_ORIGIN&&!existsSync(originFile)){console.error('APP_ORIGIN must be explicit when binding a non-loopback host.');process.exit(1);}
const app=createApplication({dataDir,publicOrigin:()=>process.env.APP_ORIGIN||(existsSync(originFile)?readFileSync(originFile,'utf8').trim():''),secureCookies:(process.env.APP_ORIGIN||'').startsWith('https://')});
app.server.listen(port,host,()=>console.log(`SECOND MIND ${process.env.NODE_ENV==='production'?'server':'local alpha'} listening on http://${host}:${port}`));
for(const signal of ['SIGINT','SIGTERM'])process.once(signal,async()=>{await app.close();process.exit(0);});
