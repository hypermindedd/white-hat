CREATE TABLE workspace_support_roles (
  workspace_id uuid NOT NULL REFERENCES workspaces(id),
  id uuid NOT NULL,
  name varchar(60) NOT NULL CHECK (length(btrim(name)) > 0),
  PRIMARY KEY(workspace_id,id)
);
CREATE UNIQUE INDEX workspace_support_roles_name ON workspace_support_roles(workspace_id,lower(name));

CREATE TABLE workspace_support_role_permissions (
  workspace_id uuid NOT NULL, role_id uuid NOT NULL,
  permission varchar(32) NOT NULL
    CHECK (permission IN ('REVIEW_RUN_INCIDENTS','READ_RUN_METRICS','REVIEW_QUALITY_FEEDBACK')),
  PRIMARY KEY(workspace_id,role_id,permission),
  FOREIGN KEY (workspace_id,role_id) REFERENCES workspace_support_roles(workspace_id,id) ON DELETE CASCADE
);
CREATE TABLE workspace_support_assignments (
  workspace_id uuid NOT NULL, subject varchar(200) NOT NULL, role_id uuid NOT NULL,
  PRIMARY KEY(workspace_id,subject),
  FOREIGN KEY (workspace_id,role_id) REFERENCES workspace_support_roles(workspace_id,id) ON DELETE CASCADE,
  FOREIGN KEY (workspace_id,subject) REFERENCES memberships(workspace_id,subject)
);
CREATE INDEX workspace_support_assignment_role ON workspace_support_assignments(workspace_id,role_id);
DO $$
DECLARE relation text;
BEGIN
  FOREACH relation IN ARRAY ARRAY['workspace_support_roles','workspace_support_role_permissions','workspace_support_assignments']
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
