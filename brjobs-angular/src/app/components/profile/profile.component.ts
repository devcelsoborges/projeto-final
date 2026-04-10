import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProfileStateService } from '../../service/profile-state.service';
import { CepService } from '../../service/cep.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

interface Usuario {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  cep?: string;
  rua?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  cpf: string;
  genero: string;
  dataNascimento: string;
  endereco: string;
  tipoUsuario: 'CONTRATANTE' | 'PRESTADOR';
  ativo: boolean;
  dataCadastro: string;
  dataAtualizacao?: string;
  fotoPerfil?: string;
}

interface Prestador {
  id: number;
  usuario: Usuario;
  funcao: string;
  experienciaProfissional: string;
  especialidades: string;
  curriculo?: string;
  descricao: string;
  ativo: boolean;
  notaMedia?: number;
  quantidadeAvaliacoes: number;
  dataCadastro: string;
  dataAtualizacao?: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit, OnDestroy {
  
  // Dados do usuário
  usuario!: Usuario;
  prestador?: Prestador;
  
  // Estado do componente
  isLoading = true;
  isEditing = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  
  // Formulário de edição
  profileForm!: FormGroup;
  prestadorForm!: FormGroup;

  // Abas
  currentTab: 'perfil' | 'profissional' | 'avaliacoes' = 'perfil';

  // Dados de avaliações (simulado)
  avaliacoes: any[] = [];

  private destroy$ = new Subject<void>();
  private ultimoCepConsultado = '';

  constructor(
    private fb: FormBuilder,
    private profileStateService: ProfileStateService,
    private cepService: CepService
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    this.loadUserProfile();

    // Escuta mudanças no estado de edição do serviço
    this.profileStateService.editing$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isEditing => {
        this.isEditing = isEditing;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Inicializa os formulários
   */
  private initializeForms(): void {
    this.profileForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required, this.telefoneValidator]],
      cep: ['', [Validators.required, this.cepValidator]],
      rua: ['', [Validators.required]],
      numero: ['', [Validators.required]],
      complemento: [''],
      bairro: ['', [Validators.required]],
      cidade: ['', [Validators.required]],
      estado: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      genero: [''],
      dataNascimento: [''],
      tipoUsuario: ['PRESTADOR', [Validators.required]]
    });

