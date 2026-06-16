package ads.uninassau.brjobs.service.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCheckoutSession {
    private String sessionId;
    private String checkoutUrl;
}
