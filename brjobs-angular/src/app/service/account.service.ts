import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/**
 * Operações de conta que não exigem sessão (ex.: confirmação de cadastro pelo link do e-mail).
 */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;

  confirmarEmail(token: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/confirmar-email`, { token });
  }

  reenviarConfirmacao(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/reenviar-confirmacao`, { email });
  }
}
