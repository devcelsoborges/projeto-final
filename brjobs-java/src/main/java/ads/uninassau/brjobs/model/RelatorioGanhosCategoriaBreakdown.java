package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Breakdown de ganhos por categoria dentro de um mês.
 * Referencia RelatorioGanhosCache.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "relatorio_ganhos_categoria")
public class RelatorioGanhosCategoriaBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cache_id", nullable = false)
    private RelatorioGanhosCache cache;

    @Column(length = 50, nullable = false)
    private String categoria;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer numServicos = 0;
}
