import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GanhosService, RelatorioGanhos } from '../../service/ganhos.service';

@Component({
  selector: 'app-earnings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="earnings-container">
      <h2>Relatório de Ganhos</h2>

      <div class="mesano-selector">
        <select [(ngModel)]="mesSelecionado" (change)="carregarRelatorio()">
          <option value="corrente">Mês Atual</option>
          <option value="2026-04">Abril 2026</option>
          <option value="2026-03">Março 2026</option>
          <option value="2026-02">Fevereiro 2026</option>
          <option value="2026-01">Janeiro 2026</option>
        </select>
      </div>

      <div *ngIf="carregando" class="loading">Carregando...</div>

      <div *ngIf="!carregando && relatorio" class="relatorio">
        <div class="summary">
          <div class="summary-card">
            <h4>Total Faturado</h4>
            <p class="valor">R$ {{ relatorio.totalFaturado | number:'1.2-2' }}</p>
          </div>
          <div class="summary-card">
            <h4>Serviços Concluídos</h4>
            <p class="valor">{{ relatorio.numServicos }}</p>
          </div>
          <div class="summary-card">
            <h4>Ticket Médio</h4>
            <p class="valor">R$ {{ (relatorio.totalFaturado / (relatorio.numServicos || 1)) | number:'1.2-2' }}</p>
          </div>
        </div>

        <div class="breakdown-section">
          <h3>Ganhos por Categoria</h3>
          <div *ngIf="relatorio.porCategoria.length === 0" class="no-data">
            Sem dados para este período
          </div>
          <table *ngIf="relatorio.porCategoria.length > 0" class="breakdown-table">
            <thead>
              <tr>
                <th>Categoria</th>
                <th>Total</th>
                <th>Serviços</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of relatorio.porCategoria">
                <td>{{ item.categoria }}</td>
                <td>R$ {{ item.total | number:'1.2-2' }}</td>
                <td>{{ item.numServicos }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="breakdown-section">
          <h3>Ganhos por Cliente</h3>
          <div *ngIf="relatorio.porCliente.length === 0" class="no-data">
            Sem dados para este período
          </div>
          <table *ngIf="relatorio.porCliente.length > 0" class="breakdown-table">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Total</th>
                <th>Serviços</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of relatorio.porCliente">
                <td>{{ item.clienteNome }}</td>
                <td>R$ {{ item.total | number:'1.2-2' }}</td>
                <td>{{ item.numServicos }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div *ngIf="erro" class="alert alert-danger">
        {{ erro }}
      </div>
    </div>
  `,
  styles: [`
    .earnings-container {
      padding: 20px;
      max-width: 1000px;
      margin: 0 auto;
    }
    .earnings-container h2 {
      margin-bottom: 20px;
    }
    .mesano-selector {
      margin-bottom: 20px;
    }
    .mesano-selector select {
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
    }
    .loading {
      text-align: center;
      padding: 40px;
      color: #999;
    }
    .relatorio {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 15px;
      margin-bottom: 30px;
    }
    .summary-card {
      background: #f5f5f5;
      padding: 20px;
      border-radius: 8px;
      text-align: center;
    }
    .summary-card h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: #666;
    }
    .valor {
      margin: 0;
      font-size: 24px;
      font-weight: bold;
      color: #2196F3;
    }
    .breakdown-section {
      margin-bottom: 30px;
    }
    .breakdown-section h3 {
      margin: 0 0 15px;
      font-size: 16px;
    }
    .no-data {
      text-align: center;
      padding: 20px;
      color: #999;
      background: #f9f9f9;
      border-radius: 4px;
    }
    .breakdown-table {
      width: 100%;
      border-collapse: collapse;
    }
    .breakdown-table thead {
      background: #f5f5f5;
    }
    .breakdown-table th {
      padding: 12px;
      text-align: left;
      font-weight: 600;
      border-bottom: 2px solid #ddd;
    }
    .breakdown-table td {
      padding: 12px;
      border-bottom: 1px solid #eee;
    }
    .breakdown-table tbody tr:hover {
      background: #f9f9f9;
    }
    .alert {
      padding: 12px 15px;
      border-radius: 4px;
      margin-top: 15px;
    }
    .alert-danger {
      background: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }
  `]
})
export class EarningsComponent implements OnInit {
  relatorio: RelatorioGanhos | null = null;
  carregando = false;
  erro = '';
  mesSelecionado = 'corrente';

  constructor(private ganhosService: GanhosService) {}

  ngOnInit() {
    this.carregarRelatorio();
  }

  carregarRelatorio() {
    this.carregando = true;
    this.erro = '';

    if (this.mesSelecionado === 'corrente') {
      this.ganhosService.gerarCorrente().subscribe({
        next: (rel) => {
          this.relatorio = rel;
          this.carregando = false;
        },
        error: (err) => {
          this.erro = 'Erro ao carregar relatório de ganhos';
          this.carregando = false;
        }
      });
    } else {
      const [ano, mes] = this.mesSelecionado.split('-').map(Number);
      this.ganhosService.gerar(ano, mes).subscribe({
        next: (rel) => {
          this.relatorio = rel;
          this.carregando = false;
        },
        error: (err) => {
          this.erro = 'Erro ao carregar relatório de ganhos';
          this.carregando = false;
        }
      });
    }
  }
}
