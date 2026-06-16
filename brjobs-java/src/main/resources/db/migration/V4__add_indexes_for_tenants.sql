-- Índices adicionais para otimizar queries de isolamento por tenant
-- Data: 8 April 2026
-- Descrição: Améliore performance de buscas filtradas por prestador/contratante

-- Índice em servicos para buscas rápidas por prestador
CREATE INDEX IF NOT EXISTS idx_servicos_usuario ON servicos(usuario_id);

-- Índice composto para buscas com ordenação
CREATE INDEX IF NOT EXISTS idx_servicos_categoria_criado ON servicos(titulo, 'Categoria'::VARCHAR);

-- Índice para solicitacoes por usuário contratante
CREATE INDEX IF NOT EXISTS idx_solicitacoes_usuario ON solicitacoes_servico(usuario_id);

-- Índice composto para buscas de avaliações por rating
CREATE INDEX IF NOT EXISTS idx_avaliacoes_prestador_nota ON avaliacoes(prestador_id, nota DESC);

-- Índice para histórico de avaliacoes
CREATE INDEX IF NOT EXISTS idx_avaliacoes_usuario_criacao ON avaliacoes(usuario_id, data_criacao DESC);
