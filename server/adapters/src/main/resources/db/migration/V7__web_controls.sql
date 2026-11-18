CREATE TABLE workspace_analysis_flags (
    workspace_id uuid PRIMARY KEY REFERENCES workspaces(id),
    paused boolean NOT NULL
);
CREATE TABLE run_cost_corrections (
    workspace_id uuid NOT NULL,
    run_id uuid NOT NULL,
    correction_id uuid NOT NULL,
    previous_micros bigint NOT NULL,
    corrected_micros bigint NOT NULL,
    previous_receipt varchar(120) NOT NULL,
    corrected_receipt varchar(120) NOT NULL,
    actor_subject varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id,run_id,correction_id),
    FOREIGN KEY (workspace_id,run_id) REFERENCES run_confirmed_costs(workspace_id,run_id)
);
DO $$
DECLARE relation text;
BEGIN
  FOREACH relation IN ARRAY ARRAY['workspace_analysis_flags','run_cost_corrections']
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', relation);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', relation);
    EXECUTE format('CREATE POLICY workspace_scope ON %I USING (
      workspace_id::text = current_setting(''whitehat.workspace'', true)
    ) WITH CHECK (workspace_id::text = current_setting(''whitehat.workspace'', true))', relation);
  END LOOP;
END $$;
CREATE TRIGGER run_cost_corrections_immutable BEFORE UPDATE OR DELETE ON run_cost_corrections
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
CREATE POLICY feedback_retention_worker ON workspace_quality_settings FOR SELECT
    USING (current_setting('whitehat.worker', true) = 'on');
