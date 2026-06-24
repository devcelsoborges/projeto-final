package ads.uninassau.brjobs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HighlightCheckoutResponseDTO {
    private Long paymentId;
    private String paymentIntentId;
    /** client_secret do PaymentIntent — usado pelo Payment Element no frontend. */
    private String clientSecret;
}
