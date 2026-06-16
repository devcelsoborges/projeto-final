package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Breakdown de ganhos por cliente (contratante) dentro de um mês.
 * Referencia RelatorioGanhosCache.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "relatorio_ganhos_cliente")
public class RelatorioGanhosClienteBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cache_id", nullable = false)
    private RelatorioGanhosCache cache;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(length = 255, nullable = false)
    private String clienteNome;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer numServicos = 0;
}
