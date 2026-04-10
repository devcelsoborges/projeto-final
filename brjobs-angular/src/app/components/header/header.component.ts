import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AccessibilityComponent } from '../accessibility/accessibility.component';
import { CommonModule } from '@angular/common';
import { ProfileStateService } from '../../service/profile-state.service';
import { ThemeMode, ThemeService } from '../../service/theme.service';
import { UxTelemetryService } from '../../service/ux-telemetry.service';
import { AuthService } from '../../service/auth.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
  usuarioMenuAberto = false;
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private profileStateService: ProfileStateService,
    private themeService: ThemeService,
    private telemetry: UxTelemetryService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.verificarLogin();

    this.authService.isLoggedIn$
      .pipe(takeUntil(this.destroy$))
      .subscribe((logged) => {
        this.isLoggedIn = logged;
        if (!logged) {
          this.usuarioNome = null;
        }
      });

    this.authService.usuario$
      .pipe(takeUntil(this.destroy$))
      .subscribe((usuario) => {
        if (usuario?.nome) {
          this.usuarioNome = usuario.nome;
        }
      });
    
    // Observar mudanças de tema
    this.themeService.darkMode$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isDark => {
        this.isDarkTheme = isDark;
      });

    this.themeService.themeMode$
      .pipe(takeUntil(this.destroy$))
      .subscribe(mode => {
        this.themeMode = mode;
      });
    
    // Inicializar com o tema atual
    this.isDarkTheme = this.themeService.isDarkModeActive();
    this.themeMode = this.themeService.getThemeMode();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Verifica se o usuário está logado analisando o localStorage
   */
  verificarLogin(): void {
    const token = localStorage.getItem('app_token');
    this.isLoggedIn = !!token;
    
    if (this.isLoggedIn) {
      this.usuarioNome = localStorage.getItem('usuario_nome');
    }
  }

  toggleMenu(): void {
    this.menuAberto = !this.menuAberto;
  }

  fecharMenu(): void {
    this.menuAberto = false;
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
  }

  /**
   * Fecha o dropdown de usuário
   */
  fecharUsuarioMenu(): void {
    this.usuarioMenuAberto = false;
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
    this.usuarioMenuAberto = false;
    
    this.router.navigate(['/login']);
  }

  /**
   * Alterna entre modo claro e escuro
   */
  definirTema(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const nextMode = target.value as ThemeMode;
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
