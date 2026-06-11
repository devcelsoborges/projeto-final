package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update AuthRefreshToken t set t.revokedAt = :revokedAt where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") LocalDateTime revokedAt);
}
