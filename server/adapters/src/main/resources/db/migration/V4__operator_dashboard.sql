ALTER TABLE workspace_operator_permissions DROP CONSTRAINT workspace_operator_permissions_permission_check;
ALTER TABLE workspace_operator_permissions ADD CHECK (
  permission IN ('REVIEW_RUN_INCIDENTS','READ_RUN_METRICS','REVIEW_QUALITY_FEEDBACK')
);

CREATE TABLE workspace_quality_settings (
  workspace_id uuid PRIMARY KEY REFERENCES workspaces(id),
  feedback_enabled boolean NOT NULL
);
CREATE TABLE run_quality_feedback (
  workspace_id uuid NOT NULL,
  run_id uuid NOT NULL,
  rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
  reviewed boolean NOT NULL DEFAULT false,
  submitted_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id,run_id),
  FOREIGN KEY (workspace_id,run_id) REFERENCES runs(workspace_id,id)
);
CREATE TABLE run_confirmed_costs (
  workspace_id uuid NOT NULL,
  run_id uuid NOT NULL,
  usd_micros bigint NOT NULL CHECK (usd_micros BETWEEN 0 AND 1000000000000),
  receipt_reference varchar(120) NOT NULL,
  PRIMARY KEY (workspace_id,run_id),
  UNIQUE (workspace_id,receipt_reference),
  FOREIGN KEY (workspace_id,run_id) REFERENCES runs(workspace_id,id)
);
DO $$
DECLARE relation text;
BEGIN
  FOREACH relation IN ARRAY ARRAY['workspace_quality_settings','run_quality_feedback','run_confirmed_costs']
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', relation);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', relation);
    EXECUTE format('CREATE POLICY workspace_scope ON %I USING (
      workspace_id::text = current_setting(''whitehat.workspace'', true)
    ) WITH CHECK (
      workspace_id::text = current_setting(''whitehat.workspace'', true)
    )', relation);
  END LOOP;
END $$;
