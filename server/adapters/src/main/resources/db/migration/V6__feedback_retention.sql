ALTER TABLE workspace_quality_settings ADD COLUMN retention_days integer
  CHECK (retention_days BETWEEN 1 AND 365);
CREATE INDEX run_quality_feedback_workspace_expiry ON run_quality_feedback(workspace_id,submitted_at);
