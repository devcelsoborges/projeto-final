import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface ChatMessage {
  id: number;
  remetenteId: number;
  remetenteName: string;
  destinatarioId: number;
  conteudo: string;
  lido: boolean;
  criadoEm: string;
}

export interface Conversa {
  id: number;
  contatoId: number;
  contatoNome: string;
  ultimaMensagem: string | null;
  ultimaMensagemEm: string | null;
  ultimaMensagemRemetenteId: number | null;
  naoLidas: number;
  atualizadaEm: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/api/v1/chat`;

  constructor(private http: HttpClient) {}

  enviarMensagem(destinatarioId: number, conteudo: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(
      `${this.apiUrl}/enviar?destinatarioId=${destinatarioId}`,
      { conteudo }
    );
  }

  obterConversa(outroUsuarioId: number, limit: number = 50): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(
      `${this.apiUrl}/conversa/${outroUsuarioId}?limit=${limit}`
    );
  }

  obterConversas(): Observable<Conversa[]> {
    return this.http.get<Conversa[]>(`${this.apiUrl}/conversas`);
  }

  marcarComoLida(mensagemId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/marcar-lida/${mensagemId}`, {});
  }

  marcarConversaComoLida(outroUsuarioId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/marcar-lidas/${outroUsuarioId}`, {});
  }

  contarNaoLidas(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/nao-lidas`);
  }
}
