package ads.uninassau.brjobs.entity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer nota;

    private String comentario;

    @OneToOne
    @JoinColumn(name = "solicitacao_id")
    private SolicitacaoServico solicitacao;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

}
