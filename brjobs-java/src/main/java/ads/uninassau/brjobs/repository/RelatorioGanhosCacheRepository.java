package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.RelatorioGanhosCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public interface RelatorioGanhosCacheRepository extends JpaRepository<RelatorioGanhosCache, Long> {

    /**
     * Encontra cache de ganhos para um prestador em um mês específico
     */
    Optional<RelatorioGanhosCache> findByPrestadorIdAndMesAno(Long prestadorId, YearMonth mesAno);
}
