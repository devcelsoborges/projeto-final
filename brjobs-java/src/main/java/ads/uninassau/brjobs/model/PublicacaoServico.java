package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "publicacoes_servico", indexes = {
    @Index(name = "idx_publicacoes_tipo_data", columnList = "tipo_publicacao,data_criacao DESC"),
    @Index(name = "idx_publicacoes_usuario", columnList = "usuario_id")
})
public class PublicacaoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_publicacao", nullable = false, length = 20)
    private TipoPublicacaoServico tipoPublicacao;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(length = 50)
    private String categoria;

    @Column
    private Double preco;

    @Column(name = "orcamento_min")
    private Double orcamentoMin;

    @Column(name = "orcamento_max")
    private Double orcamentoMax;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ATIVA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
