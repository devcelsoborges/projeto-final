ALTER TABLE social_logins
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE publicacoes_servico
    ADD COLUMN IF NOT EXISTS endereco_publicacao VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cep_publicacao VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cidade_publicacao VARCHAR(120),
    ADD COLUMN IF NOT EXISTS estado_publicacao VARCHAR(2),
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS geocode_provider VARCHAR(40),
    ADD COLUMN IF NOT EXISTS geocode_precision VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_publicacoes_geo
    ON publicacoes_servico(latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE TABLE IF NOT EXISTS geocode_cache (
    id BIGSERIAL PRIMARY KEY,
    address_hash VARCHAR(88) NOT NULL UNIQUE,
    normalized_address VARCHAR(500) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    source VARCHAR(40) NOT NULL,
    precision VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_geocode_cache_address_hash ON geocode_cache(address_hash);
