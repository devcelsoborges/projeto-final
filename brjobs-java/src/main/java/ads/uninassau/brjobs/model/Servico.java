package ads.uninassau.brjobs.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String descricao;

    private Double preco;

    @ManyToOne
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @OneToMany(mappedBy = "servico", cascade = CascadeType.ALL)
    private List<SolicitacaoServico> solicitacoes;
}
