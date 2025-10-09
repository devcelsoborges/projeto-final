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

    @Column(length = 15)
    private String telefone;

    @Column(length = 1000)
    private String endereco;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(length = 100)
    private String funcao;

    @Column(name = "data_nascimento")
    private String dataNascimento;

    @Column(length = 20)
    private String genero;

    @Column(length = 2000)
    private String experienciaProfissional;

    @Column(length = 1000)
    private String especialidades;

    @Lob
    @Column(name = "foto_perfil")
    private byte[] fotoPerfil;

    @Lob
    @Column(name = "curriculo")
    private byte[] curriculo;
}
