package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_audit_events")
@Getter
@Setter
public class AuthAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent_hash", length = 128)
    private String userAgentHash;

    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
