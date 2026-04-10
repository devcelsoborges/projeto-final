package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.Avaliacao;
import ads.uninassau.brjobs.model.Prestador;
import ads.uninassau.brjobs.model.SolicitacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    /**
     * Encontra todas as avaliações de um prestador específico
     */
    List<Avaliacao> findByPrestador(Prestador prestador);

    /**
     * Encontra todas as avaliações feitas por um usuário (tenant-aware)
     */
    List<Avaliacao> findByUsuarioId(Long usuarioId);

    /**
     * Verifica se existe uma avaliação para uma solicitação específica
     */
    boolean existsBySolicitacao(SolicitacaoServico solicitacao);

    /**
     * Encontra avaliação de um usuário para um prestador (unicidade check)
     */
    Optional<Avaliacao> findByUsuarioIdAndPrestadorId(Long usuarioId, Long prestadorId);

    /**
     * Verifica if usuário (tenant) já avaliou este prestador
     */
    boolean existsByUsuarioIdAndPrestadorId(Long usuarioId, Long prestadorId);

    /**
     * Calcula média de avaliações um um prestador
     */
    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.prestador.id = :prestadorId")
    Double getAvaliacaoMedia(@Param("prestadorId") Long prestadorId);

    /**
     * Conta quantas avaliações um prestador tem
     */
    @Query("SELECT COUNT(a) FROM Avaliacao a WHERE a.prestador.id = :prestadorId")
    long countByPrestador(@Param("prestadorId") Long prestadorId);
}
