import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface Servico {
  id: number;
  titulo: string;
  descricao: string;
  preco: number;
  categoria: string;
  dataCriacao: string;
  prestador: {
    id: number;
    nome: string;
    avaliacaoMedia: number;
    numAvaliacoes: number;
    fotoUrl: string;
  };
}

export interface CreateServicoDTO {
  titulo: string;
  descricao: string;
  categoria: string;
  preco: number;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoService {
  private apiUrl = `${environment.apiUrl}/api/v1/servicos`;

  constructor(private http: HttpClient) {}

  buscar(categoria?: string, search?: string, page: number = 1, size: number = 10, sort: string = 'recente'): Observable<any> {
    let url = this.apiUrl + '?page=' + page + '&size=' + size + '&sort=' + sort;
    if (categoria) url += '&categoria=' + encodeURIComponent(categoria);
    if (search) url += '&search=' + encodeURIComponent(search);
    return this.http.get<any>(url);
  }

  criar(servico: CreateServicoDTO): Observable<Servico> {
    return this.http.post<Servico>(this.apiUrl, servico);
  }

  atualizar(id: number, servico: CreateServicoDTO): Observable<Servico> {
    return this.http.put<Servico>(`${this.apiUrl}/${id}`, servico);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  listarTodos(): Observable<Servico[]> {
    return this.http.get<Servico[]>(`${this.apiUrl}/todos`);
  }

  obterPorId(id: number): Observable<Servico> {
    return this.http.get<Servico>(`${this.apiUrl}/${id}`);
  }
}
