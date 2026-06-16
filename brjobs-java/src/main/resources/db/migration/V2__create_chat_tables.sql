-- Criação de tabelas para Chat 1:1
-- Data: 8 April 2026
-- Descrição: Tabelas para armazenar mensagens e conversas com índices performance

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    remetente_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    destinatario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    conteudo TEXT NOT NULL,
    lido BOOLEAN DEFAULT FALSE,
    notificado BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT chat_messages_remetente_diferente_destinatario 
        CHECK (remetente_id != destinatario_id)
);

-- Índices para performance
CREATE INDEX idx_chat_remetente_destinatario ON chat_messages(remetente_id, destinatario_id);
CREATE INDEX idx_chat_destinatario_lido ON chat_messages(destinatario_id, lido);
CREATE INDEX idx_chat_notificado ON chat_messages(notificado, created_at);

-- Tabela de conversas (para otimizar lista de conversas)
CREATE TABLE IF NOT EXISTS conversas_chat (
    id BIGSERIAL PRIMARY KEY,
    usuario_1_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    usuario_2_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    ultima_mensagem_id BIGINT REFERENCES chat_messages(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(usuario_1_id, usuario_2_id),
    CONSTRAINT conversas_usuarios_diferentes 
        CHECK (usuario_1_id < usuario_2_id)
);

CREATE INDEX idx_conversas_usuario_1 ON conversas_chat(usuario_1_id, updated_at DESC);
CREATE INDEX idx_conversas_usuario_2 ON conversas_chat(usuario_2_id, updated_at DESC);
