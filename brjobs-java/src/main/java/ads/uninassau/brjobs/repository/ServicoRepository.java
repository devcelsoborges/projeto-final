package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    /**
     * Encontra um serviço pelo ID  validando que pertence ao usuário (tensor-aware)
     */
    Optional<Servico> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Encontra todos os serviços de um usuário (prestador)
     */
    List<Servico> findByUsuarioId(Long usuarioId);

    /**
     * Valida se um serviço pertence a um usuário específico
     */
    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Query customizada para busca paginada com filtros
     */
    @Query("SELECT s FROM Servico s WHERE " +
           "(:titulo IS NULL OR LOWER(s.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))) " +
           "AND (:descricao IS NULL OR LOWER(s.descricao) LIKE LOWER(CONCAT('%', :descricao, '%'))) " +
           "ORDER BY s.id DESC")
    List<Servico> buscarPorFiltros(
        @Param("titulo") String titulo,
        @Param("descricao") String descricao
    );
}