package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.HighlightPayment;
import ads.uninassau.brjobs.model.HighlightPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HighlightPaymentRepository extends JpaRepository<HighlightPayment, Long> {
    Optional<HighlightPayment> findByStripeSessionId(String stripeSessionId);
    boolean existsByPublicacaoServicoIdAndStatusIn(Long publicacaoId, Collection<HighlightPaymentStatus> statuses);
    List<HighlightPayment> findByPublicacaoServicoIdAndStatus(Long publicacaoId, HighlightPaymentStatus status);
}
