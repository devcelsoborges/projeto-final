import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ChatService, Conversa } from './chat.service';

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  unread: boolean;
  path: string;
  queryParams: Record<string, string | number>;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  constructor(private readonly chatService: ChatService) {}

  listRecent(limit = 5): Observable<NotificationItem[]> {
    return this.chatService.obterConversas().pipe(
      map((conversas) => this.toNotifications(conversas).slice(0, limit))
    );
  }

  listAll(): Observable<NotificationItem[]> {
    return this.chatService.obterConversas().pipe(
      map((conversas) => this.toNotifications(conversas))
    );
  }

  private toNotifications(conversas: Conversa[]): NotificationItem[] {
    return [...(conversas ?? [])]
      .filter((conversa) => !!conversa.ultimaMensagem || conversa.naoLidas > 0)
      .sort((a, b) => this.timestamp(b) - this.timestamp(a))
      .map((conversa) => ({
        id: conversa.id || conversa.contatoId,
        title: conversa.naoLidas > 0
          ? `${conversa.contatoNome} enviou mensagem`
          : `Conversa com ${conversa.contatoNome}`,
        message: conversa.ultimaMensagem || 'Nova mensagem recebida.',
        createdAt: conversa.atualizadaEm || conversa.ultimaMensagemEm || '',
        unread: conversa.naoLidas > 0,
        path: '/chat',
        queryParams: {
          usuarioId: conversa.contatoId,
          nome: conversa.contatoNome
        }
      }));
  }

  private timestamp(conversa: Conversa): number {
    const value = conversa.atualizadaEm || conversa.ultimaMensagemEm;
    return value ? new Date(value).getTime() || 0 : 0;
  }
}
