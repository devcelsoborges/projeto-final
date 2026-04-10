import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Avaliacao {
  id: number;
  nota: number;
  comentario: string;
  usuarioId: number;
  prestadorId: number;
  dataCriacao: string;
}

export interface CreateAvaliacaoDTO {
  prestadorId: number;
  nota: number;
  comentario: string;
}

@Injectable({
  providedIn: 'root'
})
export class AvaliacaoService {
  private apiUrl = '/api/v1/avaliacoes';

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

  atualizar(id: number, avaliacao: Partial<CreateAvaliacaoDTO>): Observable<Avaliacao> {
    return this.http.put<Avaliacao>(`${this.apiUrl}/${id}`, avaliacao);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
