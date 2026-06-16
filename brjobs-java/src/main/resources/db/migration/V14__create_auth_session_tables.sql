CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    replaced_by_token_id BIGINT,
    created_ip VARCHAR(64),
    user_agent_hash VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_auth_refresh_replaced_by FOREIGN KEY (replaced_by_token_id) REFERENCES auth_refresh_tokens(id)
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_usuario ON auth_refresh_tokens(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_family ON auth_refresh_tokens(family_id);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_expires_at ON auth_refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS auth_audit_events (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    event_type VARCHAR(80) NOT NULL,
    ip VARCHAR(64),
    user_agent_hash VARCHAR(128),
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_audit_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_auth_audit_usuario ON auth_audit_events(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auth_audit_event_type ON auth_audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_auth_audit_created_at ON auth_audit_events(created_at);
