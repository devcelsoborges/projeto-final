package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByPrestadorId(Long prestadorId);
}