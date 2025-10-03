package ads.uninassau.brjobs.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String descricao;

    private String telefone;

    private String email;

    @OneToMany(mappedBy = "prestador", cascade = CascadeType.ALL)
    private List<Servico> servicos;
}
