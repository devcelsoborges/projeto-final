import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule] // necessário para *ngIf e formulários
})
export class RegisterComponent {
  registerForm: FormGroup;
  fotoPerfilFile: File | null = null;
  curriculoFile: File | null = null;
  mensagemSucesso: string = '';
  mensagemErro: string = '';

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.registerForm = this.fb.group({
      tipoUsuario: ['trabalhador', Validators.required],
      nome: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', Validators.required],
      telefone: [''],
      endereco: [''],
      cpf: [''],
      funcao: [''],
      dataNascimento: [''],
      genero: [''],
      experienciaProfissional: [''],
      especialidades: ['']
    });
  }

  onFotoPerfilChange(event: any) {
    const file = event.target.files[0];
    if (file) this.fotoPerfilFile = file;
  }

  onCurriculoChange(event: any) {
    const file = event.target.files[0];
    if (file) this.curriculoFile = file;
  }

  submit() {
    if (this.registerForm.invalid) {
      this.mensagemErro = 'Please fill in all required fields.';
      return;
    }

    const formData = new FormData();
    Object.keys(this.registerForm.value).forEach(key => {
      formData.append(key, this.registerForm.value[key]);
    });

    if (this.fotoPerfilFile) formData.append('fotoPerfil', this.fotoPerfilFile);
    if (this.curriculoFile) formData.append('curriculo', this.curriculoFile);

    this.http.post('http://localhost:8080/api/usuarios', formData, {
      reportProgress: true,
      observe: 'events'
    }).subscribe({
      next: (event: any) => {
        if (event.type === HttpEventType.Response) {
          this.mensagemSucesso = 'Registration successful!';
          this.registerForm.reset();
          this.fotoPerfilFile = null;
          this.curriculoFile = null;
          this.mensagemErro = '';
        }
      },
      error: (err) => {
        this.mensagemErro = 'Error: ' + (err.error.message || err.message);
      }
    });
  }
}
