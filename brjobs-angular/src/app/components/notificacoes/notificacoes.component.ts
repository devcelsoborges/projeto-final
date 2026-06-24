import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NotificationItem, NotificationService } from '../../service/notification.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-notificacoes',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <main class="notifications-page">
      <header class="page-header">
        <h1>Notificações</h1>
      </header>

      <section class="notifications-list" *ngIf="!loading && notifications.length > 0">
        <a
          class="notification-row"
          *ngFor="let notification of notifications"
          [class.unread]="notification.unread"
          [routerLink]="notification.path"
          [queryParams]="notification.queryParams"
        >
          <div>
            <strong>{{ notification.title }}</strong>
            <p>{{ notification.message }}</p>
          </div>
          <time *ngIf="notification.createdAt">{{ notification.createdAt | date:'short' }}</time>
        </a>
      </section>

      <p class="empty-state" *ngIf="!loading && notifications.length === 0">
        Você ainda não tem notificações.
      </p>

      <p class="loading-state" *ngIf="loading">Carregando notificações...</p>
    </main>
  `,
  styles: [`
    .notifications-page {
      width: min(920px, calc(100% - 2rem));
      margin: 2rem auto;
    }

    .page-header {
      margin-bottom: 1rem;
    }

    .page-header h1 {
      margin: 0;
      font-size: 1.8rem;
      color: var(--color-text);
    }

    .notifications-list {
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      overflow: hidden;
      background: var(--color-surface);
    }

    .notification-row {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem;
      color: var(--color-text);
      text-decoration: none;
      border-bottom: 1px solid var(--color-border);
    }

    .notification-row:last-child {
      border-bottom: 0;
    }

    .notification-row:hover {
      background: var(--color-surface-muted);
    }

    .notification-row.unread strong {
      color: var(--color-primary);
    }

    .notification-row p {
      margin: 0.25rem 0 0;
      color: var(--color-text-muted);
      line-height: 1.35;
    }

    .notification-row time {
      flex: 0 0 auto;
      color: var(--color-text-muted);
      font-size: 0.82rem;
      white-space: nowrap;
    }

    .empty-state,
    .loading-state {
      padding: 1rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      background: var(--color-surface);
      color: var(--color-text-muted);
    }

    @media (max-width: 980px) {
      .notifications-page {
        margin: 1.5rem auto;
      }
    }

    @media (max-width: 600px) {
      .notifications-page {
        width: calc(100% - 1.5rem);
        margin: 1rem auto;
      }

      .page-header h1 {
        font-size: 1.4rem;
        word-break: break-word;
      }

      .notification-row {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.35rem;
        padding: 0.85rem;
      }

      .notification-row time {
        font-size: 0.78rem;
      }
    }
  `]
})
export class NotificacoesComponent implements OnInit, OnDestroy {
  notifications: NotificationItem[] = [];
  loading = false;
  private destroy$ = new Subject<void>();

  constructor(
    private readonly notificationService: NotificationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.notificationService.notifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe((notifications) => {
        this.notifications = notifications;
        this.cdr.markForCheck();
      });

    this.notificationService.loading$
      .pipe(takeUntil(this.destroy$))
      .subscribe((loading) => {
        this.loading = loading;
        this.cdr.markForCheck();
      });

    this.notificationService.refreshNow();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
