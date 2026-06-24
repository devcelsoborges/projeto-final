import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface ContatoPayload {
  nome: string;
  email: string;
  assunto: string;
  mensagem: string;
}

/**
 * Envia mensagens do formulário público "Entre em Contato" para o backend,
 * que as encaminha por e-mail (Resend) à caixa de atendimento.
 */
@Injectable({ providedIn: 'root' })
export class ContatoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/contato`;

  enviar(payload: ContatoPayload): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(this.url, payload);
  }
}
