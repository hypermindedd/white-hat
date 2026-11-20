ALTER TABLE voice_ingress_sessions ADD COLUMN confirmed_decision_id uuid;
ALTER TABLE voice_ingress_sessions ADD FOREIGN KEY (workspace_id, confirmed_decision_id)
    REFERENCES decisions (workspace_id, id);
