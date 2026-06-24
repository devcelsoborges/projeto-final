package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade para armazenar conexões sociais (Google, Facebook, Apple)
 * Cada usuário pode ter múltiplas contas sociais conectadas
 */
@Entity
@Table(name = "social_logins", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "provider"}),
    @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // "google", "facebook", "apple"
    @Column(nullable = false)
    private String provider;

    // ID único do usuário no provedor (ex: sub do Google)
    @Column(nullable = false)
    private String providerId;

    // Email retornado pelo provedor
    @Column(nullable = false)
    private String email;

    // Nome do usuário (do provedor)
    @Column(nullable = false)
    private String nome;

    // columnDefinition com default permite o ddl-auto=update adicionar a coluna
    // em tabelas que ja tem linhas (sem default, o ALTER NOT NULL falha).
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private Boolean emailVerified = false;

    // Foto de perfil (URL)
    @Column(length = 500)
    private String fotoUrl;

    // Último acesso via este provedor
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Data de criação
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastLoginAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastLoginAt = LocalDateTime.now();
    }
}
