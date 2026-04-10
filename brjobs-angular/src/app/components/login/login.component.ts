import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../service/auth.service';
import { SocialLoginService } from '../../service/social-login.service';
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
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private socialLoginService: SocialLoginService,
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
   * Toggle do checkbox "Lembrar e-mail"
   */
  toggleRememberMe(): void {
    this.rememberMe = !this.rememberMe;
    if (this.rememberMe && this.form.get('email')?.valid) {
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

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const { email, senha } = this.form.value;

    console.log('Tentativa de login:', { email });

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

          console.log('Login realizado com sucesso!', response);
          // Redirecionar para home após login bem-sucedido
          this.router.navigate(['/home']);
        },
        error: (error) => {
          console.error('Erro durante login:', error);
          
          if (error.status === 401) {
            this.errorMessage = 'E-mail ou senha inválidos. Tente novamente.';
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
    console.log('🔐 Iniciando login com Google...');
    this.errorMessage = null;
    this.socialLoading = true;

    try {
      this.socialLoginService.initiateGoogleSignIn(
        (credential: string) => {
          console.log('✅ Credencial Google recebida:', credential.substring(0, 20) + '...');

          // Extrair informações do token Google
          const googleData = this.socialLoginService.decodeGoogleToken(credential);
          console.log('📊 Dados do Google:', googleData);

          this.socialLoginService.loginWithGoogle(credential)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (response) => {
                console.log('✅ Login Google bem-sucedido:', response);

                // Se o backend retornou Demo User, usar dados do Google
                if (response.nome === 'Demo User' && googleData) {
                  response.nome = googleData.name || googleData.email || 'Usuário';
                }

                this.socialLoginService.storeSocialAuthTokens(response);
                this.authService.syncAuthStateFromStorage(true);

                console.log('🔄 Redirecionando para /home...');
                setTimeout(() => {
                  this.router.navigate(['/home']);
                }, 500);
              },
              error: (error) => {
                console.error('❌ Erro ao fazer login com Google:', error);
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
    } catch (error) {
      console.error('❌ Erro ao inicializar Google Sign-In:', error);
      this.errorMessage = 'Erro ao inicializar Google Sign-In. Verifique console.';
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
            this.router.navigate(['/home']);
          },
          error: (error) => {
            console.error('Erro ao fazer login com Facebook:', error);
            this.errorMessage = 'Erro ao fazer login com Facebook. Tente novamente.';
            this.socialLoading = false;
          }
        });
    });
  }

  /**
   * Login via Apple (Sign in with Apple)
   */
  loginWithApple(): void {
    this.errorMessage = null;
    this.socialLoading = true;

    // Simular chamada ao Apple Sign In
    // Note: Implementação real requer Apple SDK
    this.errorMessage = 'Apple Sign In em desenvolvimento. Tente novamente em breve.';
    this.socialLoading = false;
  }
}
