import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject, interval, of } from 'rxjs';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';
import { environment } from '../environments/environment';

/**
 * Fonte GLOBAL do contador de mensagens não-lidas (badge do header).
 *
 * Faz polling BARATO em `/chat/nao-lidas` (um COUNT) num intervalo folgado
 * (`unreadPollIntervalMs`), pausando quando a aba está oculta e retomando —
 * com refresh imediato — quando volta a ficar visível.
 *
 * Liga/desliga sozinho conforme o estado de login; basta ser injetado por um
 * componente sempre presente (o header) para viver durante toda a sessão.
 * NÃO busca a lista de conversas (endpoint pesado): isso é do NotificationService,
 * só sob demanda.
 */
@Injectable({
  providedIn: 'root'
})
export class ChatUnreadService implements OnDestroy {
  private unreadCountSubject = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this.unreadCountSubject.asObservable();

  private destroy$ = new Subject<void>();
  private pollStop$ = new Subject<void>();
  private readonly pollIntervalMs = environment.chat.unreadPollIntervalMs;

  private readonly onVisibilityChange = (): void => {
    if (document.hidden) {
      this.stopPolling();
    } else if (this.authService.isLoggedIn()) {
      // Volta a ficar visível: retoma o polling já com um refresh imediato.
      this.startPolling();
    }
  };

  constructor(
    private chatService: ChatService,
    private authService: AuthService
  ) {
    this.authService.isLoggedIn$
      .pipe(takeUntil(this.destroy$))
      .subscribe((isLoggedIn) => {
        if (isLoggedIn) {
          this.startPolling();
        } else {
          this.stopPolling();
          this.unreadCountSubject.next(0);
        }
      });

    document.addEventListener('visibilitychange', this.onVisibilityChange);
  }

  get currentUnreadCount(): number {
    return this.unreadCountSubject.value;
  }

  /** Atualização imediata do contador (ex.: após enviar/ler mensagem). */
  refreshNow(): void {
    if (!this.authService.isLoggedIn()) {
      this.unreadCountSubject.next(0);
      return;
    }

    this.chatService.contarNaoLidas()
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => of(0))
      )
      .subscribe((count) => {
        this.unreadCountSubject.next(Math.max(0, Number(count || 0)));
      });
  }

  startPolling(): void {
    this.stopPolling();
    this.refreshNow();

    interval(this.pollIntervalMs)
      .pipe(
        takeUntil(this.pollStop$),
        takeUntil(this.destroy$),
        switchMap(() => {
          // Aba oculta / sem sessão: não gasta request (mantém o valor atual).
          if (document.hidden || !this.authService.isLoggedIn()) {
            return of(this.unreadCountSubject.value);
          }
          return this.chatService.contarNaoLidas().pipe(catchError(() => of(0)));
        })
      )
      .subscribe((count) => {
        this.unreadCountSubject.next(Math.max(0, Number(count || 0)));
      });
  }

  stopPolling(): void {
    this.pollStop$.next();
  }

  ngOnDestroy(): void {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
    this.pollStop$.complete();
  }
}
