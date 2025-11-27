-- ============================================================================
-- SCRIPT DE MIGRAÇÃO DE BANCO DE DADOS
-- Alterações necessárias para suportar as melhorias de Prestador e Avaliacao
-- ============================================================================

-- ============================================================================
-- 1. ALTERAÇÕES NA TABELA PRESTADORES
-- ============================================================================

-- Adicionar coluna para armazenar nota média
ALTER TABLE prestadores ADD COLUMN nota_media DECIMAL(3,2) COMMENT 'Nota média calculada baseada em avaliações';

-- Adicionar coluna para rastrear quantidade de avaliações
ALTER TABLE prestadores ADD COLUMN quantidade_avaliacoes INT DEFAULT 0 COMMENT 'Quantidade de avaliações recebidas';

-- ============================================================================
-- 2. ALTERAÇÕES NA TABELA AVALIACOES
-- ============================================================================

-- Adicionar coluna prestador_id para relacionamento direto (ManyToOne)
ALTER TABLE avaliacoes ADD COLUMN prestador_id BIGINT NOT NULL COMMENT 'Referência ao prestador sendo avaliado';

-- Adicionar índice para melhorar performance
ALTER TABLE avaliacoes ADD INDEX idx_prestador_id (prestador_id);

-- Adicionar restrição de unicidade para solicitacao_id
-- (garante que cada solicitação tem apenas uma avaliação)
ALTER TABLE avaliacoes ADD CONSTRAINT uq_avaliacao_solicitacao UNIQUE (solicitacao_id);

-- Adicionar coluna de data de criação
ALTER TABLE avaliacoes ADD COLUMN data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Data e hora de criação da avaliação';

-- Adicionar coluna de data de atualização
ALTER TABLE avaliacoes ADD COLUMN data_atualizacao TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data e hora da última atualização';

-- ============================================================================
-- 3. ADICIONANDO CHAVES ESTRANGEIRAS
-- ============================================================================

-- Adicionar constraint para prestador_id em avaliacoes
ALTER TABLE avaliacoes
ADD CONSTRAINT fk_avaliacao_prestador
FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- ============================================================================
-- 4. ÍNDICES PARA PERFORMANCE
-- ============================================================================

-- Índice para buscar avaliações por prestador
ALTER TABLE avaliacoes ADD INDEX idx_prestador_id_avaliacao (prestador_id);

-- Índice para buscar avaliações por usuário
ALTER TABLE avaliacoes ADD INDEX idx_usuario_id (usuario_id);

-- Índice para buscar avaliações por solicitação
ALTER TABLE avaliacoes ADD INDEX idx_solicitacao_id (solicitacao_id);

-- Índice para data de criação (útil para ordenação e filtros)
ALTER TABLE avaliacoes ADD INDEX idx_data_criacao (data_criacao);

-- ============================================================================
-- 5. ATUALIZAÇÃO DE DADOS EXISTENTES (Migration Script)
-- ============================================================================

-- Preenchimento da coluna prestador_id baseado no relacionamento indireto
-- OBS: Execute isto DEPOIS de adicionar a coluna prestador_id
UPDATE avaliacoes av
INNER JOIN solicitacoes_servico ss ON av.solicitacao_id = ss.id
INNER JOIN servicos s ON ss.servico_id = s.id
INNER JOIN prestadores p ON s.prestador_id = p.id
SET av.prestador_id = p.id
WHERE av.prestador_id IS NULL;

-- Recalcular nota média para cada prestador
-- (Você pode executar isto via código Java também)
UPDATE prestadores p
SET p.nota_media = (
    SELECT AVG(av.nota)
    FROM avaliacoes av
    WHERE av.prestador_id = p.id
),
p.quantidade_avaliacoes = (
    SELECT COUNT(*)
    FROM avaliacoes av
    WHERE av.prestador_id = p.id
);

-- ============================================================================
-- 6. VERIFICAÇÃO E VALIDAÇÃO
-- ============================================================================

-- Verificar estrutura da tabela prestadores
DESCRIBE prestadores;

-- Verificar estrutura da tabela avaliacoes
DESCRIBE avaliacoes;

-- Verificar se há avaliações sem prestador_id
SELECT * FROM avaliacoes WHERE prestador_id IS NULL;

-- Verificar nota média calculada
SELECT id, nota_media, quantidade_avaliacoes FROM prestadores WHERE nota_media IS NOT NULL LIMIT 10;

-- Verificar integridade referencial
SELECT COUNT(*) FROM avaliacoes av
WHERE NOT EXISTS (SELECT 1 FROM prestadores p WHERE p.id = av.prestador_id);

-- ============================================================================
-- 7. ROLLBACK (Se necessário reverter)
-- ============================================================================

/*
-- Remover constraint
ALTER TABLE avaliacoes DROP CONSTRAINT fk_avaliacao_prestador;

-- Remover coluna prestador_id
ALTER TABLE avaliacoes DROP COLUMN prestador_id;
ALTER TABLE avaliacoes DROP COLUMN data_criacao;
ALTER TABLE avaliacoes DROP COLUMN data_atualizacao;

-- Remover colunas da tabela prestadores
ALTER TABLE prestadores DROP COLUMN nota_media;
ALTER TABLE prestadores DROP COLUMN quantidade_avaliacoes;

-- Remover índices
DROP INDEX idx_prestador_id ON avaliacoes;
DROP INDEX uq_avaliacao_solicitacao ON avaliacoes;
DROP INDEX idx_prestador_id_avaliacao ON avaliacoes;
DROP INDEX idx_usuario_id ON avaliacoes;
DROP INDEX idx_solicitacao_id ON avaliacoes;
DROP INDEX idx_data_criacao ON avaliacoes;
*/

-- ============================================================================
-- FIM DO SCRIPT
-- ============================================================================

