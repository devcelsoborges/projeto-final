package ads.uninassau.brjobs.service.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCheckoutSession {
    /** Id do PaymentIntent na Stripe (usado para casar o webhook). */
    private String sessionId;
    /** client_secret do PaymentIntent, consumido pelo Payment Element no frontend. */
    private String clientSecret;
}
