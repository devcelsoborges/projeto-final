import { Component, EventEmitter, Input, Output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { AvaliacaoService } from "../../service/avaliacao.service";

@Component({
  selector: "app-rating",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="rating-container">
      <h3>Avaliar usuário</h3>

      <div class="form-group">
        <label>Nota (1-5 estrelas)</label>
        <div class="stars" aria-label="Seleção de nota">
          <span
            *ngFor="let i of [1,2,3,4,5]"
            (click)="selecionarNota(i)"
            [class.ativa]="i <= nota"
            class="star"
            role="button"
            tabindex="0"
          >
            ★
          </span>
        </div>
        <p *ngIf="nota > 0" class="nota-texto">{{ nota }} estrela{{ nota > 1 ? "s" : "" }}</p>
      </div>

      <div class="form-group">
        <label>Comentário</label>
        <textarea
          [(ngModel)]="comentario"
          placeholder="Compartilhe sua experiência (máx. 200 caracteres)"
          maxlength="200"
          rows="4"
          class="form-control"
        ></textarea>
        <small>{{ comentario.length }}/200</small>
      </div>

      <div class="actions">
        <button
          (click)="enviarAvaliacao()"
          [disabled]="!podeEnviar()"
          class="btn btn-primary"
        >
          {{ enviando ? "Enviando..." : "Enviar avaliação" }}
        </button>
        <button (click)="cancelar()" class="btn btn-outline">Cancelar</button>
      </div>

      <div *ngIf="sucesso" class="alert alert-success">
        Avaliação enviada com sucesso. Obrigado pelo feedback.
      </div>
      <div *ngIf="erro" class="alert alert-danger">
        {{ erro }}
      </div>
    </div>
  `,
  styles: [`
    .rating-container {
      background: color-mix(in srgb, var(--color-accent-500) 7%, var(--color-surface) 93%);
      border: 1px solid color-mix(in srgb, var(--color-accent-500) 34%, var(--color-border) 66%);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-1);
      max-width: 460px;
      padding: 20px;
    }

    .rating-container h3 {
      color: var(--color-text);
      margin: 0 0 20px;
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      color: var(--color-text);
      display: block;
      font-weight: 700;
      margin-bottom: 8px;
    }

    .stars {
      display: flex;
      gap: 8px;
    }

    .star {
      color: #b86a05;
      cursor: pointer;
      filter: grayscale(1);
      font-size: 28px;
      line-height: 1;
      opacity: 0.45;
      transition: opacity 0.2s, transform 0.2s, filter 0.2s;
    }

    .star:hover {
      filter: grayscale(0.25);
      opacity: 0.85;
      transform: translateY(-1px);
    }

    .star.ativa {
      filter: grayscale(0);
      opacity: 1;
    }

    .nota-texto {
      color: var(--color-text-muted);
      font-size: 14px;
      margin: 8px 0 0;
    }

    .form-control {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      color: var(--color-text);
      font-family: inherit;
      font-size: 14px;
      padding: 10px;
      width: 100%;
    }

    .form-control:focus {
      border-color: var(--color-accent-500);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-accent-500) 22%, transparent);
      outline: none;
    }

    .form-group small {
      color: var(--color-text-muted);
      display: block;
      font-size: 12px;
      margin-top: 4px;
      text-align: right;
    }

    .actions {
      display: flex;
      gap: 10px;
    }

    .btn {
      border: 0;
      border-radius: var(--radius-sm);
      cursor: pointer;
      flex: 1;
      font-weight: 700;
      padding: 10px;
    }

    .btn-primary {
      background: var(--color-accent-500);
      color: #1b1b1b;
    }

    .btn-primary:hover:not(:disabled) {
      background: color-mix(in srgb, var(--color-accent-500) 84%, #000 16%);
    }

    .btn-primary:disabled {
      background: var(--color-border);
      color: var(--color-text-muted);
      cursor: not-allowed;
    }

    .btn-outline {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      color: var(--color-text);
    }

    .btn-outline:hover {
      background: var(--color-surface-muted);
    }

    .alert {
      border-radius: var(--radius-sm);
      font-size: 14px;
      margin-top: 15px;
      padding: 10px 12px;
    }

    .alert-success {
      background: color-mix(in srgb, var(--color-success) 12%, var(--color-surface) 88%);
      border: 1px solid color-mix(in srgb, var(--color-success) 35%, var(--color-border) 65%);
      color: var(--color-success);
    }

    .alert-danger {
      background: color-mix(in srgb, var(--color-danger) 12%, var(--color-surface) 88%);
      border: 1px solid color-mix(in srgb, var(--color-danger) 35%, var(--color-border) 65%);
      color: var(--color-danger);
    }
  `]
})
export class RatingComponent {
  @Input() prestadorId = 0;
  @Input() usuarioAvaliadoId = 0;
  @Output() avaliacaoEnviada = new EventEmitter<void>();

  nota = 0;
  comentario = "";
  enviando = false;
  sucesso = false;
  erro = "";

  constructor(private avaliacaoService: AvaliacaoService) {}

  selecionarNota(novaNota: number): void {
    this.nota = this.nota === novaNota ? 0 : novaNota;
    this.erro = "";
  }

  podeEnviar(): boolean {
    return this.nota > 0 && this.comentario.trim().length > 0 && !this.enviando;
  }

  enviarAvaliacao(): void {
    if (!this.podeEnviar()) return;

    this.enviando = true;
    this.erro = "";
    this.sucesso = false;

    const comentario = this.comentario.trim();

    this.avaliacaoService.criar({
      prestadorId: this.prestadorId,
      usuarioAvaliadoId: this.usuarioAvaliadoId || undefined,
      nota: this.nota,
      comentario
    }).subscribe({
      next: () => {
        this.sucesso = true;
        this.nota = 0;
        this.comentario = "";
        this.enviando = false;
        setTimeout(() => {
          this.sucesso = false;
          this.avaliacaoEnviada.emit();
        }, 2000);
      },
      error: (err) => {
        this.erro = err.error?.message || "Erro ao enviar avaliação";
        this.enviando = false;
      }
    });
  }

  cancelar(): void {
    this.nota = 0;
    this.comentario = "";
    this.erro = "";
    this.sucesso = false;
  }
}
