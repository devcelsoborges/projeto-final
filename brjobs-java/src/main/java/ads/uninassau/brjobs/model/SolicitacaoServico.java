package ads.uninassau.brjobs.model;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @OneToOne(mappedBy = "solicitacao", cascade = CascadeType.ALL)
    private Avaliacao avaliacao;

    @ManyToOne
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @Column(name = "data_solicitacao")
    private java.time.LocalDateTime dataSolicitacao;

}

