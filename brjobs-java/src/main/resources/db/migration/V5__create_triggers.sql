-- Trigger para atualizar cache de ganhos ao concluir serviço
-- Data: 8 April 2026
-- Descrição: Dispara automaticamente ao UPDATE servicos SET status='CONCLUIDO'
-- Garante que relatorio_ganhos_cache fica sempre atualizado com dados agregados

-- ============================================================================
-- FUNÇÃO: atualizar_relatorio_ganhos()
-- ============================================================================
-- Atualiza cache de ganhos contando serviços concluídos do mês
CREATE OR REPLACE FUNCTION atualizar_relatorio_ganhos()
RETURNS TRIGGER AS $$
DECLARE
    mes_ano_atual DATE;
    cache_id_temp BIGINT;
BEGIN
    -- Só processa se status mudou para 'CONCLUIDO'
    IF NEW.status = 'CONCLUIDO' AND COALESCE(OLD.status, '') != 'CONCLUIDO' THEN
        -- Obter primeiro dia do mês atual
        mes_ano_atual := DATE_TRUNC('month', NOW())::DATE;
        
        -- ====== UPSERT: Atualizar ou inserir na tabela principal ======
        INSERT INTO relatorio_ganhos_cache (prestador_id, mes_ano, total_faturado, num_servicos, updated_at)
        SELECT 
            NEW.usuario_id,
            mes_ano_atual,
            COALESCE(SUM(preco), 0),
            COALESCE(COUNT(*), 0),
            NOW()
        FROM servicos
        WHERE usuario_id = NEW.usuario_id
            AND status = 'CONCLUIDO'
            AND DATE_TRUNC('month', data_atualizacao)::DATE = mes_ano_atual
        ON CONFLICT (prestador_id, mes_ano) DO UPDATE SET
            total_faturado = EXCLUDED.total_faturado,
            num_servicos = EXCLUDED.num_servicos,
            updated_at = NOW();
        
        -- ====== GET: ID do cache que acabamos de atualizar ======
        SELECT id INTO cache_id_temp
        FROM relatorio_ganhos_cache
        WHERE prestador_id = NEW.usuario_id AND mes_ano = mes_ano_atual
        LIMIT 1;
        
        -- ====== DELETE e REINSERT: Breakdown por categoria ======
        DELETE FROM relatorio_ganhos_categoria 
        WHERE cache_id = cache_id_temp;
        
        INSERT INTO relatorio_ganhos_categoria (cache_id, categoria, total, num_servicos)
        SELECT 
            cache_id_temp,
            titulo,  -- Usando titulo como proxy de categoria até que campo real exista
            COALESCE(SUM(preco), 0),
            COALESCE(COUNT(*), 0)
        FROM servicos
        WHERE usuario_id = NEW.usuario_id
            AND status = 'CONCLUIDO'
            AND DATE_TRUNC('month', data_atualizacao)::DATE = mes_ano_atual
        GROUP BY titulo;
        
        -- ====== DELETE e REINSERT: Breakdown por cliente ======
        DELETE FROM relatorio_ganhos_cliente 
        WHERE cache_id = cache_id_temp;
        
        INSERT INTO relatorio_ganhos_cliente (cache_id, cliente_id, cliente_nome, total, num_servicos)
        SELECT 
            cache_id_temp,
            sol.usuario_id,
            u.nome,
            COALESCE(SUM(s.preco), 0),
            COALESCE(COUNT(*), 0)
        FROM servicos s
        JOIN solicitacoes_servico sol ON s.id = sol.servico_id
        JOIN usuarios u ON u.id = sol.usuario_id
        WHERE s.usuario_id = NEW.usuario_id
            AND s.status = 'CONCLUIDO'
            AND DATE_TRUNC('month', s.data_atualizacao)::DATE = mes_ano_atual
        GROUP BY sol.usuario_id, u.nome;
        
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGER: Disparado ao UPDATE em servicos
-- ============================================================================
-- Executa após qualquer UPDATE na tabela servicos
-- A função decide se precisa processar (apenas se status = 'CONCLUIDO')
CREATE TRIGGER trigger_atualizar_relatorio
AFTER UPDATE ON servicos
FOR EACH ROW
EXECUTE FUNCTION atualizar_relatorio_ganhos();
