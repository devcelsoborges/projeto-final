CREATE TABLE IF NOT EXISTS publicacoes_servico (
    id BIGSERIAL PRIMARY KEY,
    tipo_publicacao VARCHAR(20) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descricao TEXT NOT NULL,
    categoria VARCHAR(50),
    preco DOUBLE PRECISION,
    orcamento_min DOUBLE PRECISION,
    orcamento_max DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    usuario_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_publicacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_tipo_publicacao CHECK (tipo_publicacao IN ('PRESTACAO', 'CONTRATACAO')),
    CONSTRAINT chk_publicacao_valores CHECK (
        (tipo_publicacao = 'PRESTACAO' AND preco IS NOT NULL AND preco > 0)
        OR
        (tipo_publicacao = 'CONTRATACAO' AND orcamento_min IS NOT NULL AND orcamento_max IS NOT NULL AND orcamento_max >= orcamento_min AND orcamento_max > 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_publicacoes_tipo_data ON publicacoes_servico(tipo_publicacao, data_criacao DESC);
CREATE INDEX IF NOT EXISTS idx_publicacoes_usuario ON publicacoes_servico(usuario_id);
