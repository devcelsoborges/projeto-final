import { ChangeDetectorRef, Component, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SocialAuthService, AuthResponseDTO } from '../../service/social-auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-social-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="social-login-container">
      <p class="divider-text">ou</p>
      
      <div class="social-buttons">
        <!-- Google Login -->
        <button 
          class="btn-social btn-google"
          (click)="loginGoogle()"
          [disabled]="carregando"
          title="Login com Google">
          <svg class="icon" viewBox="0 0 24 24">
            <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          <span>Google</span>
        </button>

        <!-- Facebook Login -->
        <button 
          class="btn-social btn-facebook"
          (click)="loginFacebook()"
          [disabled]="carregando"
          title="Login com Facebook">
          <svg class="icon" viewBox="0 0 24 24">
            <path fill="currentColor" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
          </svg>
          <span>Facebook</span>
        </button>
      </div>

      <!-- Mensagens de erro -->
      <div *ngIf="erro" class="alert alert-danger">
        {{ erro }}
      </div>

      <!-- Loading -->
      <div *ngIf="carregando" class="loading-spinner">
        Autenticando...
      </div>
    </div>
  `,
  styles: [`
    .social-login-container {
      width: 100%;
      margin: 20px 0;
    }

    .divider-text {
      text-align: center;
      color: #999;
      font-size: 14px;
      margin: 20px 0;
      position: relative;
    }

    .divider-text::before,
    .divider-text::after {
      content: '';
      position: absolute;
      top: 50%;
      width: 45%;
      height: 1px;
      background: #ddd;
    }

    .divider-text::before {
      left: 0;
    }

    .divider-text::after {
      right: 0;
    }

    .social-buttons {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
      gap: 12px;
      margin-bottom: 20px;
    }

    .btn-social {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 12px;
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      background: white;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.3s ease;
      min-height: 48px;
      flex-direction: column;
    }

    .btn-social:hover:not(:disabled) {
      border-color: currentColor;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .btn-social:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-social span {
      font-size: 12px;
      text-align: center;
    }

    .icon {
      width: 24px;
      height: 24px;
    }

    .btn-google {
      color: #4285f4;
    }

    .btn-google:hover:not(:disabled) {
      background: #f8f9ff;
    }

    .btn-facebook {
      color: #1877f2;
    }

    .btn-facebook:hover:not(:disabled) {
      background: #f0f2ff;
    }
    @media (max-width: 600px) {
      .social-buttons {
        grid-template-columns: repeat(3, 1fr);
      }

      .btn-social {
        padding: 10px;
      }
    }

    .alert {
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 15px;
    }

    .alert-danger {
      background: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }

    .loading-spinner {
      text-align: center;
      color: #666;
      font-size: 14px;
    }
  `]
})
export class SocialLoginComponent {
  @Input() redirecionar = true;
  @Output() loginSucesso = new EventEmitter<AuthResponseDTO>();
  @Output() loginErro = new EventEmitter<string>();

  carregando = false;
  erro = '';

  constructor(
    private socialAuthService: SocialAuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  async loginGoogle() {
    this.carregando = true;
    this.erro = '';
    this.cdr.markForCheck();

    try {
      const response = await this.socialAuthService.loginComGoogle();
      this.processarLogin(response);
    } catch (error) {
      this.erro = 'Erro ao fazer login com Google';
      this.loginErro.emit(this.erro);
      this.carregando = false;
      this.cdr.markForCheck();
    }
  }

  async loginFacebook() {
    this.carregando = true;
    this.erro = '';
    this.cdr.markForCheck();

    try {
      const response = await this.socialAuthService.loginComFacebook();
      this.processarLogin(response);
    } catch (error) {
      this.erro = 'Erro ao fazer login com Facebook';
      this.loginErro.emit(this.erro);
      this.carregando = false;
      this.cdr.markForCheck();
    }
  }
  private processarLogin(response: AuthResponseDTO) {
    if (response.error) {
      this.erro = response.error;
      this.loginErro.emit(this.erro);
      this.carregando = false;
      this.cdr.markForCheck();
      return;
    }

    // Armazena tokens
    localStorage.setItem('usuarioId', String(response.id ?? response.usuarioId ?? ''));

    this.loginSucesso.emit(response);
    this.carregando = false;
    this.cdr.markForCheck();

    // Redireciona para home
    if (this.redirecionar) {
      this.router.navigate(['/home']);
    }
  }
}


