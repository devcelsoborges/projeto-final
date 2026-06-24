import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PasswordResetService } from '../../service/password-reset.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, NgIf, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent implements OnInit {

  // Fluxo por LINK: o usuário pede o e-mail e recebe um link que abre direto a etapa
  // de nova senha (não há mais digitação de código na tela).
  currentStep: 'email' | 'emailSent' | 'password' | 'success' = 'email';
  emailForm!: FormGroup;
  passwordForm!: FormGroup;

  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  userEmail = '';
  resetToken = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private passwordResetService: PasswordResetService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeEmailForm();
    this.initializePasswordForm();
    this.handleDeepLink();
  }

  private initializeEmailForm(): void {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  private initializePasswordForm(): void {
    this.passwordForm = this.fb.group({
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/)
      ]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  private passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  /**
   * Quando o usuário chega pelo link do e-mail (/forgot-password?email=...&code=...),
   * pula direto para a etapa de nova senha. O código vai como token; a validação
   * acontece no submit (backend).
   */
  private handleDeepLink(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) {
      this.resetToken = token;
      this.currentStep = 'password';
      this.successMessage = 'Link verificado. Defina sua nova senha.';
    }
  }

  /**
   * PASSO 1: solicitar o e-mail de recuperação (envia o link).
   */
  submitEmail(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.userEmail = this.emailForm.get('email')?.value;
    this.cdr.markForCheck();

    this.passwordResetService.requestCode(this.userEmail).subscribe({
      next: (response) => {
        this.successMessage = response.message
          || 'Se o e-mail existir, enviamos um link para redefinir sua senha.';
        this.currentStep = 'emailSent';
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = this.extractErrorMessage(error, 'Erro ao solicitar recuperação de senha.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * PASSO 2 (via link): redefinir a senha.
   */
  submitNewPassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.cdr.markForCheck();

    const newPassword = this.passwordForm.get('newPassword')?.value;

    this.passwordResetService.resetPassword(this.resetToken, newPassword).subscribe({
      next: () => {
        this.currentStep = 'success';
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = this.extractErrorMessage(error,
          'Não foi possível redefinir a senha. O link pode ter expirado — solicite um novo.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Reenviar o e-mail de recuperação.
   */
  reenviarEmail(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.cdr.markForCheck();

    this.passwordResetService.requestCode(this.userEmail).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Enviamos um novo link para o seu e-mail.';
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = this.extractErrorMessage(error, 'Erro ao reenviar o e-mail.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Voltar para o início (pedir e-mail).
   */
  goBack(): void {
    this.errorMessage = null;
    this.successMessage = null;
    this.currentStep = 'email';
    this.passwordForm.reset();
  }

  backToLogin(): void {
    this.router.navigate(['/login']);
  }

  getEmailError(): string | null {
    const control = this.emailForm.get('email');
    if (!control || !control.touched) return null;
    if (control.errors?.['required']) return 'E-mail é obrigatório';
    if (control.errors?.['email']) return 'E-mail inválido';
    return null;
  }

  getPasswordError(): string | null {
    const control = this.passwordForm.get('newPassword');
    if (!control || !control.touched) return null;
    if (control.errors?.['required']) return 'Nova senha é obrigatória';
    if (control.errors?.['minlength']) return 'Mínimo 8 caracteres';
    if (control.errors?.['pattern']) {
      return 'Use 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial (@$!%*?&)';
    }
    return null;
  }

  getConfirmPasswordError(): string | null {
    const control = this.passwordForm.get('confirmPassword');
    if (!control || !control.touched) return null;
    if (control.errors?.['required']) return 'Confirmação é obrigatória';
    if (this.passwordForm.errors?.['passwordMismatch']) {
      return 'As senhas não coincidem';
    }
    return null;
  }

  private extractErrorMessage(error: any, fallbackMessage: string): string {
    if (!error?.error) {
      return fallbackMessage;
    }
    if (typeof error.error === 'string') {
      return error.error;
    }
    if (typeof error.error.message === 'string') {
      return error.error.message;
    }
    const firstFieldError = Object.values(error.error)[0];
    if (typeof firstFieldError === 'string') {
      return firstFieldError;
    }
    return fallbackMessage;
  }
}
