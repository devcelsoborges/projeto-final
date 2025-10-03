package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Gera Getters, Setters, toString, equals e hashCode
@NoArgsConstructor // Gera o construtor vazio (obrigatório pelo JPA)
@AllArgsConstructor // Gera construtor com todos os campos
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha; // Senha CRIPTOGRAFADA
}