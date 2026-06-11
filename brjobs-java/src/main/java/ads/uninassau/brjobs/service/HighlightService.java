package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.HighlightCheckoutRequestDTO;
import ads.uninassau.brjobs.dto.HighlightCheckoutResponseDTO;
import ads.uninassau.brjobs.dto.HighlightPlanDTO;
import ads.uninassau.brjobs.model.HighlightPayment;
import ads.uninassau.brjobs.model.HighlightPaymentStatus;
import ads.uninassau.brjobs.model.HighlightPlan;
import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.HighlightPaymentRepository;
import ads.uninassau.brjobs.repository.HighlightPlanRepository;
import ads.uninassau.brjobs.repository.PublicacaoServicoRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.service.payment.PaymentCheckoutSession;
import ads.uninassau.brjobs.service.payment.PaymentGateway;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighlightService {

    private final HighlightPlanRepository highlightPlanRepository;
    private final HighlightPaymentRepository highlightPaymentRepository;
    private final PublicacaoServicoRepository publicacaoServicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentGateway paymentGateway;
    private final PublicacaoCacheService publicacaoCacheService;

    @Value("${app.highlight.webhook.secret:}")
    private String webhookSecret;

    public List<HighlightPlanDTO> listPlans() {
        return highlightPlanRepository.findByActiveTrueOrderByPriorityAsc()
                .stream()
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional
    public HighlightCheckoutResponseDTO createCheckout(Long tenantId, Long publicacaoId, HighlightCheckoutRequestDTO request) {
        if (request == null || request.getPlanId() == null) {
            throw new IllegalArgumentException("planId e obrigatorio");
        }

        Usuario usuario = usuarioRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        PublicacaoServico publicacao = publicacaoServicoRepository.findById(publicacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Publicacao nao encontrada"));

        if (!Boolean.TRUE.equals(publicacao.getAtivo())) {
            throw new IllegalArgumentException("Publicacao nao encontrada");
        }

        if (!publicacao.getUsuario().getId().equals(tenantId)) {
            throw new SecurityException("Voce nao pode destacar esta publicacao");
        }

        if (Boolean.TRUE.equals(publicacao.getIsHighlighted())
                && publicacao.getHighlightExpiresAt() != null
                && publicacao.getHighlightExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Esta publicacao ja possui destaque ativo");
        }

        if (highlightPaymentRepository.existsByPublicacaoServicoIdAndStatusIn(
                publicacaoId,
                Set.of(HighlightPaymentStatus.PENDING, HighlightPaymentStatus.APPROVED)
        )) {
            throw new IllegalArgumentException("Ja existe um pagamento ativo ou pendente para esta publicacao");
        }

        HighlightPlan plan = highlightPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plano de destaque invalido"));

        HighlightPayment payment = HighlightPayment.builder()
                .publicacaoServico(publicacao)
                .highlightPlan(plan)
                .usuario(usuario)
                .status(HighlightPaymentStatus.PENDING)
                .amount(plan.getPrice())
                .stripeSessionId("pending-" + System.nanoTime())
                .build();

        payment = highlightPaymentRepository.save(payment);
        PaymentCheckoutSession session = paymentGateway.createCheckoutSession(usuario, publicacao, plan, payment);
        payment.setStripeSessionId(session.getSessionId());
        highlightPaymentRepository.save(payment);

        log.info("highlight_checkout_created userId={} publicacaoId={} planId={} paymentId={}",
                tenantId, publicacaoId, plan.getId(), payment.getId());

        return HighlightCheckoutResponseDTO.builder()
                .paymentId(payment.getId())
                .stripeSessionId(session.getSessionId())
                .checkoutUrl(session.getCheckoutUrl())
                .build();
    }

    @Transactional
    public void processStripeWebhook(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret nao configurado");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Assinatura do webhook ausente");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception ex) {
            log.warn("highlight_webhook_invalid_signature");
            throw new IllegalArgumentException("Assinatura do webhook invalida");
        }

        log.info("highlight_webhook_received eventId={} type={}", event.getId(), event.getType());

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalArgumentException("Payload do webhook invalido"));

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }

        HighlightPayment payment = highlightPaymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new IllegalArgumentException("Pagamento nao encontrado para a sessao"));

        if (payment.getStatus() == HighlightPaymentStatus.APPROVED) {
            log.warn("highlight_webhook_duplicate paymentId={} sessionId={}", payment.getId(), session.getId());
            return;
        }

        PublicacaoServico publicacao = payment.getPublicacaoServico();
        HighlightPlan plan = payment.getHighlightPlan();

        payment.setStripeEventId(event.getId());
        payment.setStatus(HighlightPaymentStatus.APPROVED);

        publicacao.setIsHighlighted(true);
        publicacao.setHighlightPlan(plan);
        publicacao.setHighlightExpiresAt(LocalDateTime.now().plusDays(plan.getDurationDays()));

        publicacaoServicoRepository.save(publicacao);
        highlightPaymentRepository.save(payment);
        publicacaoCacheService.evictAll();

        log.info("highlight_webhook_approved paymentId={} publicacaoId={} plan={} expiresAt={}",
                payment.getId(), publicacao.getId(), plan.getName(), publicacao.getHighlightExpiresAt());
    }

    @Transactional
    @Scheduled(cron = "${app.highlight.expiration-cron:0 */1 * * * *}")
    public void expireHighlights() {
        int expiredCount = publicacaoServicoRepository.clearExpiredHighlights(LocalDateTime.now());
        if (expiredCount > 0) {
            publicacaoCacheService.evictAll();
        }
        log.info("highlight_expiration_job_run expiredCount={}", expiredCount);
    }

    private HighlightPlanDTO toPlanDto(HighlightPlan plan) {
        HighlightPlanDTO dto = new HighlightPlanDTO();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setPrice(plan.getPrice());
        dto.setDurationDays(plan.getDurationDays());
        dto.setPriority(plan.getPriority());
        return dto;
    }
}
