package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma avaliação de um prestador de serviço.
 *
 * Uma avaliação é criada após a conclusão de um serviço solicitado.
 * Contém:
 * - Nota (1-5)
 * - Comentário
 * - Relacionamentos com Usuario (quem avalia), Prestador (sendo avaliado) e SolicitacaoServico
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    // Relacionamento com SolicitacaoServico (obrigatório, único)
    @OneToOne
    @JoinColumn(name = "solicitacao_id", nullable = false, unique = true)
    private SolicitacaoServico solicitacao;

    // Relacionamento com Usuario (quem está avaliando - contratante)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Relacionamento com Prestador (quem está sendo avaliado)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Valida se a nota está dentro do intervalo permitido (1-5)
     */
    @PrePersist
    protected void validarNota() {
        if (this.nota == null || this.nota < 1 || this.nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        }
    }
}
