import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AccessibilityComponent } from '../accessibility/accessibility.component';
import { CommonModule } from '@angular/common';
import { ProfileStateService } from '../../service/profile-state.service';
import { ThemeMode, ThemeService } from '../../service/theme.service';
import { UxTelemetryService } from '../../service/ux-telemetry.service';
import { AuthService } from '../../service/auth.service';
import { NotificationItem, NotificationService } from '../../service/notification.service';
import { ChatUnreadService } from '../../service/chat-unread.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterModule, AccessibilityComponent, CommonModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  isDarkTheme = false;
  themeMode: ThemeMode = 'system';
  menuAberto = false;
  isLoggedIn = false;
  usuarioNome: string | null = null;
  usuarioEmail: string | null = null;
  usuarioId: number | null = null;
  avatarErro = false;
  private readonly apiBase = `${environment.apiUrl}/api`;
  usuarioMenuAberto = false;
  notificacoesAberto = false;
  carregandoNotificacoes = false;
  notificacoes: NotificationItem[] = [];
  unreadChatCount = 0;
  readonly maxChatBadge = environment.chat.headerBadgeMax;
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private profileStateService: ProfileStateService,
    private themeService: ThemeService,
    private telemetry: UxTelemetryService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private chatUnreadService: ChatUnreadService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.verificarLogin();

    this.authService.isLoggedIn$
      .pipe(takeUntil(this.destroy$))
      .subscribe((logged) => {
        this.isLoggedIn = logged;
        if (!logged) {
          this.usuarioNome = null;
          this.usuarioEmail = null;
          this.usuarioId = null;
          this.unreadChatCount = 0;
        }
        this.cdr.markForCheck();
      });

    this.authService.usuario$
      .pipe(takeUntil(this.destroy$))
      .subscribe((usuario) => {
        if (usuario?.nome) {
          this.usuarioNome = usuario.nome;
        }
        if (usuario?.email) {
          this.usuarioEmail = usuario.email;
        }
        if (usuario?.id && usuario.id !== this.usuarioId) {
          this.usuarioId = usuario.id;
          this.avatarErro = false; // novo usuário/foto: tenta carregar a foto de novo
        }
        this.cdr.markForCheck();
      });

    // Badge de não-lidas vem do ChatUnreadService (COUNT barato, polling global e
    // pausável). Injetá-lo aqui também garante que ele "viva" durante toda a sessão.
    this.chatUnreadService.unreadCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe((count) => {
        this.unreadChatCount = count;
        this.cdr.markForCheck();
      });

    this.notificationService.notifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe((notificacoes) => {
        this.notificacoes = (notificacoes ?? []).slice(0, 5);
        this.cdr.markForCheck();
      });

    this.notificationService.loading$
      .pipe(takeUntil(this.destroy$))
      .subscribe((loading) => {
        this.carregandoNotificacoes = loading;
        this.cdr.markForCheck();
      });
    
    // Observar mudanças de tema
    this.themeService.darkMode$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isDark => {
        this.isDarkTheme = isDark;
        this.cdr.markForCheck();
      });

    this.themeService.themeMode$
      .pipe(takeUntil(this.destroy$))
      .subscribe(mode => {
        this.themeMode = mode;
        this.cdr.markForCheck();
      });
    
    // Inicializar com o tema atual
    this.isDarkTheme = this.themeService.isDarkModeActive();
    this.themeMode = this.themeService.getThemeMode();
  }

  irParaChat(): void {
    this.router.navigate(['/chat']);
    this.fecharMenu();
    this.fecharUsuarioMenu();
    this.fecharNotificacoes();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get themeSelectValue(): 'light' | 'dark' {
    return this.isDarkTheme ? 'dark' : 'light';
  }

  /**
   * Verifica se o usuário está logado analisando o localStorage
   */
  verificarLogin(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    this.usuarioNome = localStorage.getItem('usuario_nome');
    this.usuarioEmail = localStorage.getItem('usuario_email');
    const idArmazenado = Number(localStorage.getItem('usuario_id') || '0');
    this.usuarioId = idArmazenado > 0 ? idArmazenado : null;
    this.avatarErro = false;
  }

  /** URL da foto do usuário; o <img> cai nas iniciais (avatarErro) se não houver foto. */
  get fotoUrl(): string {
    return this.usuarioId ? `${this.apiBase}/usuarios/${this.usuarioId}/foto` : '';
  }

  /** Iniciais para o avatar (ex.: "Celso Borges" -> "CB"). */
  get iniciais(): string {
    const nome = (this.usuarioNome || '').trim();
    if (!nome) {
      return '?';
    }
    const partes = nome.split(/\s+/).filter(Boolean);
    const primeira = partes[0]?.charAt(0) ?? '';
    const ultima = partes.length > 1 ? partes[partes.length - 1].charAt(0) : '';
    return ((primeira + ultima) || primeira || '?').toUpperCase();
  }

  toggleMenu(): void {
    this.menuAberto = !this.menuAberto;
    if (this.menuAberto) {
      // Abrir o menu fecha os outros painéis (evita sobreposição no mobile).
      this.fecharNotificacoes();
      this.fecharUsuarioMenu();
    }
    this.cdr.markForCheck();
  }

  fecharMenu(): void {
    this.menuAberto = false;
    this.cdr.markForCheck();
  }

  navegar(rota: string, label: string): void {
    this.telemetry.logEvent('cta_primary_click', {
      ctaName: label,
      route: rota,
      userState: this.isLoggedIn ? 'auth' : 'anon'
    });
    this.router.navigate([rota]);
    this.fecharMenu();
  }

  /**
   * Alterna o dropdown de usuário
   */
  toggleUsuarioMenu(): void {
    this.usuarioMenuAberto = !this.usuarioMenuAberto;
    this.cdr.markForCheck();
  }

  /**
   * Fecha o dropdown de usuário
   */
  fecharUsuarioMenu(): void {
    this.usuarioMenuAberto = false;
    this.cdr.markForCheck();
  }

  toggleNotificacoes(): void {
    this.notificacoesAberto = !this.notificacoesAberto;
    this.fecharUsuarioMenu();

    if (this.notificacoesAberto) {
      this.fecharMenu(); // fecha o menu mobile para não sobrepor o painel
      this.notificationService.refreshNow();
    }
    this.cdr.markForCheck();
  }

  fecharNotificacoes(): void {
    this.notificacoesAberto = false;
    this.cdr.markForCheck();
  }

  abrirTodasNotificacoes(): void {
    this.router.navigate(['/notificacoes']);
    this.fecharNotificacoes();
    this.fecharMenu();
  }

  navegarNotificacao(): void {
    this.fecharNotificacoes();
  }

  /**
   * Navega para a página de perfil e fecha o dropdown
   */
  irParaPerfil(): void {
    this.profileStateService.resetToView();
    this.router.navigate(['/profile']);
    this.fecharUsuarioMenu();
  }

  /**
   * Navega para perfil e ativa modo de edição
   */
  editarPerfil(): void {
    this.profileStateService.startEditing();
    this.router.navigate(['/profile']);
    this.fecharUsuarioMenu();
  }

  /**
   * Detecta cliques fora do dropdown para fechá-lo
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const usuarioDropdown = (event.target as HTMLElement)?.closest('.usuario-dropdown');
    if (!usuarioDropdown && this.usuarioMenuAberto) {
      this.fecharUsuarioMenu();
    }

    const notificationDropdown = (event.target as HTMLElement)?.closest('.chat-bell, .notification-menu');
    if (!notificationDropdown && this.notificacoesAberto) {
      this.fecharNotificacoes();
    }
  }

  /**
   * Faz logout do usuário
   */
  logout(): void {
    // Fechar todos os menus imediatamente
    this.fecharUsuarioMenu();
    this.menuAberto = false;
    
    this.authService.logout();
    
    // Atualizar estado
    this.isLoggedIn = false;
    this.usuarioNome = null;
    this.usuarioEmail = null;
    this.usuarioId = null;
    this.usuarioMenuAberto = false;
    this.notificacoesAberto = false;
    this.notificacoes = [];
    this.cdr.markForCheck();
    
    this.router.navigate(['/login']);
  }

  /**
   * Alterna entre modo claro e escuro
   */
  definirTema(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const nextMode = target.value as 'light' | 'dark';
    const previousMode = this.themeMode;
    this.themeService.setThemeMode(nextMode);
    this.telemetry.logEvent('theme_changed', {
      fromTheme: previousMode,
      toTheme: nextMode,
      userState: this.isLoggedIn ? 'auth' : 'anon'
    });
  }
}
// Header component finalizado
