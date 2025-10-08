import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-register',
  standalone: true,
  // Importa ReactiveFormsModule para o tratamento de formulários
  imports: [CommonModule, ReactiveFormsModule], 
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  // Define o grupo de formulário
  registerForm!: FormGroup;
  
  // Lista de tipos de usuário e gêneros para os <select> no HTML
  userTypes = ['Candidato', 'Empresa'];
  genders = ['Feminino', 'Masculino', 'Outro', 'Prefiro não informar'];

  // Injeta o FormBuilder para construir o formulário
  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    // Inicializa o formulário com todos os campos e validadores especificados
    this.registerForm = this.fb.group({
      // CAMPOS DE AUTENTICAÇÃO (MANTIDOS)
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(6)]],
      confirmarSenha: ['', Validators.required],
      
      // CAMPOS OBRIGATÓRIOS DO USUÁRIO
      tipoUsuario: ['', Validators.required],
      nome: ['', Validators.required],
      telefone: ['', Validators.required],
      endereco: ['', Validators.required],
      cpf: ['', [Validators.required, Validators.minLength(11), Validators.maxLength(11)]],
      dataNascimento: ['', Validators.required],
      genero: ['', Validators.required],
      
      // CAMPOS OPCIONAIS / DE CONTEÚDO
      funcao: [''],
      experienciaProfissional: [''],
      especialidades: [''],
      
      // CAMPOS DE ARQUIVO (Iniciam como null, tratados separadamente)
      fotoPerfil: [null], 
      curriculo: [null]
    }, { 
      // Adiciona um validador personalizado a nível de grupo para verificar se as senhas coincidem
      validators: this.passwordMatchValidator 
    });
  }

  // Validador personalizado para checar se a senha e a confirmação coincidem
  passwordMatchValidator(form: FormGroup) {
    const senha = form.get('senha')?.value;
    const confirmarSenha = form.get('confirmarSenha')?.value;
    
    return senha && confirmarSenha && senha !== confirmarSenha ? { mismatch: true } : null;
  }

  // Método auxiliar para lidar com a seleção de arquivos
  onFileChange(event: any, fieldName: 'fotoPerfil' | 'curriculo'): void {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];
      // Define o objeto File no controle do formulário.
      this.registerForm.get(fieldName)?.setValue(file);
      this.registerForm.get(fieldName)?.updateValueAndValidity();
    }
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      console.log('Dados do Cadastro Válidos:', this.registerForm.value);
      // TODO: Em uma aplicação real, você enviaria os dados do formulário e os arquivos (separadamente) para o servidor
      
      // Exemplo de como você acessaria os arquivos:
      const foto = this.registerForm.get('fotoPerfil')?.value;
      const curriculo = this.registerForm.get('curriculo')?.value;
      console.log('Arquivo de Foto:', foto);
      console.log('Arquivo de Currículo:', curriculo);

      // Substitua alert() por um modal em produção
      alert('Cadastro completo e válido! (Simulação de envio)'); 
      this.registerForm.reset();
    } else {
      console.error('Formulário Inválido. Verifique todos os campos obrigatórios.');
      this.markAllAsTouched(this.registerForm);
    }
  }

  // Função auxiliar para marcar todos os campos como 'touched' e exibir erros
  private markAllAsTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markAllAsTouched(control);
      }
    });
  }
}
