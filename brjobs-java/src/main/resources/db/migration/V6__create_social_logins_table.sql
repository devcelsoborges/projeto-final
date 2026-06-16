-- V6__create_social_logins_table.sql
-- Tabela para armazenar conexões OAuth2 (Google, Facebook, Apple)

CREATE TABLE social_logins (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL, -- 'google', 'facebook', 'apple'
    provider_id VARCHAR(255) NOT NULL, -- ID único do usuário no provedor
    email VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    foto_url VARCHAR(500),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance e constraints de unicidade
CREATE UNIQUE INDEX idx_social_logins_usuario_provider ON social_logins(usuario_id, provider);
CREATE UNIQUE INDEX idx_social_logins_provider_id ON social_logins(provider, provider_id);
CREATE INDEX idx_social_logins_email_provider ON social_logins(email, provider);
CREATE INDEX idx_social_logins_created_at ON social_logins(created_at);

-- Comentários
COMMENT ON TABLE social_logins IS 'Armazena conexões OAuth2 de usuários com Google, Facebook e Apple';
COMMENT ON COLUMN social_logins.provider IS 'Provedor OAuth2: google, facebook ou apple';
COMMENT ON COLUMN social_logins.provider_id IS 'ID único do usuário no provedor (sub do Google, id do Facebook, etc)';
COMMENT ON COLUMN social_logins.last_login_at IS 'Data/hora do último login via este provedor';
