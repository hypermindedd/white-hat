import { spawnSync } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import pg from 'pg';

const host = required('PGHOST');
const port = Number(required('PGPORT'));
const accounts = {
  admin: { user: required('PGUSER'), password: required('PGPASSWORD') },
  migration: { user: 'wh_migration', password: required('WH_MIGRATION_PASSWORD') },
  api: { user: 'wh_api', password: required('WH_DATABASE_PASSWORD') },
  web: { user: 'wh_sessions', password: required('WH_SESSION_PASSWORD') },
  idp: { user: 'wh_keycloak', password: required('WH_KEYCLOAK_PASSWORD') },
};

function required(name) {
  const value = process.env[name];
  if (!value) throw new Error('Missing required setting: ' + name);
  return value;
}

async function withDb(database, account, run) {
  const client = new pg.Client({ host, port, database, ...account, connectionTimeoutMillis: 10000 });
  try {
    await client.connect();
    return await run(client);
  } finally {
    await client.end();
  }
}

async function ensureRole(db, { user, password }) {
  const exists = await db.query('SELECT 1 FROM pg_roles WHERE rolname=$1', [user]);
  if (exists.rowCount) return;
  const { rows } = await db.query("SELECT format('CREATE ROLE %I LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD %L', $1::text, $2::text) AS ddl", [user, password]);
  await db.query(rows[0].ddl);
}

try {
  await withDb(required('PGDATABASE'), accounts.admin, async db => {
    await db.query("SELECT pg_advisory_lock(hashtext('whitehat-railway-staging-bootstrap'))");
    for (const account of [accounts.migration, accounts.api, accounts.web, accounts.idp]) await ensureRole(db, account);
    for (const [database, owner, runtime] of [
      ['whitehat_api', accounts.migration, accounts.api],
      ['whitehat_sessions', accounts.migration, accounts.web],
      ['whitehat_keycloak', accounts.idp, accounts.idp],
    ]) {
      const exists = await db.query('SELECT 1 FROM pg_database WHERE datname=$1', [database]);
      if (!exists.rowCount) await db.query(`CREATE DATABASE "${database}" OWNER "${owner.user}"`);
      await db.query(`REVOKE CONNECT, TEMPORARY ON DATABASE "${database}" FROM PUBLIC`);
      await db.query(`GRANT CONNECT ON DATABASE "${database}" TO "${runtime.user}"`);
    }
    const migration = spawnSync('bash', [new URL('../api/bin/api', import.meta.url).pathname, '--migrate'], {
      stdio: 'ignore',
      env: { PATH: process.env.PATH, JAVA_HOME: process.env.JAVA_HOME,
        WH_DATABASE_URL: `jdbc:postgresql://${host}:${port}/whitehat_api`,
        WH_DATABASE_USER: accounts.migration.user, WH_DATABASE_PASSWORD: accounts.migration.password },
    });
    if (migration.status !== 0) throw new Error('Flyway did not finish');

    await withDb('whitehat_api', accounts.migration, async api => {
      await api.query('REVOKE ALL ON SCHEMA public FROM PUBLIC');
      await api.query('GRANT USAGE ON SCHEMA public TO wh_api');
      await api.query('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wh_api');
      await api.query('REVOKE INSERT, UPDATE, DELETE ON flyway_schema_history FROM wh_api');
      const { rows } = await api.query('SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1');
      console.log('Flyway schema version:', rows[0]?.version);
    });

    const sessionSql = await readFile(new URL('./db/session-store.sql', import.meta.url), 'utf8');
    await withDb('whitehat_sessions', accounts.migration, async web => {
      await web.query(sessionSql);
      await web.query('REVOKE ALL ON SCHEMA public FROM PUBLIC');
      await web.query('GRANT USAGE ON SCHEMA public TO wh_sessions');
      await web.query('GRANT SELECT, INSERT, UPDATE, DELETE ON wh_web_sessions, wh_web_login_flows, wh_web_rate_windows TO wh_sessions');
    });
    for (const [database, account] of [['whitehat_api', accounts.api], ['whitehat_sessions', accounts.web], ['whitehat_keycloak', accounts.idp]]) {
      await withDb(database, account, async db => {
        const { rows } = await db.query('SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname=current_user');
        if (rows[0]?.rolsuper !== false || rows[0]?.rolbypassrls !== false) throw new Error('Unsafe runtime role');
      });
    }
  });
  console.log('WHITEHAT_DATABASE_BOOTSTRAP_OK');
} catch (error) {
  console.error('WHITEHAT_DATABASE_BOOTSTRAP_FAILED', error.code ?? (error.message === 'Flyway did not finish' ? 'FLYWAY_FAILED' : 'CHECK_CONFIGURATION'));
  process.exitCode = 1;
}
