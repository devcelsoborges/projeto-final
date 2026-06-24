import { ChangeDetectorRef, Component, OnDestroy, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { AuthService } from "../../service/auth.service";
import { RegisterService } from "../../service/register.service";
import { SocialLoginService } from "../../service/social-login.service";

@Component({
  selector: "app-register",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: "./register.component.html",
  styleUrls: ["./register.component.css"]
})
export class RegisterComponent implements OnInit, OnDestroy {
  registerForm!: FormGroup;
  loading = false;
  socialLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private registerService: RegisterService,
    private authService: AuthService,
    private socialLoginService: SocialLoginService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      nome: ["", [Validators.required, Validators.minLength(3)]],
      email: ["", [Validators.required, Validators.email]],
      senha: ["", [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
      confirmarSenha: ["", Validators.required]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    this.errorMessage = null;
    this.successMessage = null;

    if (this.registerForm.invalid) {
      this.markAllAsTouched();
      this.errorMessage = "Corrija os campos destacados para criar sua conta.";
      return;
    }

    const formValue = this.registerForm.value;
    this.loading = true;
    this.cdr.markForCheck();

    this.registerService.registrarContratante({
      nome: formValue.nome.trim(),
      email: formValue.email.trim(),
      senha: formValue.senha,
      confirmacaoSenha: formValue.confirmarSenha
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (usuario) => {
          this.authService.setUsuarioAtual(usuario);
          this.successMessage =
            `Conta criada! Enviamos um e-mail de confirmação para ${usuario.email}. ` +
            `Confirme seu e-mail para poder publicar.`;
          this.loading = false;
          this.cdr.markForCheck();
          setTimeout(() => this.router.navigate(["/home"]), 2500);
        },
        error: (error) => this.handleRegistroErro(error)
      });
  }

  signUpWithGoogle(): void {
    this.errorMessage = null;
    this.socialLoading = true;
    this.cdr.markForCheck();

    this.socialLoginService.initiateGoogleSignIn(
      (credential: string) => {
        this.socialLoginService.loginWithGoogle(credential)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (response) => {
              this.socialLoginService.storeSocialAuthTokens(response);
              this.authService.syncAuthStateFromStorage(true);
              this.successMessage = `Bem-vindo, ${response.nome}!`;
              this.socialLoading = false;
              this.cdr.markForCheck();
              setTimeout(() => this.router.navigate(["/home"]), 800);
            },
            error: (error) => {
              const backendMessage =
                error?.error?.error ||
                error?.error?.message ||
                (typeof error?.error === "string" ? error.error : null) ||
                error?.message;
              this.errorMessage = backendMessage || "Erro ao registrar com Google. Tente novamente.";
              this.socialLoading = false;
              this.cdr.markForCheck();
            }
          });
      },
      (message: string) => {
        this.errorMessage = message;
        this.socialLoading = false;
        this.cdr.markForCheck();
      },
      () => {
        this.socialLoading = false;
        this.cdr.markForCheck();
      }
    );
  }

  signUpWithFacebook(): void {
    this.errorMessage = null;
    this.socialLoading = true;
    this.cdr.markForCheck();

    this.socialLoginService.initiateFacebookSignIn((token: string) => {
      this.socialLoginService.loginWithFacebook(token)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.socialLoginService.storeSocialAuthTokens(response);
            this.authService.syncAuthStateFromStorage(true);
            this.successMessage = `Bem-vindo, ${response.nome}!`;
            this.socialLoading = false;
            this.cdr.markForCheck();
            setTimeout(() => this.router.navigate(["/home"]), 800);
          },
          error: (error) => {
            const backendMessage = error?.error?.message || error?.message;
            this.errorMessage = backendMessage || "Erro ao registrar com Facebook. Tente novamente.";
            this.socialLoading = false;
            this.cdr.markForCheck();
          }
        });
    });
  }

  getNomeError(): string | null {
    const control = this.registerForm.get("nome");
    if (!control || !control.touched) return null;
    if (control.errors?.["required"]) return "Nome é obrigatório.";
    if (control.errors?.["minlength"]) return "Informe seu nome completo.";
    return null;
  }

  getEmailError(): string | null {
    const control = this.registerForm.get("email");
    if (!control || !control.touched) return null;
    if (control.errors?.["required"]) return "E-mail é obrigatório.";
    if (control.errors?.["email"]) return "Informe um e-mail válido.";
    return null;
  }

  getSenhaError(): string | null {
    const control = this.registerForm.get("senha");
    if (!control || !control.touched) return null;
    if (control.errors?.["required"]) return "Senha é obrigatória.";
    if (control.errors?.["minlength"] || control.errors?.["weakPassword"]) {
      return "A senha ainda não atende aos requisitos.";
    }
    return null;
  }

  getConfirmarSenhaError(): string | null {
    const control = this.registerForm.get("confirmarSenha");
    if (!control || !control.touched) return null;
    if (control.errors?.["required"]) return "Confirmação é obrigatória.";
    if (this.registerForm.errors?.["passwordMismatch"]) return "As senhas não coincidem.";
    return null;
  }

  private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const isStrong =
      value.length >= 8 &&
      /[A-Z]/.test(value) &&
      /[a-z]/.test(value) &&
      /[0-9]/.test(value) &&
      /[@$!%*?&]/.test(value);

    return isStrong ? null : { weakPassword: true };
  }

  private passwordMatchValidator(form: FormGroup): ValidationErrors | null {
    const senha = form.get("senha")?.value;
    const confirmarSenha = form.get("confirmarSenha")?.value;
    return senha && confirmarSenha && senha !== confirmarSenha ? { passwordMismatch: true } : null;
  }

  private markAllAsTouched(): void {
    Object.values(this.registerForm.controls).forEach((control) => control.markAsTouched());
  }

  private handleRegistroErro(error: any): void {
    const backendMessage =
      error?.error?.message ||
      error?.message ||
      (typeof error?.error === "string" ? error.error : null);

    if (error?.status === 409) {
      this.errorMessage = backendMessage || "Este e-mail já está cadastrado. Entre na sua conta.";
    } else if (error?.status === 400) {
      this.errorMessage = backendMessage || "Dados inválidos. Verifique os campos.";
    } else if (error?.status === 0) {
      this.errorMessage = "Não foi possível conectar ao servidor.";
    } else {
      this.errorMessage = backendMessage || "Erro ao realizar cadastro. Tente novamente.";
    }

    this.loading = false;
    this.cdr.markForCheck();
  }
}
