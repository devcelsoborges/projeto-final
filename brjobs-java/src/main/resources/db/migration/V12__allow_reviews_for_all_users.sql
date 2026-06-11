-- Ratings target any user, while keeping optional prestador_id for legacy data.
ALTER TABLE avaliacoes
ADD COLUMN IF NOT EXISTS usuario_avaliado_id BIGINT;

UPDATE avaliacoes a
SET usuario_avaliado_id = p.usuario_id
FROM prestadores p
WHERE a.usuario_avaliado_id IS NULL
  AND a.prestador_id = p.id;

ALTER TABLE avaliacoes
ALTER COLUMN usuario_avaliado_id SET NOT NULL;

ALTER TABLE avaliacoes
ALTER COLUMN prestador_id DROP NOT NULL;

ALTER TABLE avaliacoes
ADD CONSTRAINT fk_avaliacoes_usuario_avaliado
FOREIGN KEY (usuario_avaliado_id) REFERENCES usuarios(id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_avaliacoes_usuario_usuario_avaliado
ON avaliacoes(usuario_id, usuario_avaliado_id);

CREATE INDEX IF NOT EXISTS idx_avaliacoes_usuario_avaliado
ON avaliacoes(usuario_avaliado_id);
