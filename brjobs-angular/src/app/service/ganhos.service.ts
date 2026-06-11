import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface BreakdownCategoria {
  categoria: string;
  total: number;
  numServicos: number;
}

export interface BreakdownCliente {
  clienteId: number;
  clienteNome: string;
  total: number;
  numServicos: number;
}

export interface RelatorioGanhos {
  mes: string;
  totalFaturado: number;
  numServicos: number;
  porCategoria: BreakdownCategoria[];
  porCliente: BreakdownCliente[];
}

@Injectable({
  providedIn: 'root'
})
export class GanhosService {
  private apiUrl = `${environment.apiUrl}/api/v1/ganhos`;

  constructor(private http: HttpClient) {}

  gerar(ano: number, mes: number): Observable<RelatorioGanhos> {
    return this.http.get<RelatorioGanhos>(`${this.apiUrl}?ano=${ano}&mes=${mes}`);
  }

  gerarCorrente(): Observable<RelatorioGanhos> {
    return this.http.get<RelatorioGanhos>(`${this.apiUrl}/corrente`);
  }
}
