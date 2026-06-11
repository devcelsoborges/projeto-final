-- Migration V8: Add numero, complemento, and bio columns to usuarios table
-- This migration adds missing address and profile fields to the usuarios table

-- Add numero (address number) column
ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS numero VARCHAR(20);

-- Add complemento (address complement) column
ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS complemento VARCHAR(255);

-- Add bio (user biography/description) column with max 600 characters
ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS bio VARCHAR(600);

-- Add comments for documentation
COMMENT ON COLUMN usuarios.numero IS 'Address number (e.g., 123, 45A)';
COMMENT ON COLUMN usuarios.complemento IS 'Address complement (e.g., apartment, floor, reference)';
COMMENT ON COLUMN usuarios.bio IS 'User biography or description (max 600 characters)';
