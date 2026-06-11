package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.TipoPublicacaoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PublicacaoServicoRepository extends JpaRepository<PublicacaoServico, Long> {

    @EntityGraph(attributePaths = {"usuario", "highlightPlan"})
    @Query("""
            SELECT p
            FROM PublicacaoServico p
            LEFT JOIN p.highlightPlan hp
            WHERE p.ativo = true
            ORDER BY
                CASE
                    WHEN p.isHighlighted = true AND (p.highlightExpiresAt IS NULL OR p.highlightExpiresAt > CURRENT_TIMESTAMP) THEN 0
                    ELSE 1
                END,
                COALESCE(hp.priority, 0) DESC,
                p.dataCriacao DESC
            """)
    List<PublicacaoServico> findAtivasOrdenadas();

    @EntityGraph(attributePaths = {"usuario", "highlightPlan"})
    @Query("""
            SELECT p
            FROM PublicacaoServico p
            LEFT JOIN p.highlightPlan hp
            WHERE p.ativo = true
              AND p.tipoPublicacao = :tipoPublicacao
            ORDER BY
                CASE
                    WHEN p.isHighlighted = true AND (p.highlightExpiresAt IS NULL OR p.highlightExpiresAt > CURRENT_TIMESTAMP) THEN 0
                    ELSE 1
                END,
                COALESCE(hp.priority, 0) DESC,
                p.dataCriacao DESC
            """)
    List<PublicacaoServico> findAtivasOrdenadasPorTipo(@Param("tipoPublicacao") TipoPublicacaoServico tipoPublicacao);

    @EntityGraph(attributePaths = {"usuario", "highlightPlan"})
    List<PublicacaoServico> findByUsuarioIdAndAtivoTrueOrderByDataCriacaoDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "highlightPlan"})
    @Query(
            value = """
                    SELECT p
                    FROM PublicacaoServico p
                    LEFT JOIN p.highlightPlan hp
                    WHERE p.ativo = true
                        AND (:tipo IS NULL OR p.tipoPublicacao = :tipo)
                    ORDER BY
                        CASE
                            WHEN p.isHighlighted = true AND (p.highlightExpiresAt IS NULL OR p.highlightExpiresAt > CURRENT_TIMESTAMP) THEN 0
                            ELSE 1
                        END,
                        COALESCE(hp.priority, 0) DESC,
                        p.dataCriacao DESC
                    """,
            countQuery = """
                    SELECT COUNT(p)
                    FROM PublicacaoServico p
                    WHERE p.ativo = true
                        AND (:tipo IS NULL OR p.tipoPublicacao = :tipo)
                    """
    )
    Page<PublicacaoServico> buscarAtivasPaginado(@Param("tipo") TipoPublicacaoServico tipo, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            UPDATE PublicacaoServico p
            SET p.isHighlighted = false,
                p.highlightExpiresAt = NULL,
                p.highlightPlan = NULL
            WHERE p.isHighlighted = true
              AND p.highlightExpiresAt IS NOT NULL
              AND p.highlightExpiresAt <= :referenceTime
            """)
    int clearExpiredHighlights(@Param("referenceTime") LocalDateTime referenceTime);
}
