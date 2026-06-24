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
@Builder
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

    @Column
    private String telefone;

    @Column(unique = true)
    private String cpf;

    @Column
    private String genero;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column
    private String endereco;

    @Column(name = "cep", length = 20)
    private String cep;

    @Column(name = "rua")
    private String rua;

    @Column(name = "bairro")
    private String bairro;

    @Column(name = "cidade")
    private String cidade;

    @Column(name = "estado", length = 2)
    private String estado;

    @Column(name = "numero")
    private String numero;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "bio", length = 600)
    private String bio;

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

    @Column(name = "password_reset_code", length = 6)
    private String passwordResetCode;

    // Hash (SHA-256) do token de redefinição enviado no link do e-mail.
    // Guardamos só o hash: vazamento do banco não expõe tokens utilizáveis.
    @Column(name = "password_reset_token_hash", length = 64)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    // Confirmação de e-mail no cadastro (não bloqueia login; registra a confirmação).
    @Column(name = "email_confirmado")
    private Boolean emailConfirmado;

    @Column(name = "email_confirmation_token", length = 64)
    private String emailConfirmationToken;

    @Column(name = "email_confirmation_expires_at")
    private LocalDateTime emailConfirmationExpiresAt;

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
     * Verifica se a conta foi criada via login social (Google/Facebook/Apple)
     * e ainda não definiu uma senha local. Contas sociais recebem um
     * placeholder na coluna senha (NOT NULL), nunca um hash BCrypt.
     */
    public boolean isSomenteLoginSocial() {
        return senha == null
                || "SOCIAL_LOGIN".equals(senha)
                || senha.startsWith("OAUTH2_");
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
