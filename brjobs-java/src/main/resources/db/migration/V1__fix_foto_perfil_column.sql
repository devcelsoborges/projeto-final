-- Script para corrigir a coluna foto_perfil no PostgreSQL
-- Alterar coluna de varchar para bytea com conversão manual

ALTER TABLE usuarios ALTER COLUMN foto_perfil SET DATA TYPE bytea USING foto_perfil::bytea;

