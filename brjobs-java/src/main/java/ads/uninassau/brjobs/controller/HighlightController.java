package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.HighlightCheckoutRequestDTO;
import ads.uninassau.brjobs.dto.HighlightCheckoutResponseDTO;
import ads.uninassau.brjobs.dto.HighlightPlanDTO;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.HighlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HighlightController {

    private final HighlightService highlightService;

    @GetMapping("/api/highlight/plans")
    public ResponseEntity<List<HighlightPlanDTO>> listPlans() {
        return ResponseEntity.ok(highlightService.listPlans());
    }

    @PostMapping("/api/highlight/checkout/{jobPostId}")
    @ValidateTenant
    public ResponseEntity<HighlightCheckoutResponseDTO> createCheckout(
            @RequestAttribute("tenant_id") Long tenantId,
            @PathVariable("jobPostId") Long jobPostId,
            @RequestBody HighlightCheckoutRequestDTO request
    ) {
        return ResponseEntity.ok(highlightService.createCheckout(tenantId, jobPostId, request));
    }

    @PostMapping("/api/webhook/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        highlightService.processStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
