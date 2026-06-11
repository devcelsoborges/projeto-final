-- Ratings can be created directly from the public profile/publication UI.
-- Existing service-based ratings may still keep solicitacao_id.
ALTER TABLE avaliacoes
ALTER COLUMN solicitacao_id DROP NOT NULL;
