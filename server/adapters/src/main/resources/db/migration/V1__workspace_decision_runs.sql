CREATE TABLE workspaces (
  id uuid PRIMARY KEY, name varchar(120) NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE memberships (
  workspace_id uuid NOT NULL REFERENCES workspaces(id), subject varchar(200) NOT NULL,
  role varchar(16) NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER','SUPPORT')),
  active boolean NOT NULL DEFAULT true, PRIMARY KEY (workspace_id, subject)
);
CREATE TABLE decisions (
  workspace_id uuid NOT NULL REFERENCES workspaces(id), id uuid NOT NULL,
  owner_subject varchar(200) NOT NULL, revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
  title varchar(120) NOT NULL, decision text NOT NULL, objective text NOT NULL,
  context text NOT NULL, source_notes text NOT NULL,
  mode varchar(16) NOT NULL CHECK (mode IN ('challenge','kill','blind','premortem','reality','compare','devil')),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, id),
  FOREIGN KEY (workspace_id, owner_subject) REFERENCES memberships(workspace_id, subject)
);
CREATE TABLE runs (
  workspace_id uuid NOT NULL, id uuid NOT NULL, decision_id uuid NOT NULL,
  owner_subject varchar(200) NOT NULL, input_revision bigint NOT NULL,
  idempotency_key varchar(120) NOT NULL, request_hash char(64) NOT NULL,
  snapshot_json jsonb NOT NULL,
  status varchar(32) NOT NULL CHECK (status IN ('queued','running','cancellation_requested','succeeded','failed','cancelled','reconciliation_required')),
  lease_token uuid, lease_until timestamptz, attempts integer NOT NULL DEFAULT 0,
  result_json jsonb, error_code varchar(80), engine_hash char(64) NOT NULL,
  model text, provider_request_id text, input_tokens bigint, output_tokens bigint,
  usage_day date NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')::date,
  created_at timestamptz NOT NULL DEFAULT now(), completed_at timestamptz,
  PRIMARY KEY (workspace_id, id),
  FOREIGN KEY (workspace_id, decision_id) REFERENCES decisions(workspace_id, id),
  UNIQUE (workspace_id, owner_subject, idempotency_key)
);
CREATE UNIQUE INDEX one_active_run_per_decision ON runs(workspace_id, decision_id)
  WHERE status IN ('queued','running','cancellation_requested','reconciliation_required');
CREATE INDEX run_claim_queue ON runs(created_at, id) WHERE status = 'queued';
CREATE INDEX run_active_leases ON runs(lease_until) WHERE status IN ('running','cancellation_requested');
CREATE TABLE usage_counters (
  workspace_id uuid NOT NULL, subject varchar(200) NOT NULL, usage_day date NOT NULL,
  attempts integer NOT NULL CHECK (attempts > 0), PRIMARY KEY(workspace_id, subject, usage_day),
  FOREIGN KEY(workspace_id, subject) REFERENCES memberships(workspace_id, subject)
);
CREATE TABLE provider_receipts (
  workspace_id uuid NOT NULL, run_id uuid NOT NULL, lease_token uuid NOT NULL,
  model text, provider_request_id text, input_tokens bigint, output_tokens bigint,
  received_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY (workspace_id, run_id, lease_token),
  FOREIGN KEY (workspace_id, run_id) REFERENCES runs(workspace_id, id)
);
CREATE TABLE audit (
  id uuid PRIMARY KEY, workspace_id uuid NOT NULL REFERENCES workspaces(id),
  actor_subject varchar(200) NOT NULL, action varchar(80) NOT NULL, target_id uuid NOT NULL,
  target_subject varchar(200),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE FUNCTION prevent_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'APPEND_ONLY_AUDIT'; END $$;
CREATE TRIGGER audit_append_only BEFORE UPDATE OR DELETE ON audit
  FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
