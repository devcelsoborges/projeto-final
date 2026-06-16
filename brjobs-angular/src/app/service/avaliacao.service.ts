import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface Avaliacao {
  id: number;
  nota: number;
  comentario: string;
  usuarioId: number;
  usuarioAvaliadoId?: number;
  prestadorId: number;
  dataCriacao: string;
}

export interface CreateAvaliacaoDTO {
  prestadorId: number;
  usuarioAvaliadoId?: number;
  nota: number;
  comentario: string;
}

@Injectable({
  providedIn: 'root'
})
export class AvaliacaoService {
  private apiUrl = `${environment.apiUrl.replace('/v1', '')}/avaliacoes`;

  constructor(private http: HttpClient) {}

  criar(avaliacao: CreateAvaliacaoDTO): Observable<Avaliacao> {
    return this.http.post<Avaliacao>(`${this.apiUrl}/v1`, avaliacao);
  }

  listarRecebidas(): Observable<Avaliacao[]> {
    return this.http.get<Avaliacao[]>(`${this.apiUrl}/v1/recebidas`);
  }

  obterStats(prestadorId: number): Observable<{ media_avaliacao: number; total_avaliacoes: number }> {
    return this.http.get<{ media_avaliacao: number; total_avaliacoes: number }>(
      `${this.apiUrl}/v1/prestador/${prestadorId}/stats`
    );
  }

  obterPorId(id: number): Observable<Avaliacao> {
    return this.http.get<Avaliacao>(`${this.apiUrl}/${id}`);
  }

  listarPorPrestador(prestadorId: number): Observable<Avaliacao[]> {
    return this.http.get<Avaliacao[]>(`${this.apiUrl}/prestador/${prestadorId}`);
  }

  listarPorUsuario(usuarioId: number): Observable<Avaliacao[]> {
    return this.http.get<Avaliacao[]>(`${this.apiUrl}/usuario/${usuarioId}`);
  }

  listarRecebidasPorUsuario(usuarioId: number): Observable<Avaliacao[]> {
    return this.http.get<Avaliacao[]>(`${this.apiUrl}/usuario/${usuarioId}/recebidas`);
  }

  obterStatsUsuario(usuarioId: number): Observable<{ media_avaliacao: number; total_avaliacoes: number }> {
    return this.http.get<{ media_avaliacao: number; total_avaliacoes: number }>(
      `${this.apiUrl}/v1/usuario/${usuarioId}/stats`
    );
  }

  atualizar(id: number, avaliacao: Partial<CreateAvaliacaoDTO>): Observable<Avaliacao> {
    return this.http.put<Avaliacao>(`${this.apiUrl}/${id}`, avaliacao);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
