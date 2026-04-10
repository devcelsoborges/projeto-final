-- Criação de tabelas de cache agregado para Relatório de Ganhos
-- Data: 8 April 2026
-- Descrição: Cache pré-computado de ganhos mensais com breakdown por categoria e cliente
-- Disparador: Trigger atualiza ao marcar servico como CONCLUIDO

CREATE TABLE IF NOT EXISTS relatorio_ganhos_cache (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    mes_ano DATE NOT NULL,
    total_faturado DECIMAL(12, 2) DEFAULT 0,
    num_servicos INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(prestador_id, mes_ano)
);

CREATE INDEX idx_relatorio_ganhos_prestador_mes ON relatorio_ganhos_cache(prestador_id, mes_ano);

-- Breakdown por categoria
CREATE TABLE IF NOT EXISTS relatorio_ganhos_categoria (
    id BIGSERIAL PRIMARY KEY,
    cache_id BIGINT NOT NULL REFERENCES relatorio_ganhos_cache(id) ON DELETE CASCADE,
    categoria VARCHAR(50) NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    num_servicos INT NOT NULL
);

CREATE INDEX idx_relatorio_ganhos_categoria_cache ON relatorio_ganhos_categoria(cache_id);

-- Breakdown por cliente
CREATE TABLE IF NOT EXISTS relatorio_ganhos_cliente (
    id BIGSERIAL PRIMARY KEY,
    cache_id BIGINT NOT NULL REFERENCES relatorio_ganhos_cache(id) ON DELETE CASCADE,
    cliente_id BIGINT NOT NULL REFERENCES usuarios(id),
    cliente_nome VARCHAR(255) NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    num_servicos INT NOT NULL
);

CREATE INDEX idx_relatorio_ganhos_cliente_cache ON relatorio_ganhos_cliente(cache_id);
CREATE INDEX idx_relatorio_ganhos_cliente_id ON relatorio_ganhos_cliente(cliente_id);
