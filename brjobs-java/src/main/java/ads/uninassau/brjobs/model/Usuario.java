package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipoUsuario;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    private String telefone;
    private String endereco;
    private String cpf;
    private String funcao;
    private String dataNascimento;
    private String genero;

    @Column(length = 2000)
    private String experienciaProfissional;

    private String especialidades;

    @Lob
    @Column(name = "foto_perfil")
    private byte[] fotoPerfil;

    @Lob
    @Column(name = "curriculo")
    private byte[] curriculo;
}
