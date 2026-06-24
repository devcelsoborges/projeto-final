package ads.uninassau.brjobs.service.payment;

import ads.uninassau.brjobs.model.HighlightPayment;
import ads.uninassau.brjobs.model.HighlightPlan;
import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.Usuario;

public interface PaymentGateway {
    PaymentCheckoutSession createCheckoutSession(Usuario usuario, PublicacaoServico publicacaoServico, HighlightPlan plan, HighlightPayment payment);

    /** Consulta o status atual do pagamento no provedor (ex.: "succeeded", "processing"). */
    String retrievePaymentStatus(String paymentIntentId);

    /**
     * Cancela um pagamento ainda não concluído no provedor (best-effort). Usado para descartar
     * checkouts abandonados. Não deve lançar se o pagamento não puder ser cancelado.
     */
    void cancelPayment(String paymentIntentId);
}
