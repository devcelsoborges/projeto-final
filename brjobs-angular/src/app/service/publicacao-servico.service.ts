import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../environments/environment";

export type TipoPublicacao = "PRESTACAO" | "CONTRATACAO";

export interface PublicacaoServico {
  id: number;
  tipoPublicacao: TipoPublicacao;
  titulo: string;
  descricao: string;
  categoria?: string;
  enderecoPublicacao?: string;
  cepPublicacao?: string;
  cidadePublicacao?: string;
  estadoPublicacao?: string;
  latitude?: number;
  longitude?: number;
  geocodeProvider?: string;
  geocodePrecision?: string;
  distanceKm?: number;
  usuarioBairro?: string;
  usuarioCidade?: string;
  usuarioEndereco?: string;
  preco?: number;
  orcamentoMin?: number;
  orcamentoMax?: number;
  status: string;
  usuarioId: number;
  usuarioNome: string;
  isHighlighted?: boolean;
  highlightExpiresAt?: string;
  highlightPlanId?: number;
  highlightPlanName?: string;
  highlightPriority?: number;
  dataCriacao: string;
  prestador?: {
    id: number;
    nome: string;
    avaliacaoMedia: number;
    numAvaliacoes: number;
    fotoUrl: string;
  };
}

export interface CriarPublicacaoServicoDTO {
  tipoPublicacao: TipoPublicacao;
  titulo: string;
  descricao: string;
  categoria?: string;
  enderecoPublicacao: string;
  cepPublicacao?: string;
  cidadePublicacao?: string;
  estadoPublicacao?: string;
  latitude?: number;
  longitude?: number;
  geocodeProvider?: string;
  geocodePrecision?: string;
  preco?: number;
  orcamentoMin?: number;
  orcamentoMax?: number;
}

export interface PublicacaoPage {
  content: PublicacaoServico[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: "root"
})
export class PublicacaoServicoService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/publicacoes`;

  constructor(private readonly http: HttpClient) {}

  listar(tipo?: TipoPublicacao): Observable<PublicacaoServico[]> {
    let url = this.apiUrl;
    if (tipo) {
      url += `?tipo=${encodeURIComponent(tipo)}`;
    }
    return this.http.get<PublicacaoServico[]>(url);
  }

  buscarPaginado(params: {
    tipo?: TipoPublicacao;
    termo?: string;
    page?: number;
    size?: number;
    lat?: number;
    lng?: number;
  }): Observable<PublicacaoPage> {
    const query: string[] = [];
    if (params.tipo) {
      query.push(`tipo=${encodeURIComponent(params.tipo)}`);
    }
    if (params.termo) {
      query.push(`termo=${encodeURIComponent(params.termo)}`);
    }
    query.push(`page=${params.page ?? 0}`);
    query.push(`size=${params.size ?? 20}`);
    if (params.lat != null && params.lng != null) {
      query.push(`lat=${encodeURIComponent(params.lat)}`);
      query.push(`lng=${encodeURIComponent(params.lng)}`);
    }

    return this.http.get<PublicacaoPage>(`${this.apiUrl}/paginado?${query.join("&")}`);
  }

  listarMinhas(): Observable<PublicacaoServico[]> {
    return this.http.get<PublicacaoServico[]>(`${this.apiUrl}/minhas`);
  }

  obterPorId(id: number): Observable<PublicacaoServico> {
    return this.http.get<PublicacaoServico>(`${this.apiUrl}/${id}`);
  }

  criar(payload: CriarPublicacaoServicoDTO): Observable<PublicacaoServico> {
    return this.http.post<PublicacaoServico>(this.apiUrl, payload);
  }

  encerrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