    this.prestadorForm = this.fb.group({
      funcao: ['', [Validators.required]],
      especialidades: ['', [Validators.required]],
      experienciaProfissional: [''],
      descricao: ['']
    });
  }

  /**
   * Carrega o perfil do usuário
   */
  private loadUserProfile(): void {
    this.errorMessage = null;

    try {
      // Tentar carregar do localStorage primeiro (para Google SSO)
      const nomeLS = localStorage.getItem('usuario_nome');
      const emailLS = localStorage.getItem('usuario_email');
      const tipoLS = localStorage.getItem('usuario_tipo');

      if (nomeLS && emailLS) {
        this.usuario = {
          id: parseInt(localStorage.getItem('usuario_id') || '1'),
          nome: nomeLS,
          email: emailLS,
          telefone: localStorage.getItem('usuario_telefone') || '',
          cep: localStorage.getItem('usuario_cep') || '',
          rua: localStorage.getItem('usuario_rua') || '',
          numero: localStorage.getItem('usuario_numero') || '',
          complemento: localStorage.getItem('usuario_complemento') || '',
          bairro: localStorage.getItem('usuario_bairro') || '',
          cidade: localStorage.getItem('usuario_cidade') || '',
          estado: localStorage.getItem('usuario_estado') || '',
          cpf: localStorage.getItem('usuario_cpf') || '',
          genero: localStorage.getItem('usuario_genero') || '',
          dataNascimento: localStorage.getItem('usuario_dataNascimento') || '',
          endereco: localStorage.getItem('usuario_endereco') || '',
          tipoUsuario: (tipoLS as 'CONTRATANTE' | 'PRESTADOR') || 'PRESTADOR',
          ativo: true,
          dataCadastro: localStorage.getItem('usuario_dataCadastro') || new Date().toISOString(),
          fotoPerfil: 'assets/default-avatar.png'
        };
      } else {
        // Fallback: Dados de exemplo
        this.usuario = {
          id: 1,
          nome: 'Celso Borges',
          email: 'borgesnetocs@gmail.com',
          telefone: '(11) 99999-9999',
          cep: '01310-100',
          rua: 'Avenida Paulista',
          numero: '1000',
          complemento: '',
          bairro: 'Bela Vista',
          cidade: 'São Paulo',
          estado: 'SP',
          cpf: '123.456.789-00',
          genero: 'Masculino',
          dataNascimento: '1990-05-15',
          endereco: 'São Paulo, SP',
          tipoUsuario: 'PRESTADOR',
          ativo: true,
          dataCadastro: '2024-01-15T10:30:00',
          fotoPerfil: 'assets/default-avatar.png'
        };
      }

      this.hidratarEnderecoLegado();

      // Se é prestador, carregar dados profissionais
      if (this.usuario.tipoUsuario === 'PRESTADOR') {
        this.prestador = {
          id: 1,
          usuario: this.usuario,
          funcao: 'Desenvolvedor Full Stack',
          especialidades: 'Angular, TypeScript, Node.js, MongoDB',
          experienciaProfissional: 'Mais de 5 anos de experiência em desenvolvimento web',
          descricao: 'Profissional dedicado com experiência em diversos projetos',
          ativo: true,
          notaMedia: 4.8,
          quantidadeAvaliacoes: 15,
          dataCadastro: '2024-01-15T10:30:00'
        };

        this.loadAvaliacoes();
      }

      // Carrega dados nos formulários
      this.profileForm.patchValue({
        nome: this.usuario.nome,
        email: this.usuario.email,
        telefone: this.formatTelefone(this.usuario.telefone || ''),
        cep: this.formatCep(this.usuario.cep || ''),
        rua: this.usuario.rua || '',
        numero: this.usuario.numero || '',
        complemento: this.usuario.complemento || '',
        bairro: this.usuario.bairro || '',
        cidade: this.usuario.cidade || '',
        estado: (this.usuario.estado || '').toUpperCase(),
        genero: this.usuario.genero,
        dataNascimento: this.usuario.dataNascimento,
        tipoUsuario: this.usuario.tipoUsuario
      });

      if (this.prestador) {
        this.prestadorForm.patchValue({
          funcao: this.prestador.funcao,
          especialidades: this.prestador.especialidades,
          experienciaProfissional: this.prestador.experienciaProfissional,
          descricao: this.prestador.descricao
        });
      }

      this.isLoading = false;
    } catch (error) {
      console.error('Erro ao carregar perfil:', error);
      this.errorMessage = 'Erro ao carregar perfil. Tente novamente.';
      this.isLoading = false;
    }
  }

  /**
   * Carrega as avaliações do prestador
   */
  private loadAvaliacoes(): void {
    // Simula dados de avaliações - em produção vem do backend
    this.avaliacoes = [
      {
        id: 1,
        nota: 5,
        comentario: 'Excelente profissional, muito atencioso',
        usuario: 'Maria Silva',
        data: '2024-11-20'
      },
      {
        id: 2,
        nota: 4.5,
        comentario: 'Ótimo trabalho, recomendo',
        usuario: 'Carlos Oliveira',
        data: '2024-11-15'
      },
      {
        id: 3,
        nota: 5,
        comentario: 'Perfeito! Entregou antes do prazo',
        usuario: 'Ana Costa',
        data: '2024-11-10'
      }
    ];
  }

  /**
   * Ativa o modo de edição
   */
  toggleEdit(): void {
    this.profileStateService.toggleEditing();
    if (!this.isEditing) {
      this.loadUserProfile();
    }
  }

  /**
   * Salva as alterações do perfil
   */
  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.errorMessage = 'Por favor, preencha todos os campos obrigatórios.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    // Simula chamada ao backend
    setTimeout(() => {
      try {
        const formData = this.profileForm.value;
        const enderecoCompleto = this.montarEnderecoCompleto(formData);
        
        // Atualiza o objeto usuário
        this.usuario = {
          ...this.usuario,
          ...formData,
          estado: String(formData.estado || '').toUpperCase(),
          endereco: enderecoCompleto
        };

        // Salva no localStorage
        localStorage.setItem('usuario_nome', formData.nome);
        localStorage.setItem('usuario_email', formData.email);
        localStorage.setItem('usuario_telefone', (formData.telefone || '').replace(/\D/g, ''));
        localStorage.setItem('usuario_cep', (formData.cep || '').replace(/\D/g, ''));
        localStorage.setItem('usuario_rua', formData.rua || '');
        localStorage.setItem('usuario_numero', formData.numero || '');
        localStorage.setItem('usuario_complemento', formData.complemento || '');
        localStorage.setItem('usuario_bairro', formData.bairro || '');
        localStorage.setItem('usuario_cidade', formData.cidade || '');
        localStorage.setItem('usuario_estado', String(formData.estado || '').toUpperCase());
        localStorage.setItem('usuario_endereco', enderecoCompleto);
        localStorage.setItem('usuario_genero', formData.genero);
        localStorage.setItem('usuario_dataNascimento', formData.dataNascimento);
        localStorage.setItem('usuario_tipo', formData.tipoUsuario);

        this.successMessage = 'Perfil atualizado com sucesso!';
        this.isEditing = false;
        this.isLoading = false;

        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      } catch (error) {
        console.error('Erro ao salvar perfil:', error);
        this.errorMessage = 'Erro ao salvar perfil. Tente novamente.';
        this.isLoading = false;
      }
    }, 1500);
  }

  /**
   * Salva as alterações do perfil profissional
   */
  savePrestadorProfile(): void {
    if (this.prestadorForm.invalid) {
      this.prestadorForm.markAllAsTouched();
      this.errorMessage = 'Por favor, preencha os campos obrigatórios.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    // Simula chamada ao backend
    setTimeout(() => {
      try {
        const formData = this.prestadorForm.value;
        
        // Atualiza o objeto prestador
        if (this.prestador) {
          this.prestador = {
            ...this.prestador,
            ...formData
          };
        }

        this.successMessage = 'Perfil profissional atualizado com sucesso!';
        this.isLoading = false;

        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      } catch (error) {
        this.errorMessage = 'Erro ao salvar perfil profissional. Tente novamente.';
        this.isLoading = false;
      }
    }, 1500);
  }

  /**
   * Manipula upload de foto de perfil
   */
  onProfilePhotoChange(event: any): void {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];

      // Validação de tamanho (máx 5MB)
      const maxSize = 5 * 1024 * 1024;
      if (file.size > maxSize) {
        this.errorMessage = `Arquivo muito grande. Máximo 5MB.`;
        return;
      }

      // Validação de tipo
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'O arquivo deve ser uma imagem.';
        return;
      }

      // Simula leitura de arquivo
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.usuario.fotoPerfil = e.target.result;
        this.successMessage = 'Foto atualizada com sucesso!';
        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      };
      reader.readAsDataURL(file);
    }
  }

  /**
   * Manipula upload de currículo
   */
  onCurriculoChange(event: any): void {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];

      // Validação de tamanho (máx 10MB)
      const maxSize = 10 * 1024 * 1024;
      if (file.size > maxSize) {
        this.errorMessage = `Arquivo muito grande. Máximo 10MB.`;
        return;
      }

      // Validação de tipo
      if (!['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'].includes(file.type)) {
        this.errorMessage = 'O currículo deve ser PDF ou DOCX.';
        return;
      }

      this.successMessage = `Currículo "${file.name}" enviado com sucesso!`;
      setTimeout(() => {
        this.successMessage = null;
      }, 3000);
    }
  }

  /**
   * Muda a aba ativa
   */
  switchTab(tab: 'perfil' | 'profissional' | 'avaliacoes'): void {
    this.currentTab = tab;
  }

  /**
   * Retorna a classe CSS para a aba ativa
   */
  getTabClass(tab: string): string {
    return this.currentTab === tab ? 'tab-button active' : 'tab-button';
  }

  /**
   * Helpers para validação
   */
  getFieldError(fieldName: string, form: FormGroup): string | null {
    const control = form.get(fieldName);
    if (!control || !control.touched) return null;

    if (control.errors?.['required']) return 'Campo obrigatório';
    if (control.errors?.['invalidTelefone']) return 'Telefone deve ter 10 ou 11 dígitos';
    if (control.errors?.['invalidCep']) return 'CEP deve ter 8 dígitos';
    if (control.errors?.['minlength']) return 'Campo muito curto';
    if (control.errors?.['email']) return 'E-mail inválido';
    return null;
  }

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
   * Formata telefone
   */
  formatTelefone(value: string): string {
    const cleaned = value.replace(/\D/g, '');
    if (cleaned.length <= 2) return cleaned;
    if (cleaned.length <= 7) return `(${cleaned.slice(0, 2)}) ${cleaned.slice(2)}`;
    return `(${cleaned.slice(0, 2)}) ${cleaned.slice(2, 7)}-${cleaned.slice(7, 11)}`;
  }

  onTelefoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const valorFormatado = this.formatTelefone(input.value);
    input.value = valorFormatado;
    this.profileForm.get('telefone')?.setValue(valorFormatado, { emitEvent: false });
  }

  formatCep(value: string): string {
    const cleaned = value.replace(/\D/g, '').slice(0, 8);
    if (cleaned.length > 5) {
      return `${cleaned.slice(0, 5)}-${cleaned.slice(5)}`;
    }
    return cleaned;
  }

  onCepInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const valorFormatado = this.formatCep(input.value);
    input.value = valorFormatado;
    this.profileForm.get('cep')?.setValue(valorFormatado, { emitEvent: false });

    const cepLimpo = valorFormatado.replace(/\D/g, '');
    if (cepLimpo.length === 8 && cepLimpo !== this.ultimoCepConsultado) {
      this.buscarEnderecoPorCep(cepLimpo);
    }
  }

  private montarEnderecoCompleto(formData: any): string {
    const linhaPrincipal = `${formData.rua}, ${formData.numero}`;
    const linhaSecundaria = [formData.complemento, formData.bairro].filter(Boolean).join(', ');
    const cidadeUf = `${formData.cidade} - ${String(formData.estado || '').toUpperCase()}`;
    const cepLimpo = String(formData.cep || '').replace(/\D/g, '').slice(0, 8);
    const cepFormatado = cepLimpo.length > 5 ? `${cepLimpo.slice(0, 5)}-${cepLimpo.slice(5)}` : cepLimpo;

    return [linhaPrincipal, linhaSecundaria, cidadeUf, cepFormatado].filter(Boolean).join(', ');
  }

  private hidratarEnderecoLegado(): void {
    const possuiCamposSeparados = !!(this.usuario.rua || this.usuario.bairro || this.usuario.cidade || this.usuario.estado);
    if (possuiCamposSeparados || !this.usuario.endereco) {
      return;
    }

    const partes = this.usuario.endereco
      .split(',')
      .map(parte => parte.trim())
      .filter(Boolean);

    if (partes.length === 0) {
      return;
    }

    const primeiraParte = partes[0] || '';
    const ruaNumeroMatch = primeiraParte.match(/^(.*?)[\s,-]+(\d+[A-Za-z0-9-]*)$/);
    if (ruaNumeroMatch) {
      this.usuario.rua = ruaNumeroMatch[1].trim();
      this.usuario.numero = ruaNumeroMatch[2].trim();
    } else {
      this.usuario.rua = primeiraParte;
    }

    if (!this.usuario.bairro && partes.length > 1) {
      this.usuario.bairro = partes[1];
    }

    const cidadeUfParte = partes.find(parte => parte.includes(' - '));
    if (cidadeUfParte) {
      const [cidade, uf] = cidadeUfParte.split(' - ');
      this.usuario.cidade = this.usuario.cidade || (cidade || '').trim();
      this.usuario.estado = this.usuario.estado || (uf || '').trim().toUpperCase();
    }

    const cepParte = partes.find(parte => /\d{5}-?\d{3}/.test(parte));
    if (cepParte) {
      const cepLimpo = cepParte.replace(/\D/g, '').slice(0, 8);
      this.usuario.cep = cepLimpo.length > 5 ? `${cepLimpo.slice(0, 5)}-${cepLimpo.slice(5)}` : cepLimpo;
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
              this.profileForm.get(item.campo)?.setValue(item.valor);
            }
          });

          this.errorMessage = null;
        },
        error: () => {
          this.errorMessage = 'Não foi possível consultar o CEP agora.';
        }
      });
  }

  /**
   * Formata data para exibição
   */
  formatData(data: string): string {
    if (!data) {
      return '';
    }

    const isoMatch = data.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (isoMatch) {
      const [, ano, mes, dia] = isoMatch;
      return `${dia}/${mes}/${ano}`;
    }

    const date = new Date(data);
    if (Number.isNaN(date.getTime())) {
      return data;
    }

    return date.toLocaleDateString('pt-BR');
  }

  /**
   * Retorna a cor da estrela baseada na nota
   */
  getStarColor(nota: number): string {
    if (nota >= 4.5) return '#ffd700';
    if (nota >= 3.5) return '#ffb700';
    return '#ff9800';
  }

  /**
   * Retorna o texto da avaliação
   */
  getAvaliacaoTexto(nota: number): string {
    if (nota >= 4.5) return 'Excelente';
    if (nota >= 3.5) return 'Bom';
    if (nota >= 2.5) return 'Satisfatório';
    return 'Precisa melhorar';
  }
}
