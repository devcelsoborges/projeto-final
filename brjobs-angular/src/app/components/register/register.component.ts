import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, NgForOf } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { RegisterService } from '../../service/register.service';
import { AuthService } from '../../service/auth.service';
import { SocialLoginService } from '../../service/social-login.service';
import { CepService } from '../../service/cep.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, NgForOf, ReactiveFormsModule, RouterModule], 
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit, OnDestroy {
  registerForm!: FormGroup;
  
  userTypes = ['Contratante', 'Prestador'];
  genders = ['Feminino', 'Masculino', 'Outro', 'Prefiro não informar'];

  loading = false;
  socialLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  private destroy$ = new Subject<void>();
  private ultimoCepConsultado = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private registerService: RegisterService,
    private authService: AuthService,
    private socialLoginService: SocialLoginService,
    private cepService: CepService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Inicializa o formulário com validadores
   */
  private initializeForm(): void {
    this.registerForm = this.fb.group({
      // CAMPOS DE AUTENTICAÇÃO
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(6), this.passwordStrengthValidator]],
      confirmarSenha: ['', Validators.required],
      
      // CAMPOS OBRIGATÓRIOS
      tipoUsuario: ['', Validators.required],
      nome: ['', [Validators.required, Validators.minLength(3)]],
      telefone: ['', [Validators.required, this.telefoneValidator]],
      cep: ['', [Validators.required, this.cepValidator]],
      rua: ['', Validators.required],
      numero: ['', Validators.required],
      complemento: [''],
      bairro: ['', Validators.required],
      cidade: ['', Validators.required],
      estado: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      cpf: ['', [Validators.required, this.cpfValidator]],
      dataNascimento: ['', [Validators.required, this.ageValidator]],
      genero: ['', Validators.required],
      
      // CAMPOS OPCIONAIS
      funcao: [''],
      experienciaProfissional: [''],
      especialidades: [''],
      
      // ARQUIVOS
      fotoPerfil: [null]
    }, { 
      validators: this.passwordMatchValidator 
    });
  }

  /**
   * Validador de força de senha - Alinhado com backend
   * Requer: 1 maiúscula, 1 minúscula, 1 número, 1 caractere especial (@$!%*?&)
   */
  private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumeric = /[0-9]/.test(value);
    const hasSpecialChar = /[@$!%*?&]/.test(value);

    // Deve ter TODOS os requisitos
    const isStrong = hasUpperCase && hasLowerCase && hasNumeric && hasSpecialChar && value.length >= 8;

    return !isStrong ? { weakPassword: true } : null;
  }

  /**
   * Validador de CPF (formato básico)
   */
  private cpfValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    // Remove máscara se houver
    const cpf = value.replace(/\D/g, '');

    if (cpf.length !== 11) {
      return { invalidCpf: true };
    }

    // Verifica se todos os dígitos são iguais
    if (/^(\d)\1{10}$/.test(cpf)) {
      return { invalidCpf: true };
    }

    // Validação básica de checksum (Módulo 11)
    let sum = 0;
    let remainder;

    for (let i = 1; i <= 9; i++) {
      sum += parseInt(cpf.substring(i - 1, i)) * (11 - i);
    }

    remainder = (sum * 10) % 11;
    if (remainder === 10 || remainder === 11) remainder = 0;
    if (remainder !== parseInt(cpf.substring(9, 10))) {
      return { invalidCpf: true };
    }

    sum = 0;
    for (let i = 1; i <= 10; i++) {
      sum += parseInt(cpf.substring(i - 1, i)) * (12 - i);
    }

    remainder = (sum * 10) % 11;
    if (remainder === 10 || remainder === 11) remainder = 0;
    if (remainder !== parseInt(cpf.substring(10, 11))) {
      return { invalidCpf: true };
    }

    return null;
  }

  /**
   * Validador de telefone
   */
  private telefoneValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const telefone = value.replace(/\D/g, '');
    return telefone.length >= 10 && telefone.length <= 11 ? null : { invalidTelefone: true };
  }

  private cepValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const cep = value.replace(/\D/g, '');
    return cep.length === 8 ? null : { invalidCep: true };
  }

  /**
   * Validador de idade (mínimo 18 anos)
   */
  private ageValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    const birthDate = new Date(value);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }

    return age >= 18 ? null : { underage: true };
  }

  /**
   * Validador de coincidência de senhas
   */
  private passwordMatchValidator(form: FormGroup): ValidationErrors | null {
    const senha = form.get('senha')?.value;
    const confirmarSenha = form.get('confirmarSenha')?.value;
    
    return senha && confirmarSenha && senha !== confirmarSenha ? { passwordMismatch: true } : null;
  }

  /**
   * Formata CPF (XXX.XXX.XXX-XX)
   */
  formatCPF(event: any): void {
    const input = event.target;
    let value = input.value.replace(/\D/g, '');

    if (value.length > 11) {
      value = value.slice(0, 11);
    }

    if (value.length > 5) {
      value = value.slice(0, 3) + '.' + value.slice(3, 6) + '.' + value.slice(6, 9) + '-' + value.slice(9);
    } else if (value.length > 2) {
      value = value.slice(0, 3) + '.' + value.slice(3);
    }

    input.value = value;
    this.registerForm.get('cpf')?.setValue(value, { emitEvent: false });
  }

  /**
   * Formata telefone ((XX) XXXXX-XXXX)
   */
  formatTelefone(event: any): void {
    const input = event.target;
    let value = input.value.replace(/\D/g, '');

    if (value.length > 11) {
      value = value.slice(0, 11);
    }

    if (value.length > 6) {
      value = '(' + value.slice(0, 2) + ') ' + value.slice(2, 7) + '-' + value.slice(7);
    } else if (value.length > 2) {
      value = '(' + value.slice(0, 2) + ') ' + value.slice(2);
    }

    input.value = value;
    this.registerForm.get('telefone')?.setValue(value, { emitEvent: false });
  }

  formatCep(event: any): void {
    const input = event.target;
    let value = input.value.replace(/\D/g, '');

    if (value.length > 8) {
      value = value.slice(0, 8);
    }

    if (value.length > 5) {
      value = `${value.slice(0, 5)}-${value.slice(5)}`;
    }

    input.value = value;
    this.registerForm.get('cep')?.setValue(value, { emitEvent: false });

    const cepLimpo = value.replace(/\D/g, '');
    if (cepLimpo.length === 8 && cepLimpo !== this.ultimoCepConsultado) {
      this.buscarEnderecoPorCep(cepLimpo);
    }
  }

  private buscarEnderecoPorCep(cep: string): void {
    this.ultimoCepConsultado = cep;

    this.cepService.consultarCep(cep)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dados) => {
          if (dados?.erro) {
            this.errorMessage = 'CEP não encontrado.';
            return;
          }

          const enderecoPartes = [
            { campo: 'rua', valor: dados.logradouro || '' },
            { campo: 'bairro', valor: dados.bairro || '' },
            { campo: 'cidade', valor: dados.localidade || '' },
            { campo: 'estado', valor: (dados.uf || '').toUpperCase() }
          ];

          enderecoPartes.forEach(item => {
            if (item.valor) {
              this.registerForm.get(item.campo)?.setValue(item.valor);
            }
          });

          this.errorMessage = null;
        },
        error: () => {
          this.errorMessage = 'Não foi possível consultar o CEP agora.';
        }
      });
  }

  private montarEnderecoCompleto(formValue: any): string {
    const linhaPrincipal = `${formValue.rua}, ${formValue.numero}`;
    const linhaSecundaria = [formValue.complemento, formValue.bairro].filter(Boolean).join(', ');
    const cidadeUf = `${formValue.cidade} - ${String(formValue.estado || '').toUpperCase()}`;
    const cepLimpo = String(formValue.cep || '').replace(/\D/g, '').slice(0, 8);
    const cepFormatado = cepLimpo.length > 5 ? `${cepLimpo.slice(0, 5)}-${cepLimpo.slice(5)}` : cepLimpo;

    return [linhaPrincipal, linhaSecundaria, cidadeUf, cepFormatado].filter(Boolean).join(', ');
  }

  /**
   * Manipula seleção de arquivos
   */
  onFileChange(event: any, fieldName: 'fotoPerfil'): void {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];

      // Validação de tamanho (máx 5MB)
      const maxSize = 5 * 1024 * 1024;
      if (file.size > maxSize) {
        this.errorMessage = `Arquivo muito grande. Máximo 5MB. (${(file.size / 1024 / 1024).toFixed(2)}MB)`;
        event.target.value = '';
        return;
      }

      // Validação de tipo para foto
      if (fieldName === 'fotoPerfil' && !file.type.startsWith('image/')) {
        this.errorMessage = 'A foto deve ser uma imagem (JPG, PNG, etc).';
        event.target.value = '';
        return;
      }

      this.errorMessage = null;
      this.registerForm.get(fieldName)?.setValue(file);
      this.registerForm.get(fieldName)?.updateValueAndValidity();
    }
  }

  /**
   * Submete o formulário
   */
  onSubmit(): void {
    this.errorMessage = null;
    this.successMessage = null;

    if (this.registerForm.invalid) {
      this.markAllAsTouched(this.registerForm);
      this.errorMessage = 'Por favor, corrija os erros no formulário.';
      return;
    }

    this.loading = true;
    const formValue = this.registerForm.value;
    const tipoUsuario = formValue.tipoUsuario.toUpperCase();

    // Converter data para o formato esperado pelo backend (YYYY-MM-DD)
    let dataNascimento = formValue.dataNascimento;
    if (dataNascimento instanceof Date) {
      dataNascimento = dataNascimento.toISOString().split('T')[0];
    } else if (typeof dataNascimento === 'string' && dataNascimento.includes('/')) {
      // Converter de DD/MM/YYYY para YYYY-MM-DD
      const [dia, mes, ano] = dataNascimento.split('/');
      dataNascimento = `${ano}-${mes}-${dia}`;
    }

    // Preparar dados comuns
    const dadosComuns = {
      nome: formValue.nome,
      email: formValue.email,
      senha: formValue.senha,
      telefone: formValue.telefone.replace(/\D/g, ''),
      dataNascimento: dataNascimento,
      cpf: formValue.cpf.replace(/\D/g, ''),
      genero: formValue.genero,
      endereco: this.montarEnderecoCompleto(formValue),
      cep: (formValue.cep || '').replace(/\D/g, ''),
      rua: formValue.rua || '',
      numero: formValue.numero || '',
      complemento: formValue.complemento || '',
      bairro: formValue.bairro || '',
      cidade: formValue.cidade || '',
      estado: String(formValue.estado || '').toUpperCase(),
      bio: ''
    };

    if (tipoUsuario === 'PRESTADOR') {
      // Registrar como prestador
      const dadosPrestador = {
        ...dadosComuns,
        funcao: formValue.funcao || '',
        experienciaProfissional: formValue.experienciaProfissional || '',
        especialidades: formValue.especialidades || ''
      };

      this.registerService.registrarPrestador(dadosPrestador)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (usuario) => this.handleRegistroSucesso(usuario),
          error: (error) => this.handleRegistroErro(error)
        });
    } else {
      // Registrar como contratante
      this.registerService.registrarContratante(dadosComuns)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (usuario) => this.handleRegistroSucesso(usuario),
          error: (error) => this.handleRegistroErro(error)
        });
    }
  }

  /**
   * Trata sucesso no registro
   */
  private handleRegistroSucesso(usuario: any): void {
    console.log('Cadastro realizado com sucesso!', usuario);
    this.successMessage = `Bem-vindo, ${usuario.nome}! Redirecionando para o login...`;
    
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 2000);
  }

  /**
   * Trata erro no registro
   */
  private handleRegistroErro(error: any): void {
    console.error('Erro ao registrar:', error);
    const backendMessage =
      error?.error?.message ||
      (typeof error?.error === 'string' ? error.error : null) ||
      error?.message;
    
    if (error.status === 409) {
      this.errorMessage = backendMessage || 'Este email ou CPF já está registrado.';
    } else if (error.status === 400) {
      this.errorMessage = backendMessage || 'Dados inválidos. Verifique os campos.';
    } else if (error.status === 0) {
      this.errorMessage = 'Erro ao conectar ao servidor. Verifique se o backend está rodando.';
    } else {
      this.errorMessage = backendMessage || 'Erro ao realizar cadastro. Tente novamente.';
    }
    this.loading = false;
  }

  /**
   * Marca todos os campos como touched para mostrar erros
   */
  private markAllAsTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markAllAsTouched(control);
      }
    });
  }

  /**
   * Helpers para obter mensagens de erro específicas
   */
  getEmailError(): string | null {
    const control = this.registerForm.get('email');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'E-mail é obrigatório';
    if (control.errors?.['email']) return 'E-mail inválido';
    return null;
  }

  getSenhaError(): string | null {
    const control = this.registerForm.get('senha');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Senha é obrigatória';
    if (control.errors?.['minlength']) return 'Mínimo 8 caracteres';
    if (control.errors?.['weakPassword']) return 'Senha deve conter: 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial (@$!%*?&)';
    return null;
  }

  getConfirmarSenhaError(): string | null {
    const control = this.registerForm.get('confirmarSenha');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Confirmação é obrigatória';
    if (this.registerForm.errors?.['passwordMismatch']) return 'As senhas não coincidem';
    return null;
  }

  getCpfError(): string | null {
    const control = this.registerForm.get('cpf');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'CPF é obrigatório';
    if (control.errors?.['invalidCpf']) return 'CPF inválido';
    return null;
  }

  getTelefoneError(): string | null {
    const control = this.registerForm.get('telefone');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Telefone é obrigatório';
    if (control.errors?.['invalidTelefone']) return 'Telefone deve ter 10 ou 11 dígitos';
    return null;
  }

  getCepError(): string | null {
    const control = this.registerForm.get('cep');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'CEP é obrigatório';
    if (control.errors?.['invalidCep']) return 'CEP deve ter 8 dígitos';
    return null;
  }

  getIdadeError(): string | null {
    const control = this.registerForm.get('dataNascimento');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Data de nascimento é obrigatória';
    if (control.errors?.['underage']) return 'Você deve ter pelo menos 18 anos';
    return null;
  }

  getNomeError(): string | null {
    const control = this.registerForm.get('nome');
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Nome é obrigatório';
    if (control.errors?.['minlength']) return 'Nome deve ter no mínimo 3 caracteres';
    return null;
  }

  /**
   * Login via Google durante registro
   */
  signUpWithGoogle(): void {
    this.errorMessage = null;
    this.socialLoading = true;

    this.socialLoginService.initiateGoogleSignIn(
      (credential: string) => {
        this.socialLoginService.loginWithGoogle(credential)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (response) => {
              this.socialLoginService.storeSocialAuthTokens(response);
              this.authService.syncAuthStateFromStorage(true);
              this.successMessage = `Bem-vindo, ${response.nome}! Redirecionando...`;
              setTimeout(() => {
                this.router.navigate(['/home']);
              }, 1500);
            },
            error: (error) => {
              console.error('Erro ao registrar com Google:', error);
              const backendMessage =
                error?.error?.error ||
                error?.error?.message ||
                (typeof error?.error === 'string' ? error.error : null) ||
                error?.message;
              this.errorMessage = backendMessage || 'Erro ao registrar com Google. Tente novamente.';
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
  }

  /**
   * Login via Facebook durante registro
   */
  signUpWithFacebook(): void {
    this.errorMessage = null;
    this.socialLoading = true;

    this.socialLoginService.initiateFacebookSignIn((token: string) => {
      this.socialLoginService.loginWithFacebook(token)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.socialLoginService.storeSocialAuthTokens(response);
            this.successMessage = `Bem-vindo, ${response.nome}! Redirecionando...`;
            setTimeout(() => {
              this.router.navigate(['/home']);
            }, 1500);
          },
          error: (error) => {
            console.error('Erro ao registrar com Facebook:', error);
            this.errorMessage = 'Erro ao registrar com Facebook. Tente novamente.';
            this.socialLoading = false;
          }
        });
    });
  }

  /**
   * Signup via Apple
   */
  signUpWithApple(): void {
    this.errorMessage = null;
    this.socialLoading = true;

    // Simular chamada ao Apple Sign In
    this.errorMessage = 'Apple Sign In em desenvolvimento. Tente novamente em breve.';
    this.socialLoading = false;
  }
}
