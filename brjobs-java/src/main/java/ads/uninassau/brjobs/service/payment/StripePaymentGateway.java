package ads.uninassau.brjobs.service.payment;

import ads.uninassau.brjobs.model.HighlightPayment;
import ads.uninassau.brjobs.model.HighlightPlan;
import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StripePaymentGateway implements PaymentGateway {

    @Value("${app.highlight.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.highlight.default-currency:brl}")
    private String currency;

    /**
     * Cria um PaymentIntent (checkout próprio via Payment Element no frontend).
     * Retorna o id e o client_secret; os métodos de pagamento (cartão, Pix, etc.) são
     * determinados automaticamente pela configuração do Dashboard da Stripe.
     */
    @Override
    public PaymentCheckoutSession createCheckoutSession(Usuario usuario, PublicacaoServico publicacaoServico, HighlightPlan plan, HighlightPayment payment) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key nao configurada");
        }

        Stripe.apiKey = stripeSecretKey;

        try {
            long amountInCents = Math.round(plan.getPrice() * 100);
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setDescription("Destaque de publicacao - " + plan.getName() + " (#" + publicacaoServico.getId() + ")")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("publicacaoId", String.valueOf(publicacaoServico.getId()))
                    .putMetadata("planId", String.valueOf(plan.getId()))
                    .putMetadata("paymentId", String.valueOf(payment.getId()))
                    .putMetadata("usuarioId", String.valueOf(usuario.getId()))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return PaymentCheckoutSession.builder()
                    .sessionId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .build();
        } catch (StripeException ex) {
            log.error("Stripe payment intent error for publicacao {}", publicacaoServico.getId(), ex);
            throw new IllegalStateException("Nao foi possivel iniciar o pagamento com a Stripe");
        }
    }

    @Override
    public String retrievePaymentStatus(String paymentIntentId) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key nao configurada");
        }
        Stripe.apiKey = stripeSecretKey;
        try {
            return PaymentIntent.retrieve(paymentIntentId).getStatus();
        } catch (StripeException ex) {
            log.warn("Stripe retrieve payment intent failed id={} reason={}", paymentIntentId, ex.getMessage());
            return null;
        }
    }

    /**
     * Cancela um PaymentIntent abandonado (best-effort). Ignora ids que não são de PaymentIntent
     * (ex.: placeholder "pending-...") e pagamentos que já não podem ser cancelados (ex.: succeeded).
     */
    @Override
    public void cancelPayment(String paymentIntentId) {
        if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")
                || stripeSecretKey == null || stripeSecretKey.isBlank()) {
            return;
        }
        Stripe.apiKey = stripeSecretKey;
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            String status = intent.getStatus();
            // succeeded/canceled não são canceláveis; só cancela o que ainda está em aberto.
            if (status != null && !"succeeded".equals(status) && !"canceled".equals(status)) {
                intent.cancel();
                log.info("stripe_payment_intent_canceled id={}", paymentIntentId);
            }
        } catch (StripeException ex) {
            log.warn("Stripe cancel payment intent failed id={} reason={}", paymentIntentId, ex.getMessage());
        }
    }
}
