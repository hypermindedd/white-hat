-- Apply with the database owner in the BFF session database.
-- Grant the BFF login SELECT/INSERT/UPDATE/DELETE on these three tables only.
CREATE TABLE IF NOT EXISTS wh_web_sessions (
  id char(64) PRIMARY KEY, expires_at bigint NOT NULL, payload text NOT NULL
);
CREATE INDEX IF NOT EXISTS wh_web_sessions_expiry ON wh_web_sessions(expires_at);
CREATE TABLE IF NOT EXISTS wh_web_login_flows (
  id char(64) PRIMARY KEY, expires_at bigint NOT NULL, payload text NOT NULL
);
CREATE INDEX IF NOT EXISTS wh_web_login_flows_expiry ON wh_web_login_flows(expires_at);
CREATE TABLE IF NOT EXISTS wh_web_rate_windows (
  id char(64) PRIMARY KEY, expires_at bigint NOT NULL, attempts integer NOT NULL
);
CREATE INDEX IF NOT EXISTS wh_web_rate_windows_expiry ON wh_web_rate_windows(expires_at);
