import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-contato',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contato.component.html',
  styleUrls: ['./contato.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContatoComponent {
  form: FormGroup;
  enviado = false;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      assunto: ['', [Validators.required, Validators.minLength(5)]],
      mensagem: ['', [Validators.required, Validators.minLength(10)]],
    });
  }

  enviarFormulario(): void {
    if (this.form.valid) {
      this.enviado = true;
      this.form.reset();
      
      // Resetar a mensagem após 3 segundos
      setTimeout(() => {
        this.enviado = false;
      }, 3000);
    }
  }

  get nome() {
    return this.form.get('nome');
  }

  get email() {
    return this.form.get('email');
  }

  get assunto() {
    return this.form.get('assunto');
  }

  get mensagem() {
    return this.form.get('mensagem');
  }
}
