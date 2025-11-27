package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.Avaliacao;
import ads.uninassau.brjobs.model.Prestador;
import ads.uninassau.brjobs.model.SolicitacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    /**
     * Encontra todas as avaliações de um prestador específico
     */
    List<Avaliacao> findByPrestador(Prestador prestador);

    /**
     * Encontra todas as avaliações feitas por um usuário
     */
    List<Avaliacao> findByUsuario(Usuario usuario);

    /**
     * Verifica se existe uma avaliação para uma solicitação específica
     */
    boolean existsBySolicitacao(SolicitacaoServico solicitacao);
}
