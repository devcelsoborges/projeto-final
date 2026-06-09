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
  stripeSessionId: string;
  checkoutUrl: string;
}

@Injectable({
  providedIn: "root"
})
export class HighlightService {
  private readonly apiBase = environment.apiUrl.replace("/v1", "");

  constructor(private readonly http: HttpClient) {}

  listPlans(): Observable<HighlightPlan[]> {
    return this.http.get<HighlightPlan[]>(`${this.apiBase}/highlight/plans`);
  }

  createCheckout(jobPostId: number, planId: number): Observable<HighlightCheckoutResponse> {
    return this.http.post<HighlightCheckoutResponse>(
      `${this.apiBase}/highlight/checkout/${jobPostId}`,
      { planId }
    );
  }
}
