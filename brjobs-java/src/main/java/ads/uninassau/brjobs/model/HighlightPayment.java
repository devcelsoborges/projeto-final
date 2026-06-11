package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "highlight_payments", indexes = {
        @Index(name = "idx_highlight_payment_publicacao", columnList = "publicacao_id"),
        @Index(name = "idx_highlight_payment_session", columnList = "stripe_session_id", unique = true),
        @Index(name = "idx_highlight_payment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HighlightPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_id", nullable = false)
    private PublicacaoServico publicacaoServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highlight_plan_id", nullable = false)
    private HighlightPlan highlightPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "stripe_session_id", nullable = false, unique = true, length = 255)
    private String stripeSessionId;

    @Column(name = "stripe_event_id", length = 255)
    private String stripeEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HighlightPaymentStatus status;

    @Column(nullable = false)
    private Double amount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
