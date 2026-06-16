package ads.uninassau.brjobs.service.payment;

import ads.uninassau.brjobs.model.HighlightPayment;
import ads.uninassau.brjobs.model.HighlightPlan;
import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.Usuario;

public interface PaymentGateway {
    PaymentCheckoutSession createCheckoutSession(Usuario usuario, PublicacaoServico publicacaoServico, HighlightPlan plan, HighlightPayment payment);
}
