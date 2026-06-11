package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.AuthAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, Long> {
}
