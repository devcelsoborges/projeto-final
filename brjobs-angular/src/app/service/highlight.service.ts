import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../environments/environment";

export interface HighlightPlan {
  id: number;
  name: string;
  price: number;
  durationDays: number;
  priority: number;
}

export interface HighlightCheckoutResponse {
  paymentId: number;
  paymentIntentId: string;
  clientSecret: string;
}

@Injectable({
  providedIn: "root"
})
export class HighlightService {
  private readonly apiBase = `${environment.apiUrl}/api`;

  constructor(private readonly http: HttpClient) {}

  listPlans(): Observable<HighlightPlan[]> {
    return this.http.get<HighlightPlan[]>(`${this.apiBase}/highlight/plans`);
  }

  /** Cria o PaymentIntent do destaque e devolve o client_secret para o Payment Element. */
  createCheckout(jobPostId: number, planId: number): Observable<HighlightCheckoutResponse> {
    return this.http.post<HighlightCheckoutResponse>(
      `${this.apiBase}/highlight/checkout/${jobPostId}`,
      { planId }
    );
  }

  /** Confirma o pagamento ao voltar do checkout (consulta o PaymentIntent e ativa o destaque). */
  confirmar(paymentId: number): Observable<{ highlighted: boolean }> {
    return this.http.post<{ highlighted: boolean }>(
      `${this.apiBase}/highlight/confirmar/${paymentId}`,
      {}
    );
  }
}
