import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvaliacaoService } from '../../service/avaliacao.service';

@Component({
  selector: 'app-rating',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="rating-container">
      <h3>Avaliar Prestador</h3>
      
      <div class="form-group">
        <label>Nota (1-5 estrelas):</label>
        <div class="stars">
          <span *ngFor="let i of [1,2,3,4,5]" 
                (click)="selecionarNota(i)"
                [class.ativa]="i <= nota"
                class="star">
            ⭐
          </span>
        </div>
        <p *ngIf="nota > 0" class="nota-texto">{{ nota }} estrela{{ nota > 1 ? 's' : '' }}</p>
      </div>

      <div class="form-group">
        <label>Comentário:</label>
        <textarea [(ngModel)]="comentario" 
                  placeholder="Compartilhe sua experiência (máx 500 caracteres)"
                  maxlength="500"
                  rows="4"
                  class="form-control"></textarea>
        <small>{{ comentario.length }}/500</small>
      </div>

      <div class="actions">
        <button (click)="enviarAvaliacao()" 
                [disabled]="!podeEnviar()"
                class="btn btn-primary">
          {{ enviando ? 'Enviando...' : 'Enviar Avaliação' }}
        </button>
        <button (click)="cancelar()" class="btn btn-outline">Cancelar</button>
      </div>

      <div *ngIf="sucesso" class="alert alert-success">
        Avaliação enviada com sucesso! Obrigado pelo feedback.
      </div>
      <div *ngIf="erro" class="alert alert-danger">
        {{ erro }}
      </div>
    </div>
  `,
  styles: [`
    .rating-container {
      background: #f9f9f9;
      padding: 20px;
      border-radius: 8px;
      max-width: 400px;
    }
    .rating-container h3 {
      margin: 0 0 20px;
    }
    .form-group {
      margin-bottom: 20px;
    }
    .form-group label {
      display: block;
      font-weight: 500;
      margin-bottom: 8px;
    }
    .stars {
      display: flex;
      gap: 8px;
    }
    .star {
      font-size: 24px;
      cursor: pointer;
      opacity: 0.3;
      transition: opacity 0.2s;
    }
    .star:hover {
      opacity: 0.6;
    }
    .star.ativa {
      opacity: 1;
    }
    .nota-texto {
      margin: 8px 0 0;
      font-size: 14px;
      color: #666;
    }
    .form-control {
      width: 100%;
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-family: inherit;
      font-size: 14px;
    }
    .form-control:focus {
      outline: none;
      border-color: #2196F3;
      box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
    }
    .form-group small {
      display: block;
      font-size: 12px;
      color: #999;
      margin-top: 4px;
      text-align: right;
    }
    .actions {
      display: flex;
      gap: 10px;
    }
    .btn {
      flex: 1;
      padding: 10px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
    }
    .btn-primary {
      background: #2196F3;
      color: white;
    }
    .btn-primary:hover:not(:disabled) {
      background: #1976D2;
    }
    .btn-primary:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
    .btn-outline {
      border: 1px solid #ddd;
      background: white;
      color: #333;
    }
    .btn-outline:hover {
      background: #f5f5f5;
    }
    .alert {
      padding: 10px 12px;
      border-radius: 4px;
      margin-top: 15px;
      font-size: 14px;
    }
    .alert-success {
      background: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }
    .alert-danger {
      background: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }
  `]
})
export class RatingComponent {
  @Input() prestadorId: number = 0;
  @Output() avaliacaoEnviada = new EventEmitter<void>();
  
  nota = 0;
  comentario = '';
  enviando = false;
  sucesso = false;
  erro = '';

  constructor(private avaliacaoService: AvaliacaoService) {}

  selecionarNota(novaNota: number) {
    this.nota = this.nota === novaNota ? 0 : novaNota;
    this.erro = '';
  }

  podeEnviar(): boolean {
    return this.nota > 0 && this.comentario.trim().length > 0 && !this.enviando;
  }

  enviarAvaliacao() {
    if (!this.podeEnviar()) return;

    this.aviando = true;
    this.erro = '';
    this.sucesso = false;

    this.avaliacaoService.criar({
      prestadorId: this.prestadorId,
      nota: this.nota,
      comentario: this.comentario
    }).subscribe({
      next: () => {
        this.sucesso = true;
        this.nota = 0;
        this.comentario = '';
        this.enviando = false;
        setTimeout(() => {
          this.sucesso = false;
          this.avaliacaoEnviada.emit();
        }, 2000);
      },
      error: (err) => {
        this.erro = err.error?.message || 'Erro ao enviar avaliação';
        this.enviando = false;
      }
    });
  }

  cancelar() {
    this.nota = 0;
    this.comentario = '';
    this.erro = '';
    this.sucesso = false;
  }

  private aviando: boolean = false;
}
