package ads.uninassau.brjobs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    private String senha;

    private String telefone;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<SolicitacaoServico> solicitacoes;
}