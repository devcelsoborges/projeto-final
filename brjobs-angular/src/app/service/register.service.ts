import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { environment } from '../environments/environment';

interface CadastroContratanteDTO {
  nome: string;
  email: string;
  senha: string;
  telefone: string;
  dataNascimento: string;
  cpf: string;
  genero: string;
  endereco: string;
  cep?: string;
  rua?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  numero?: string;
  complemento?: string;
  bio?: string;
}

interface CadastroPrestadorDTO {
  nome: string;
  email: string;
  senha: string;
  telefone: string;
  dataNascimento: string;
  cpf: string;
  genero: string;
  endereco: string;
  cep?: string;
  rua?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  numero?: string;
  complemento?: string;
  bio?: string;
  funcao: string;
  experienciaProfissional?: string;
  especialidades?: string;
}

interface UsuarioDTO {
  id: number;
  nome: string;
  email: string;
  telefone: string;
  cpf: string;
  genero: string;
  dataNascimento: string;
  endereco: string;
  cep?: string;
  rua?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  numero?: string;
  complemento?: string;
  bio?: string;
  tipoUsuario: string;
  ativo: boolean;
  dataCadastro?: string;
}

interface ApiResponse {
  success: boolean;
  message: string;
  data?: UsuarioDTO;
}

@Injectable({
  providedIn: 'root'
})
export class RegisterService {
  private apiUrl = `${environment.apiUrl}/api/usuarios`;

  constructor(private http: HttpClient) { }

  /**
   * Registra um novo usuário contratante
   */
  registrarContratante(dados: CadastroContratanteDTO): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.apiUrl}/contratante`, dados)
      .pipe(
        tap(response => {
}),
        catchError(error => {
return throwError(() => this.handleError(error));
        })
      );
  }

  /**
   * Registra um novo usuário prestador de serviço
   */
  registrarPrestador(dados: CadastroPrestadorDTO): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.apiUrl}/prestador`, dados)
      .pipe(
        tap(response => {
}),
        catchError(error => {
return throwError(() => this.handleError(error));
        })
      );
  }

  /**
   * Verifica se o email já está registrado
   */
  verificarEmailExistente(email: string): Observable<boolean> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/email/${email}`)
      .pipe(
        map(() => true), // Se sucesso, email existe
        catchError(error => {
          // Se retornar 404, o email não existe
          if (error.status === 404) {
            return of(false);
          }
          return throwError(() => error);
        })
      );
  }

  /**
   * Tratamento centralizado de erros
   */
  private handleError(error: any): ApiResponse {
    let message = 'Erro ao processar requisição';

    if (error.status === 400) {
      message = error.error?.message || 'Dados inválidos. Verifique os campos.';
    } else if (error.status === 409) {
      message = 'Email já registrado no sistema.';
    } else if (error.status === 500) {
      message = 'Erro no servidor. Tente novamente mais tarde.';
    } else if (error.error?.message) {
      message = error.error.message;
    }

    return {
      success: false,
      message: message
    };
  }
}
