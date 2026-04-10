import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError } from 'rxjs';
import { throwError } from 'rxjs';

interface LoginRequest {
  email: string;
  senha: string;
}

interface LoginResponse {
  token: string;
  message?: string;
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
  tipoUsuario: string;
  ativo: boolean;
  dataCadastro?: string;
  fotoPerfil?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private tokenSubject = new BehaviorSubject<string | null>(this.getStoredToken());
  public token$ = this.tokenSubject.asObservable();

  private usuarioSubject = new BehaviorSubject<UsuarioDTO | null>(null);
  public usuario$ = this.usuarioSubject.asObservable();

  private isLoggedInSubject = new BehaviorSubject<boolean>(!!this.getStoredToken());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  constructor(private http: HttpClient) {
    this.syncAuthStateFromStorage(true);
  }

  /**
   * Realiza login com email e senha
   */
  login(email: string, senha: string): Observable<LoginResponse> {
    const loginRequest: LoginRequest = { email, senha };
    
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, loginRequest)
      .pipe(
        tap(response => {
          if (response.token) {
            this.setToken(response.token);
            // Carregar dados do usuário após login bem-sucedido
            this.obterUsuarioAutenticado().subscribe();
          }
        }),
        catchError(error => {
          console.error('Erro ao fazer login:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Obtém os dados do usuário autenticado
   */
  obterUsuarioAutenticado(): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/me`)
      .pipe(
        tap(usuario => {
          this.usuarioSubject.next(usuario);
        }),
        catchError(error => {
          console.error('Erro ao obter usuário autenticado:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Valida se o token atual é válido
   */
  validarToken(): Observable<void> {
    return this.http.get<void>(`${this.apiUrl}/validate`)
      .pipe(
        catchError(error => {
          console.error('Token inválido:', error);
          this.logout();
          return throwError(() => error);
        })
      );
  }

  /**
   * Realiza logout e limpa dados armazenados
   */
  logout(): void {
    // Atualiza a UI imediatamente.
    this.clearToken();

    // Notifica o backend em segundo plano.
    this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
      error: (error) => {
        console.error('Erro ao fazer logout:', error);
      }
    });
  }

  /**
   * Define o token JWT e atualiza states
   */
  private setToken(token: string): void {
    localStorage.setItem('auth_token', token);
    this.tokenSubject.next(token);
    this.isLoggedInSubject.next(true);
  }

  /**
   * Obtém o token armazenado
   */
  getStoredToken(): string | null {
    return localStorage.getItem('auth_token')
      || localStorage.getItem('token')
      || localStorage.getItem('app_token');
  }

  /**
   * Retorna o token atual
   */
  getToken(): string | null {
    const tokenAtual = this.tokenSubject.value;
    if (tokenAtual) {
      return tokenAtual;
    }

    const tokenPersistido = this.getStoredToken();
    if (tokenPersistido) {
      this.tokenSubject.next(tokenPersistido);
      this.isLoggedInSubject.next(true);
    }

    return tokenPersistido;
  }

  /**
   * Limpa o token e atualiza states
   */
  private clearToken(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('token');
    localStorage.removeItem('app_token');
    this.tokenSubject.next(null);
    this.usuarioSubject.next(null);
    this.isLoggedInSubject.next(false);
  }

  /**
   * Verifica se o usuário está autenticado
   */
  isLoggedIn(): boolean {
    return !!this.getStoredToken();
  }

  /**
   * Obtém o usuário atual armazenado
   */
  getUsuarioAtual(): UsuarioDTO | null {
    return this.usuarioSubject.value;
  }

  /**
   * Define o usuário atual
   */
  setUsuarioAtual(usuario: UsuarioDTO): void {
    this.usuarioSubject.next(usuario);
  }

  /**
   * Sincroniza estado reativo com o token armazenado (útil após login social).
   */
  syncAuthStateFromStorage(loadUser = true): void {
    const token = this.getStoredToken();

    this.tokenSubject.next(token);
    this.isLoggedInSubject.next(!!token);

    if (!token) {
      this.usuarioSubject.next(null);
      return;
    }

    if (loadUser) {
      this.obterUsuarioAutenticado().subscribe({
        error: () => {
          // Evita quebrar UI quando /auth/me falha temporariamente.
        }
      });
    }
  }
}
