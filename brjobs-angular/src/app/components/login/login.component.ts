import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../service/auth.service';
import { SocialLoginService } from '../../service/social-login.service';
import { AccountService } from '../../service/account.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, NgIf, ReactiveFormsModule, RouterModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  loading = false;
  socialLoading = false;
  errorMessage: string | null = null;
  rememberMe = false;
  // Conta com e-mail não confirmado: mostra a opção de reenviar a confirmação.
  showResendConfirmation = false;
  unconfirmedEmail: string | null = null;
  resendMessage: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private socialLoginService: SocialLoginService,
    private accountService: AccountService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loading = false;
    this.socialLoading = false;
    this.initializeForm();
    this.loadRememberedEmail();

    queueMicrotask(() => this.cdr.detectChanges());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Inicializa o formulário de login com validadores
   */
  private initializeForm(): void {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(8)]],  // Mínimo 8 caracteres como exigido pelo backend
    });
  }

  /**
   * Carrega email salvo do localStorage se existir
   */
  private loadRememberedEmail(): void {
    const rememberedEmail = localStorage.getItem('rememberedEmail');
    if (rememberedEmail) {
      this.form.patchValue({ email: rememberedEmail });
      this.rememberMe = true;
    }
  }

  /**
   * Atualiza a preferencia de lembrar e-mail sem registrar o checkbox no formGroup.
   */
  onRememberMeChange(checked: boolean): void {
    this.rememberMe = checked;
    if (checked && this.form.get('email')?.valid) {
      localStorage.setItem('rememberedEmail', this.form.get('email')?.value);
    } else {
      localStorage.removeItem('rememberedEmail');
    }
  }

  /**
   * Submete o formulário de login
   */
  submit(): void {
    this.errorMessage = null;
    this.showResendConfirmation = false;
    this.resendMessage = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const { email, senha } = this.form.value;

    // Chamada real ao backend via AuthService
    this.authService.login(email, senha)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          // Salvar email se "Lembrar e-mail" está ativado
          if (this.rememberMe) {
            localStorage.setItem('rememberedEmail', email);
          } else {
            localStorage.removeItem('rememberedEmail');
          }

          // Redirecionar para home após login bem-sucedido
          this.router.navigate(['/home']);
        },
        error: (error) => {
          if (error.status === 403 && error.error?.code === 'EMAIL_NOT_CONFIRMED') {
            this.errorMessage = error.error?.message || 'Confirme seu e-mail para acessar a conta.';
            this.unconfirmedEmail = error.error?.email || email;
            this.showResendConfirmation = true;
          } else if (error.status === 401) {
            // Usa a mensagem do backend (ex.: conta criada com login social orienta a
            // entrar com o provedor ou usar "Esqueci minha senha"); cai no texto padrão
            // só quando não há mensagem (senha realmente inválida).
            this.errorMessage = error.error?.message || 'E-mail ou senha inválidos. Tente novamente.';
          } else if (error.status === 0) {
            this.errorMessage = 'Erro ao conectar ao servidor. Verifique se o backend está rodando.';
          } else {
            this.errorMessage = error.error?.message || 'Erro ao fazer login. Tente novamente.';
          }
          this.loading = false;
        }
      });
  }

  /**
   * Reenvia o e-mail de confirmação para a conta não confirmada.
   */
  reenviarConfirmacao(): void {
    const alvo = this.unconfirmedEmail || this.form.get('email')?.value;
    if (!alvo) {
      return;
    }
    this.resendMessage = null;
    this.accountService.reenviarConfirmacao(alvo)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.resendMessage = res.message || 'E-mail de confirmação reenviado.';
          this.cdr.detectChanges();
        },
        error: () => {
          this.resendMessage = 'Não foi possível reenviar agora. Tente novamente em instantes.';
          this.cdr.detectChanges();
        }
      });
  }

  /**
   * Verifica se há erro específico no campo de email
   */
  getEmailError(): string | null {
    const emailControl = this.form.get('email');
    if (!emailControl || !emailControl.touched) return null;

    if (emailControl.errors?.['required']) {
      return 'E-mail é obrigatório';
    }
    if (emailControl.errors?.['email']) {
      return 'E-mail inválido';
    }
    return null;
  }

  /**
   * Verifica se há erro específico no campo de senha
   */
  getSenhaError(): string | null {
    const senhaControl = this.form.get('senha');
    if (!senhaControl || !senhaControl.touched) return null;

    if (senhaControl.errors?.['required']) {
      return 'Senha é obrigatória';
    }
    if (senhaControl.errors?.['minlength']) {
      return 'Mínimo 8 caracteres';
    }
    return null;
  }

  /**
   * Login via Google
   */
  loginWithGoogle(): void {
this.errorMessage = null;
    this.socialLoading = true;

    try {
      this.socialLoginService.initiateGoogleSignIn(
        (credential: string) => {
// Extrair informações do token Google
          const googleData = this.socialLoginService.decodeGoogleToken(credential);
this.socialLoginService.loginWithGoogle(credential)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (response) => {
// Se o backend retornou Demo User, usar dados do Google
                if (response.nome === 'Demo User' && googleData) {
                  response.nome = googleData.name || googleData.email || 'Usuário';
                }

                this.socialLoginService.storeSocialAuthTokens(response);
                this.authService.markAuthenticated(response as any);
setTimeout(() => {
                  this.router.navigate(['/home']);
                }, 500);
              },
              error: (error) => {
const backendMessage =
                  error?.error?.error ||
                  error?.error?.message ||
                  (typeof error?.error === 'string' ? error.error : null) ||
                  error?.message;
                this.errorMessage = backendMessage || 'Erro ao fazer login com Google. Tente novamente.';
                this.socialLoading = false;
              }
            });
        },
        (message: string) => {
          this.errorMessage = message;
          this.socialLoading = false;
        },
        () => {
          this.socialLoading = false;
        }
      );
    } catch {
      this.errorMessage = 'Erro ao inicializar Google Sign-In.';
      this.socialLoading = false;
    }
  }

  /**
   * Login via Facebook
   */
  loginWithFacebook(): void {
    this.errorMessage = null;
    this.socialLoading = true;

    this.socialLoginService.initiateFacebookSignIn((token: string) => {
      this.socialLoginService.loginWithFacebook(token)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.socialLoginService.storeSocialAuthTokens(response);
            this.authService.markAuthenticated(response as any);
            this.router.navigate(['/home']);
          },
          error: (error) => {
this.errorMessage = 'Erro ao fazer login com Facebook. Tente novamente.';
            this.socialLoading = false;
          }
        });
    });
  }
}
