package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cache pré-computado de ganhos mensais por prestador.
 * Atualizado via trigger SQL ao marcar serviço como CONCLUIDO.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "relatorio_ganhos_cache", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"prestador_id", "mes_ano"})
})
public class RelatorioGanhosCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @Column(name = "mes_ano", nullable = false)
    private java.time.YearMonth mesAno;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalFaturado = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer numServicos = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreationTimestamp
    private LocalDateTime dataAtualizacao;

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
