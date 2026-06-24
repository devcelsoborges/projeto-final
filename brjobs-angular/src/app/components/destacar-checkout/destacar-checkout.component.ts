import { ChangeDetectorRef, Component, OnInit, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { loadStripe, Stripe, StripeElements, StripePaymentElement } from "@stripe/stripe-js";
import { HighlightService, HighlightPlan } from "../../service/highlight.service";
import { environment } from "../../environments/environment";

/**
 * Checkout próprio do destaque, usando o Stripe Payment Element (sem redirecionar para
 * o checkout hospedado). Cria o PaymentIntent, monta o Payment Element na própria página
 * e confirma o pagamento. Ao concluir (cartão), chama o endpoint `confirmar` para ativar
 * o destaque na hora; o webhook payment_intent.succeeded é redundância e cobre o fluxo
 * assíncrono (ex.: Pix).
 */
@Component({
  selector: "app-destacar-checkout",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./destacar-checkout.component.html",
  styleUrls: ["./destacar-checkout.component.css"]
})
export class DestacarCheckoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly highlightService = inject(HighlightService);
  private readonly cdr = inject(ChangeDetectorRef);

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;

  publicacaoId = 0;
  planId = 0;
  private paymentId = 0;

  planos: HighlightPlan[] = [];
  escolhendoPlano = false;

  carregando = true;
  pagando = false;
  pronto = false;
  sucesso = false;
  erro = "";

  ngOnInit(): void {
    this.publicacaoId = Number(this.route.snapshot.paramMap.get("publicacaoId"));
    this.planId = Number(this.route.snapshot.queryParamMap.get("plano"));

    if (!this.publicacaoId) {
      this.erro = "Destaque inválido.";
      this.carregando = false;
      return;
    }

    if (this.planId) {
      // Caminho normal (deep link com ?plano=N): vai direto para o pagamento.
      this.iniciarCheckout(this.planId);
    } else {
      // Sem plano na URL (link direto, histórico ou param perdido num reload): em vez de
      // travar com "Destaque inválido", deixa o usuário escolher o plano aqui mesmo.
      this.carregarPlanos();
    }
  }

  private carregarPlanos(): void {
    this.carregando = true;
    this.cdr.detectChanges();
    this.highlightService.listPlans().subscribe({
      next: (planos) => {
        this.planos = planos ?? [];
        this.escolhendoPlano = true;
        this.carregando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.erro = "Não foi possível carregar os planos de destaque.";
        this.carregando = false;
        this.cdr.detectChanges();
      }
    });
  }

  selecionarPlano(planId: number): void {
    this.planId = planId;
    this.escolhendoPlano = false;
    this.iniciarCheckout(planId);
  }

  private iniciarCheckout(planId: number): void {
    this.carregando = true;
    this.erro = "";
    this.cdr.detectChanges();
    this.highlightService.createCheckout(this.publicacaoId, planId).subscribe({
      next: (res) => {
        this.paymentId = res.paymentId;
        this.iniciarStripe(res.clientSecret);
      },
      error: (err) => {
        // O backend pode devolver o erro como string (texto puro) ou como objeto { message }.
        const corpo = err?.error;
        const msg = typeof corpo === "string" ? corpo : corpo?.message;
        this.erro = msg || "Não foi possível iniciar o pagamento do destaque.";
        this.carregando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private async iniciarStripe(clientSecret: string): Promise<void> {
    try {
      this.stripe = await loadStripe(environment.stripePublishableKey);
      if (!this.stripe) {
        throw new Error("Stripe.js não carregou.");
      }

      this.elements = this.stripe.elements({
        clientSecret,
        appearance: { theme: "stripe" }
      });
      this.paymentElement = this.elements.create("payment");
      this.carregando = false;
      this.pronto = true;
      this.cdr.detectChanges();
      // monta após o container existir no DOM
      this.paymentElement.mount("#payment-element");
    } catch (e: any) {
      this.erro = "Não foi possível carregar o formulário de pagamento.";
      this.carregando = false;
      this.cdr.detectChanges();
    }
  }

  async pagar(): Promise<void> {
    if (!this.stripe || !this.elements || this.pagando) {
      return;
    }
    this.pagando = true;
    this.erro = "";
    this.cdr.detectChanges();

    const { error, paymentIntent } = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: {
        return_url: `${window.location.origin}/minhas-publicacoes?highlight=success`
      },
      // 'if_required' mantém cartão na própria página; métodos com redirecionamento
      // (ex.: alguns 3DS/Pix) usam o return_url acima.
      redirect: "if_required"
    });

    if (error) {
      this.erro = error.message || "Não foi possível concluir o pagamento.";
      this.pagando = false;
      this.cdr.detectChanges();
      return;
    }

    if (paymentIntent && paymentIntent.status === "succeeded") {
      // Pago: confirma no backend para ativar o destaque na hora (sem depender do webhook).
      this.highlightService.confirmar(this.paymentId).subscribe({
        next: () => this.finalizarSucesso(),
        // Pagamento ok mesmo se a confirmação falhar: o webhook aplica o destaque depois.
        error: () => this.finalizarSucesso()
      });
    } else if (paymentIntent && paymentIntent.status === "processing") {
      // Ex.: Pix — pago de forma assíncrona; o webhook ativa o destaque ao confirmar.
      this.finalizarSucesso();
    } else {
      this.erro = "Pagamento não confirmado. Tente novamente.";
      this.pagando = false;
      this.cdr.detectChanges();
    }
  }

  private finalizarSucesso(): void {
    this.sucesso = true;
    this.pagando = false;
    this.cdr.detectChanges();
  }

  voltar(): void {
    this.router.navigate(["/publicacoes", this.publicacaoId]);
  }

  irParaMinhas(): void {
    this.router.navigate(["/minhas-publicacoes"], { queryParams: { highlight: "success" } });
  }
}
