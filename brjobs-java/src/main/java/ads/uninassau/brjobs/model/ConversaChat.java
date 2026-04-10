package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma conversa 1:1 entre dois usuários.
 * Armazena referência à última mensagem para otimizar queries.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "conversas_chat", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_1_id", "usuario_2_id"})
})
public class ConversaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_1_id", nullable = false)
    private Usuario usuario1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_2_id", nullable = false)
    private Usuario usuario2;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultima_mensagem_id")
    private ChatMessage ultimaMensagem;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void validateUsuarios() {
        if (usuario1 != null && usuario2 != null) {
            if (usuario1.getId().equals(usuario2.getId())) {
                throw new IllegalArgumentException("Os dois usuários não podem ser o mesmo");
            }
            // Garantir que usuario_1_id < usuario_2_id para manter consistência
            if (usuario1.getId() > usuario2.getId()) {
                Usuario temp = usuario1;
                usuario1 = usuario2;
                usuario2 = temp;
            }
        }
    }
}
