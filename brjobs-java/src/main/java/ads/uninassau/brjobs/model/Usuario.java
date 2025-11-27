package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa um usuário no sistema.
 * Pode ser um CONTRATANTE ou um PRESTADOR de serviços.
 * Se for PRESTADOR, terá um relacionamento com a entidade Prestador.
 */
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
    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String telefone;

    @Column(unique = true, nullable = false)
    private String cpf;

    @Column(nullable = false)
    private String genero;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoUsuario tipoUsuario;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    // Foto de perfil armazenada como bytea
    @Column(name = "foto_perfil")
    private byte[] fotoPerfil;

    // Relacionamento com Prestador (opcional, apenas se tipoUsuario == PRESTADOR)
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Prestador prestador;

    /**
     * Verifica se o usuário é um prestador de serviço
     */
    public boolean isPrestador() {
        return tipoUsuario.isPrestador();
    }

    /**
     * Verifica se o usuário é um contratante
     */
    public boolean isContratante() {
        return tipoUsuario.isContratante();
    }

    /**
     * Ativa o usuário
     */
    public void ativar() {
        this.ativo = true;
    }

    /**
     * Desativa o usuário
     */
    public void desativar() {
        this.ativo = false;
    }
}