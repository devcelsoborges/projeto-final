import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  usuario1Id: number;
  usuario2Id: number;
  ultimaMensagem: ChatMessage | null;
  dataAtualizacao: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = '/api/v1/chat';

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

  contarNaoLidas(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/nao-lidas`);
  }
}
