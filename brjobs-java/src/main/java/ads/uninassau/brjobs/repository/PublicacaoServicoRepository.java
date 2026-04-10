package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.TipoPublicacaoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicacaoServicoRepository extends JpaRepository<PublicacaoServico, Long> {

    List<PublicacaoServico> findByAtivoTrueOrderByDataCriacaoDesc();

    List<PublicacaoServico> findByAtivoTrueAndTipoPublicacaoOrderByDataCriacaoDesc(TipoPublicacaoServico tipoPublicacao);

    List<PublicacaoServico> findByUsuarioIdAndAtivoTrueOrderByDataCriacaoDesc(Long usuarioId);

        @EntityGraph(attributePaths = {"usuario"})
        @Query(
                value = """
                        SELECT p
                        FROM PublicacaoServico p
                        WHERE p.ativo = true
                            AND (:tipo IS NULL OR p.tipoPublicacao = :tipo)
                        ORDER BY p.dataCriacao DESC
                        """,
                countQuery = """
                        SELECT COUNT(p)
                        FROM PublicacaoServico p
                        WHERE p.ativo = true
                            AND (:tipo IS NULL OR p.tipoPublicacao = :tipo)
                        """
        )
        Page<PublicacaoServico> buscarAtivasPaginado(@Param("tipo") TipoPublicacaoServico tipo, Pageable pageable);
}
