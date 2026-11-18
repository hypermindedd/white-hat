CREATE TABLE workspace_pilot_entitlements (
    workspace_id uuid NOT NULL REFERENCES workspaces(id),
    revision bigint NOT NULL CHECK (revision > 0),
    monthly_attempt_limit integer NOT NULL CHECK (monthly_attempt_limit >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id, revision)
);

CREATE TRIGGER workspace_pilot_entitlements_immutable
    BEFORE UPDATE OR DELETE ON workspace_pilot_entitlements
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();

CREATE TABLE workspace_pilot_usage (
    workspace_id uuid NOT NULL REFERENCES workspaces(id),
    period_start date NOT NULL CHECK (extract(day FROM period_start) = 1),
    attempts integer NOT NULL CHECK (attempts > 0),
    PRIMARY KEY (workspace_id, period_start)
);

ALTER TABLE runs ADD COLUMN pilot_entitlement_revision bigint;
ALTER TABLE runs ADD FOREIGN KEY (workspace_id, pilot_entitlement_revision)
    REFERENCES workspace_pilot_entitlements(workspace_id, revision);
CREATE INDEX runs_pilot_usage_period ON runs(workspace_id, usage_day)
    WHERE pilot_entitlement_revision IS NOT NULL;

ALTER TABLE workspace_pilot_entitlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspace_pilot_entitlements FORCE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON workspace_pilot_entitlements
    USING (workspace_id::text = current_setting('whitehat.workspace', true))
    WITH CHECK (workspace_id::text = current_setting('whitehat.workspace', true));

ALTER TABLE workspace_pilot_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspace_pilot_usage FORCE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON workspace_pilot_usage
    USING (workspace_id::text = current_setting('whitehat.workspace', true))
    WITH CHECK (workspace_id::text = current_setting('whitehat.workspace', true));
