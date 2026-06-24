-- Separa categoria (pai) de subcategoria (filha) nas publicações de serviço.
-- Antes, a coluna `categoria` guardava a subcategoria combinada.
ALTER TABLE publicacoes_servico ADD COLUMN IF NOT EXISTS subcategoria VARCHAR(50);
