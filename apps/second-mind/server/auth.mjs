import {randomBytes,scrypt as scryptCallback,timingSafeEqual,createHash} from 'node:crypto';
import {promisify} from 'node:util';
const scrypt=promisify(scryptCallback);
export const sha=value=>createHash('sha256').update(value).digest('hex');
export async function hashPassword(password){
 if(typeof password!=='string'||password.length<12||password.length>256)throw new Error('PASSWORD_LENGTH');
 const salt=randomBytes(16).toString('hex');
 const derived=await scrypt(password,salt,64,{N:32768,r:8,p:1,maxmem:64*1024*1024});
 return `scrypt:${salt}:${derived.toString('hex')}`;
}
export async function verifyPassword(password,stored){
 if(typeof password!=='string'||password.length>256)return false;
 const [type,salt,hex]=stored.split(':');if(type!=='scrypt'||!salt||!hex)return false;
 const actual=await scrypt(password,salt,64,{N:32768,r:8,p:1,maxmem:64*1024*1024});
 const expected=Buffer.from(hex,'hex');return actual.length===expected.length&&timingSafeEqual(actual,expected);
}
export function createSession(db,userId){
 const token=randomBytes(32).toString('base64url'),csrf=randomBytes(24).toString('base64url');
 db.prepare('INSERT INTO sessions VALUES(?,?,?,?)').run(sha(token),userId,csrf,Date.now()+12*60*60*1000);
 return {token,csrf};
}
export function sessionFrom(db,request){
 const token=(request.headers.cookie||'').split(';').map(x=>x.trim()).find(x=>x.startsWith('sm_session='))?.slice(11);
 if(!token)return null;
 return db.prepare('SELECT u.id,u.username,u.display_name,u.role,s.csrf,s.token_hash FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>? AND u.active=1').get(sha(token),Date.now())||null;
}
