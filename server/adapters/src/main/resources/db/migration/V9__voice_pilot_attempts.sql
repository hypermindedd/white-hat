CREATE TABLE voice_pilot_attempts (
    workspace_id uuid PRIMARY KEY REFERENCES workspaces(id),
    attempts integer NOT NULL CHECK (attempts BETWEEN 1 AND 3)
);
ALTER TABLE voice_pilot_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_pilot_attempts FORCE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON voice_pilot_attempts
    USING (workspace_id::text = current_setting('whitehat.workspace', true))
    WITH CHECK (workspace_id::text = current_setting('whitehat.workspace', true));
