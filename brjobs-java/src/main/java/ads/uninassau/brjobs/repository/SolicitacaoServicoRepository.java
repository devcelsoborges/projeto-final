package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.SolicitacaoServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoServicoRepository extends JpaRepository<SolicitacaoServico, Long> {

    /**
     * Encontra uma solicitação pelo ID validando que o usuário é o contratante (tenant-aware)
     */
    Optional<SolicitacaoServico> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Encontra todas as solicitações de um contratante
     */
    List<SolicitacaoServico> findByUsuarioId(Long usuarioId);

    /**
     * Encontra todas as solicitações para os serviços de um prestador (owner)
     */
    @Query("SELECT s FROM SolicitacaoServico s WHERE s.servico.usuario.id = :prestadorId")
    List<SolicitacaoServico> findByPrestadorId(@Param("prestadorId") Long prestadorId);

    /**
     * Valida se contratante tem solicitação com prestador (para validar permissão de avaliação)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END " +
           "FROM SolicitacaoServico s " +
           "WHERE s.usuario.id = :contratanteId AND s.servico.usuario.id = :prestadorId")
    boolean existsTransaction(@Param("contratanteId") Long contratanteId, @Param("prestadorId") Long prestadorId);

    /**
     * Encontra solicitações por status
     */
    List<SolicitacaoServico> findByStatus(String status);

    /**
     * Valida se ID pertence a um usuário (tenant-aware)
     */
    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}