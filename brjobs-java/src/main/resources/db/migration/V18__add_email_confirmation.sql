-- Confirmação de cadastro por e-mail (template account-activation).
-- Não bloqueia login; apenas registra a confirmação e guarda o token do link (24h).

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_confirmado BOOLEAN;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_confirmation_token VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_confirmation_expires_at TIMESTAMP;

-- Usuários já existentes são considerados confirmados (grandfather), para não
-- receberem cobrança de confirmação retroativa.
UPDATE usuarios SET email_confirmado = TRUE WHERE email_confirmado IS NULL;

-- Índice para o lookup pelo token do link de confirmação.
CREATE INDEX IF NOT EXISTS idx_usuarios_email_confirmation_token
    ON usuarios (email_confirmation_token);
