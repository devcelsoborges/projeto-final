-- Persist address details used by the profile form.
ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS cep VARCHAR(20);

ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS rua VARCHAR(255);

ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS estado VARCHAR(2);

COMMENT ON COLUMN usuarios.cep IS 'Postal code used by the profile address form';
COMMENT ON COLUMN usuarios.rua IS 'Street name used by the profile address form';
COMMENT ON COLUMN usuarios.estado IS 'Brazilian state UF used by the profile address form';

ALTER TABLE usuarios
ALTER COLUMN cep TYPE VARCHAR(20);

ALTER TABLE usuarios
ALTER COLUMN estado TYPE VARCHAR(2);
