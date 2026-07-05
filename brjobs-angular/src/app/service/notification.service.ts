import { Injectable } from '@angular/core';
import { BehaviorSubject, map, Observable, of, Subject } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';
import { ChatService, Conversa } from './chat.service';
import { AuthService } from './auth.service';

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  unread: boolean;
  path: string;
  queryParams: Record<string, string | number>;
}

/**
 * Lista de notificações (conversas) exibida no sino do header e na página
 * /notificacoes. Busca o endpoint PESADO `/chat/conversas` apenas SOB DEMANDA
 * (ao abrir o sino ou a página) — não faz polling em segundo plano.
 *
 * O contador do badge NÃO vem daqui: fica a cargo do ChatUnreadService, que faz
 * um COUNT barato. Aqui só limpamos o estado quando o usuário desloga.
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<NotificationItem[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private destroy$ = new Subject<void>();

  readonly notifications$ = this.notificationsSubject.asObservable();
  readonly loading$ = this.loadingSubject.asObservable();
  readonly unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(
    private readonly chatService: ChatService,
    private readonly authService: AuthService
  ) {
    this.authService.isLoggedIn$
      .pipe(takeUntil(this.destroy$))
      .subscribe((isLoggedIn) => {
        if (!isLoggedIn) {
          this.notificationsSubject.next([]);
          this.unreadCountSubject.next(0);
          this.loadingSubject.next(false);
        }
      });
  }

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

  refreshNow(): void {
    if (!this.authService.isLoggedIn()) {
      this.notificationsSubject.next([]);
      this.unreadCountSubject.next(0);
      this.loadingSubject.next(false);
      return;
    }

    this.loadingSubject.next(true);
    this.listAll()
      .pipe(catchError(() => of([])))
      .subscribe((notifications) => {
        this.applyNotifications(notifications);
        this.loadingSubject.next(false);
      });
  }

  private applyNotifications(notifications: NotificationItem[]): void {
    this.notificationsSubject.next(notifications ?? []);
    this.unreadCountSubject.next((notifications ?? []).filter((item) => item.unread).length);
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
