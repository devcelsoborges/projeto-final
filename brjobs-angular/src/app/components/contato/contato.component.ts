import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ContatoService } from '../../service/contato.service';

@Component({
  selector: 'app-contato',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contato.component.html',
  styleUrls: ['./contato.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContatoComponent {
  private readonly contatoService = inject(ContatoService);

  form: FormGroup;
  enviado = false;
  enviando = false;
  erro: string | null = null;

  constructor(
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      assunto: ['', [Validators.required, Validators.minLength(5)]],
      mensagem: ['', [Validators.required, Validators.minLength(10)]],
    });
  }

  enviarFormulario(): void {
    if (this.form.invalid || this.enviando) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando = true;
    this.erro = null;
    this.cdr.markForCheck();

    this.contatoService.enviar(this.form.value).subscribe({
      next: () => {
        this.enviado = true;
        this.enviando = false;
        this.form.reset();
        this.cdr.markForCheck();

        // Resetar a mensagem de sucesso após alguns segundos
        setTimeout(() => {
          this.enviado = false;
          this.cdr.markForCheck();
        }, 5000);
      },
      error: (err) => {
        this.enviando = false;
        this.erro = err?.error?.message
          || 'Não foi possível enviar sua mensagem agora. Tente novamente em instantes.';
        this.cdr.markForCheck();
      },
    });
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
