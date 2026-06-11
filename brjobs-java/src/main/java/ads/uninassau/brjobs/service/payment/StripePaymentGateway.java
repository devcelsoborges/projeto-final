package ads.uninassau.brjobs.service.payment;

import ads.uninassau.brjobs.model.HighlightPayment;
import ads.uninassau.brjobs.model.HighlightPlan;
import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StripePaymentGateway implements PaymentGateway {

    @Value("${app.highlight.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.highlight.checkout.success-url:http://localhost:4200/minhas-publicacoes?highlight=success}")
    private String successUrl;

    @Value("${app.highlight.checkout.cancel-url:http://localhost:4200/minhas-publicacoes?highlight=cancel}")
    private String cancelUrl;

    @Value("${app.highlight.default-currency:brl}")
    private String currency;

    @Override
    public PaymentCheckoutSession createCheckoutSession(Usuario usuario, PublicacaoServico publicacaoServico, HighlightPlan plan, HighlightPayment payment) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key nao configurada");
        }

        Stripe.apiKey = stripeSecretKey;

        try {
            long amountInCents = Math.round(plan.getPrice() * 100);
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl.replace("{jobPostId}", String.valueOf(publicacaoServico.getId())))
                    .setCancelUrl(cancelUrl.replace("{jobPostId}", String.valueOf(publicacaoServico.getId())))
                    .putMetadata("publicacaoId", String.valueOf(publicacaoServico.getId()))
                    .putMetadata("planId", String.valueOf(plan.getId()))
                    .putMetadata("paymentId", String.valueOf(payment.getId()))
                    .putMetadata("usuarioId", String.valueOf(usuario.getId()))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency.toLowerCase())
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Destaque de publicacao - " + plan.getName())
                                                                    .setDescription("Publicacao " + publicacaoServico.getTitulo())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return PaymentCheckoutSession.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException ex) {
            log.error("Stripe checkout error for publicacao {}", publicacaoServico.getId(), ex);
            throw new IllegalStateException("Nao foi possivel iniciar o pagamento com a Stripe");
        }
    }
}
