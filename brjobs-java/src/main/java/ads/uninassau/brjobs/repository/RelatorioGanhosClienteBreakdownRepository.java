package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.RelatorioGanhosClienteBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelatorioGanhosClienteBreakdownRepository extends JpaRepository<RelatorioGanhosClienteBreakdown, Long> {

    /**
     * Encontra todos os breakdowns por cliente para um relatório (cache_id)
     */
    List<RelatorioGanhosClienteBreakdown> findByCacheId(Long cacheId);

    /**
     * Deleta todos os breakdowns de cliente para um relatório (usado ao atualizar trigger)
     */
    void deleteByCacheId(Long cacheId);
}
