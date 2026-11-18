CREATE TABLE workspace_operator_permissions (
  workspace_id uuid NOT NULL,
  subject varchar(200) NOT NULL,
  permission varchar(32) NOT NULL CHECK (permission IN ('REVIEW_RUN_INCIDENTS')),
  PRIMARY KEY (workspace_id, subject, permission),
  FOREIGN KEY (workspace_id, subject) REFERENCES memberships(workspace_id, subject)
);
CREATE TABLE run_incident_reviews (
  workspace_id uuid NOT NULL, run_id uuid NOT NULL,
  reviewer_subject varchar(200) NOT NULL, disposition varchar(32) NOT NULL
    CHECK (disposition IN ('FOLLOW_UP_REQUIRED', 'RECEIPT_REVIEWED')),
  reviewed_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, run_id),
  FOREIGN KEY (workspace_id, run_id) REFERENCES runs(workspace_id, id),
  FOREIGN KEY (workspace_id, reviewer_subject) REFERENCES memberships(workspace_id, subject)
);
CREATE INDEX runs_owner_history_idx ON runs(workspace_id, owner_subject, created_at DESC, id DESC);
