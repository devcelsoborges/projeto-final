package ads.uninassau.brjobs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HighlightCheckoutResponseDTO {
    private Long paymentId;
    private String stripeSessionId;
    private String checkoutUrl;
}
