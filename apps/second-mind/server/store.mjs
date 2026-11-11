import {DatabaseSync} from 'node:sqlite';
import {mkdirSync,chmodSync} from 'node:fs';
import {join} from 'node:path';

export function openStore(dataDir){
 mkdirSync(dataDir,{recursive:true,mode:0o700});
 const db=new DatabaseSync(join(dataDir,'second-mind.sqlite'),{timeout:5000});
 try{chmodSync(join(dataDir,'second-mind.sqlite'),0o600);}catch{}
 db.exec(`PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;
 CREATE TABLE IF NOT EXISTS migrations(version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY,username TEXT UNIQUE NOT NULL,display_name TEXT NOT NULL,password_hash TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('admin','member')),active INTEGER NOT NULL DEFAULT 1,created_at TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,csrf TEXT NOT NULL,expires_at INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS challenges(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id),title TEXT NOT NULL,decision TEXT NOT NULL,objective TEXT NOT NULL,context TEXT NOT NULL,source_notes TEXT NOT NULL,mode TEXT NOT NULL,status TEXT NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS messages(id TEXT PRIMARY KEY,challenge_id TEXT NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,role TEXT NOT NULL CHECK(role IN ('user','assistant')),content TEXT NOT NULL,created_at TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS runs(id TEXT PRIMARY KEY,challenge_id TEXT NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,user_id TEXT NOT NULL REFERENCES users(id),idempotency_key TEXT NOT NULL,request_hash TEXT NOT NULL,status TEXT NOT NULL,result_json TEXT,error_code TEXT,model TEXT,provider_request_id TEXT,input_tokens INTEGER,output_tokens INTEGER,engine_version TEXT NOT NULL,core_hash TEXT NOT NULL,day_key TEXT NOT NULL,created_at TEXT NOT NULL,completed_at TEXT,UNIQUE(user_id,idempotency_key));
 CREATE TABLE IF NOT EXISTS feedback(challenge_id TEXT PRIMARY KEY REFERENCES challenges(id) ON DELETE CASCADE,user_id TEXT NOT NULL REFERENCES users(id),useful INTEGER NOT NULL,note TEXT NOT NULL,created_at TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS evaluations(id TEXT PRIMARY KEY,run_id TEXT UNIQUE NOT NULL REFERENCES runs(id) ON DELETE CASCADE,evaluator_id TEXT NOT NULL REFERENCES users(id),scores_json TEXT NOT NULL,evidence_json TEXT NOT NULL,weighted_score REAL NOT NULL,critical_pass INTEGER NOT NULL,created_at TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS audit(id TEXT PRIMARY KEY,actor_id TEXT,action TEXT NOT NULL,target_id TEXT,metadata_json TEXT NOT NULL,created_at TEXT NOT NULL);
 CREATE TRIGGER IF NOT EXISTS audit_no_update BEFORE UPDATE ON audit BEGIN SELECT RAISE(ABORT,'append_only_audit'); END;
 CREATE TRIGGER IF NOT EXISTS audit_no_delete BEFORE DELETE ON audit BEGIN SELECT RAISE(ABORT,'append_only_audit'); END;
 INSERT OR IGNORE INTO migrations VALUES(1,datetime('now'));
 INSERT OR IGNORE INTO settings VALUES('daily_limit','20');`);
 db.exec(`UPDATE runs SET status='failed',error_code='SERVER_RESTARTED',completed_at=datetime('now') WHERE status='running'; UPDATE challenges SET status='failed' WHERE status='running';`);
 return db;
}
