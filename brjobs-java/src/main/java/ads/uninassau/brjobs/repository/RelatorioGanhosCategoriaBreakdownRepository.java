package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.RelatorioGanhosCategoriaBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelatorioGanhosCategoriaBreakdownRepository extends JpaRepository<RelatorioGanhosCategoriaBreakdown, Long> {

    /**
     * Encontra todos os breakdowns por categoria para um relatório (cache_id)
     */
    List<RelatorioGanhosCategoriaBreakdown> findByCacheId(Long cacheId);

    /**
     * Deleta todos os breakdowns de categoria para um relatório (usado ao atualizar trigger)
     */
    void deleteByCacheId(Long cacheId);
}
