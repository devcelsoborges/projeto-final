-- Increase profile bio/description limit from 300 to 600 characters.
ALTER TABLE usuarios
ALTER COLUMN bio TYPE VARCHAR(600);

COMMENT ON COLUMN usuarios.bio IS 'User biography or description (max 600 characters)';
