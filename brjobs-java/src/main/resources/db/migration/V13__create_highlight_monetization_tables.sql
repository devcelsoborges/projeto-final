CREATE TABLE IF NOT EXISTS highlight_plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    price NUMERIC(10,2) NOT NULL,
    duration_days INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE publicacoes_servico
    ADD COLUMN IF NOT EXISTS is_highlighted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS highlight_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS highlight_plan_id BIGINT;

ALTER TABLE publicacoes_servico
    ADD CONSTRAINT fk_publicacoes_highlight_plan
    FOREIGN KEY (highlight_plan_id) REFERENCES highlight_plans(id);

CREATE TABLE IF NOT EXISTS highlight_payments (
    id BIGSERIAL PRIMARY KEY,
    publicacao_id BIGINT NOT NULL REFERENCES publicacoes_servico(id),
    highlight_plan_id BIGINT NOT NULL REFERENCES highlight_plans(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    stripe_session_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_event_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_publicacoes_destaque ON publicacoes_servico(is_highlighted, highlight_expires_at, data_criacao DESC);
CREATE INDEX IF NOT EXISTS idx_highlight_payment_publicacao ON highlight_payments(publicacao_id);
CREATE INDEX IF NOT EXISTS idx_highlight_payment_status ON highlight_payments(status);

INSERT INTO highlight_plans (name, price, duration_days, priority, active)
VALUES
    ('Basico', 10.00, 5, 1, TRUE),
    ('Plus', 20.00, 11, 2, TRUE),
    ('Premium', 40.00, 25, 3, TRUE)
ON CONFLICT (name) DO UPDATE
SET price = EXCLUDED.price,
    duration_days = EXCLUDED.duration_days,
    priority = EXCLUDED.priority,
    active = EXCLUDED.active;
