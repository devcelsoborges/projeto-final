-- Unificação de contas por e-mail (case-insensitive).
--
-- A constraint UNIQUE padrão em usuarios.email é case-sensitive no Postgres, então
-- 'Joao@X.com' e 'joao@x.com' poderiam coexistir como duas contas. Toda a aplicação
-- passou a normalizar o e-mail (minúsculo, sem espaços) na escrita e a buscar de forma
-- case-insensitive; esta migração torna essa garantia também responsabilidade do banco.
--
-- 1) Normaliza e-mails já gravados (caixa/espaços) para a forma canônica.
UPDATE usuarios SET email = lower(trim(email)) WHERE email <> lower(trim(email));

-- 2) Índice único funcional: impede definitivamente duas contas com o mesmo e-mail,
--    inclusive com caixa diferente e sob concorrência.
--    OBS.: se houver duplicatas pré-existentes, resolvê-las antes (mesclar/remover)
--    pois a criação do índice falhará enquanto existirem e-mails repetidos.
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_email_lower ON usuarios (lower(email));
