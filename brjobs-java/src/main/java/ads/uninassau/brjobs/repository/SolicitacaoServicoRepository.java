package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.SolicitacaoServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SolicitacaoServicoRepository extends JpaRepository<SolicitacaoServico, Long> {

    List<SolicitacaoServico> findByUsuarioId(Long usuarioId);

    List<SolicitacaoServico> findByPrestadorId(Long prestadorId);
}