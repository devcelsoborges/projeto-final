import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NotificationItem, NotificationService } from '../../service/notification.service';

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
  `]
})
export class NotificacoesComponent implements OnInit {
  notifications: NotificationItem[] = [];
  loading = false;

  constructor(
    private readonly notificationService: NotificationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.notificationService.listAll().subscribe({
      next: (notifications) => {
        this.notifications = notifications;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.notifications = [];
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
