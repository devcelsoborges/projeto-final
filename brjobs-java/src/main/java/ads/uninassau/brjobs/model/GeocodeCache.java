package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "geocode_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodeCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_hash", nullable = false, unique = true, length = 88)
    private String addressHash;

    @Column(name = "normalized_address", nullable = false, length = 500)
    private String normalizedAddress;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(length = 40)
    private String precision;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
