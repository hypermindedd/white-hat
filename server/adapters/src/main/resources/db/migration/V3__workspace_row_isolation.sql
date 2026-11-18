-- Tenant context is set transaction-locally by PostgresStore. Run the API with
-- a non-owner, NOBYPASSRLS account; schema administration uses a separate login.
DO $$
DECLARE relation text;
BEGIN
  FOREACH relation IN ARRAY ARRAY['decisions','runs','usage_counters','provider_receipts',
                                   'audit','workspace_operator_permissions','run_incident_reviews']
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', relation);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', relation);
    EXECUTE format('CREATE POLICY workspace_scope ON %I USING (
      workspace_id::text = current_setting(''whitehat.workspace'', true)
    ) WITH CHECK (
      workspace_id::text = current_setting(''whitehat.workspace'', true)
    )', relation);
  END LOOP;

  -- The trusted coordinator claims globally queued work. This capability is
  -- enabled only within its own transactions, never from request parameters.
  FOREACH relation IN ARRAY ARRAY['runs','provider_receipts','audit']
  LOOP
    EXECUTE format('CREATE POLICY worker_scope ON %I USING (
      current_setting(''whitehat.worker'', true) = ''on''
    ) WITH CHECK (
      current_setting(''whitehat.worker'', true) = ''on''
    )', relation);
  END LOOP;
END $$;
