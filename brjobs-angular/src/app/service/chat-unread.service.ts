import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject, interval, of } from 'rxjs';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatUnreadService implements OnDestroy {
  private unreadCountSubject = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this.unreadCountSubject.asObservable();

  private destroy$ = new Subject<void>();
  private pollStop$ = new Subject<void>();
  private readonly pollIntervalMs = environment.chat.pollIntervalMs;

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
  }

  get currentUnreadCount(): number {
    return this.unreadCountSubject.value;
  }

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

  private startPolling(): void {
    this.stopPolling();
    this.refreshNow();

    interval(this.pollIntervalMs)
      .pipe(
        takeUntil(this.pollStop$),
        takeUntil(this.destroy$),
        switchMap(() => this.chatService.contarNaoLidas().pipe(catchError(() => of(0))))
      )
      .subscribe((count) => {
        this.unreadCountSubject.next(Math.max(0, Number(count || 0)));
      });
  }

  private stopPolling(): void {
    this.pollStop$.next();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
    this.pollStop$.complete();
  }
}
