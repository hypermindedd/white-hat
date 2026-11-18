CREATE TABLE g001_dispatch (
    model_id text PRIMARY KEY CHECK (model_id IN ('gpt-5.4-mini-2026-03-17', 'gpt-5.4-2026-03-05')),
    workspace_id uuid NOT NULL UNIQUE REFERENCES workspaces(id),
    reserved_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE g001_dispatch ENABLE ROW LEVEL SECURITY;
ALTER TABLE g001_dispatch FORCE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON g001_dispatch
    USING (workspace_id::text = current_setting('whitehat.workspace', true))
    WITH CHECK (workspace_id::text = current_setting('whitehat.workspace', true));
