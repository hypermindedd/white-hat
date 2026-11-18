CREATE TABLE voice_ingress_sessions (
    workspace_id uuid NOT NULL,
    session_id uuid NOT NULL,
    actor_subject varchar(200) NOT NULL,
    lease_id uuid NOT NULL,
    cursor bigint NOT NULL DEFAULT 0 CHECK (cursor >= 0),
    expires_at timestamptz NOT NULL DEFAULT (clock_timestamp() + interval '40 seconds'),
    closed boolean NOT NULL DEFAULT false,
    PRIMARY KEY (workspace_id, session_id),
    FOREIGN KEY (workspace_id, actor_subject) REFERENCES memberships(workspace_id, subject)
);

CREATE TABLE voice_ingress_turns (
    workspace_id uuid NOT NULL,
    session_id uuid NOT NULL,
    turn_id uuid NOT NULL,
    transcript text NOT NULL CHECK (char_length(transcript) BETWEEN 1 AND 4000),
    PRIMARY KEY (workspace_id, session_id, turn_id),
    FOREIGN KEY (workspace_id, session_id) REFERENCES voice_ingress_sessions(workspace_id, session_id)
);

ALTER TABLE voice_ingress_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_ingress_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON voice_ingress_sessions
    USING (workspace_id::text = current_setting('whitehat.workspace', true))
    WITH CHECK (workspace_id::text = current_setting('whitehat.workspace', true));

ALTER TABLE voice_ingress_turns ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_ingress_turns FORCE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON voice_ingress_turns
    USING (workspace_id::text = current_setting('whitehat.workspace', true))
    WITH CHECK (workspace_id::text = current_setting('whitehat.workspace', true));
