package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma mensagem de chat 1:1 entre dois usuários.
 * Persistida para manter histórico e enviar notificações offline.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_remetente_destinatario", columnList = "remetente_id,destinatario_id"),
    @Index(name = "idx_chat_destinatario_lido", columnList = "destinatario_id,lido"),
    @Index(name = "idx_chat_notificado", columnList = "notificado,created_at")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lido = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean notificado = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void validateUsuarios() {
        if (remetente != null && destinatario != null && remetente.getId().equals(destinatario.getId())) {
            throw new IllegalArgumentException("Remetente e destinatário não podem ser o mesmo usuário");
        }
    }
}
