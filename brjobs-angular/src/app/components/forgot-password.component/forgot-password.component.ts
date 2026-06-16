import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { PasswordResetService } from '../../service/password-reset.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, NgIf, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent implements OnInit {
  
  // Estados do formulário
  currentStep: 'email' | 'code' | 'password' | 'success' = 'email';
  emailForm!: FormGroup;
  codeForm!: FormGroup;
  passwordForm!: FormGroup;
  
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  debugCode: string | null = null;
  
  userEmail: string = '';
  resetCode: string = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private passwordResetService: PasswordResetService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeEmailForm();
    this.initializeCodeForm();
    this.initializePasswordForm();
  }

  /**
   * Inicializa o formulário de e-mail
   */
  private initializeEmailForm(): void {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  /**
   * Inicializa o formulário de código de verificação
   */
  private initializeCodeForm(): void {
    this.codeForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  /**
   * Inicializa o formulário de nova senha
   */
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

  /**
   * Validador para verificar se as senhas coincidem
   */
  private passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    
    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  /**
   * PASSO 1: Submeter e-mail para recuperação
   */
  submitEmail(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.debugCode = null;
    this.userEmail = this.emailForm.get('email')?.value;
    this.cdr.markForCheck();

    this.passwordResetService.requestCode(this.userEmail).subscribe({
      next: (response) => {
        this.successMessage = response.message || `Se o e-mail existir, você receberá instruções para redefinir a senha.`;
        this.debugCode = response.debugCode || null;
        this.currentStep = 'code';
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
   * PASSO 2: Verificar código enviado
   */
  submitCode(): void {
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.resetCode = this.codeForm.get('code')?.value;
    this.cdr.markForCheck();

    this.passwordResetService.verifyCode(this.userEmail, this.resetCode).subscribe({
      next: () => {
        this.successMessage = 'Código verificado com sucesso! Digite sua nova senha.';
        this.currentStep = 'password';
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = this.extractErrorMessage(error, 'Erro ao verificar código.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * PASSO 3: Redefinir a senha
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

    this.passwordResetService.resetPassword(this.userEmail, this.resetCode, newPassword).subscribe({
      next: () => {
        this.currentStep = 'success';
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = this.extractErrorMessage(error, 'Erro ao redefinir a senha.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Voltar para o passo anterior
   */
  goBack(): void {
    this.errorMessage = null;
    this.successMessage = null;
    
    if (this.currentStep === 'code') {
      this.currentStep = 'email';
      this.codeForm.reset();
    } else if (this.currentStep === 'password') {
      this.currentStep = 'code';
      this.passwordForm.reset();
    }
  }

  /**
   * Reenviar código de verificação
   */
  resendCode(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.debugCode = null;
    this.cdr.markForCheck();

    this.passwordResetService.requestCode(this.userEmail).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Novo código enviado.';
        this.debugCode = response.debugCode || null;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = this.extractErrorMessage(error, 'Erro ao reenviar código.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Voltar para login após sucesso
   */
  backToLogin(): void {
    this.router.navigate(['/login']);
  }

  /**
   * Helpers para validação de campos
   */
  getEmailError(): string | null {
    const control = this.emailForm.get('email');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'E-mail é obrigatório';
    if (control.errors?.['email']) return 'E-mail inválido';
    return null;
  }

  getCodeError(): string | null {
    const control = this.codeForm.get('code');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Código é obrigatório';
    if (control.errors?.['minlength']) return 'Código deve ter 6 dígitos';
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
