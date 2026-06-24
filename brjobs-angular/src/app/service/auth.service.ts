import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, map, of, switchMap } from 'rxjs';
import { throwError } from 'rxjs';
import { environment } from '../environments/environment';
interface LoginRequest {
  email: string;
  senha: string;
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface UsuarioDTO {
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
  emailConfirmado?: boolean;
  dataCadastro?: string;
  fotoPerfil?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/v1/auth`;
  private tokenSubject = new BehaviorSubject<string | null>(null);
  public token$ = this.tokenSubject.asObservable();

  private usuarioSubject = new BehaviorSubject<UsuarioDTO | null>(null);
  public usuario$ = this.usuarioSubject.asObservable();

  private isLoggedInSubject = new BehaviorSubject<boolean>(false);
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  constructor(private http: HttpClient) {
    this.syncAuthStateFromStorage(true);
  }

  login(email: string, senha: string): Observable<UsuarioDTO> {
    const loginRequest: LoginRequest = { email, senha };

    return this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post<UsuarioDTO>(`${this.apiUrl}/login`, loginRequest, {
        withCredentials: true,
        headers: this.csrfHeaders(csrf)
      }))
    ).pipe(
      tap((usuario) => this.applyAuthenticatedUser(usuario)),
      catchError((error) => throwError(() => error))
    );
  }

  obterUsuarioAutenticado(silent = false): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/me`, { withCredentials: true })
      .pipe(
        tap((usuario) => this.applyAuthenticatedUser(usuario)),
        catchError(error => {
          this.clearAuthState(error?.status === 401 || error?.status === 403);
          return throwError(() => error);
        })
      );
  }

  validarToken(): Observable<void> {
    return this.obterUsuarioAutenticado().pipe(map(() => undefined));
  }

  refreshSession(): Observable<void> {
    return this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post<void>(`${this.apiUrl}/refresh`, {}, {
        withCredentials: true,
        headers: this.csrfHeaders(csrf)
      }))
    );
  }

  logout(): void {
    this.clearAuthState(true);

    this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post(`${this.apiUrl}/logout`, {}, {
        withCredentials: true,
        headers: this.csrfHeaders(csrf)
      }))
    ).subscribe({ error: () => undefined });
  }

  getStoredToken(): string | null {
    return null;
  }

  getToken(): string | null {
    return null;
  }

  isLoggedIn(): boolean {
    return this.isLoggedInSubject.value;
  }

  getUsuarioAtual(): UsuarioDTO | null {
    return this.usuarioSubject.value;
  }

  setUsuarioAtual(usuario: UsuarioDTO): void {
    this.applyAuthenticatedUser(usuario);
  }

  syncAuthStateFromStorage(loadUser = true): void {
    this.removeLegacyTokens();

    if (!loadUser) {
      this.hydrateUserFromStorage();
      return;
    }

    if (!this.hasStoredUserHint()) {
      this.clearAuthState(false);
      return;
    }

    this.obterUsuarioAutenticado(true).subscribe({
      error: (error) => {
        if (error?.status === 401 || error?.status === 403) {
          this.clearAuthState(true);
        } else {
          this.clearAuthState(false);
        }
      }
    });
  }

  markAuthenticated(usuario?: UsuarioDTO): void {
    if (usuario) {
      this.applyAuthenticatedUser(usuario);
      return;
    }

    this.obterUsuarioAutenticado().subscribe({
      error: () => this.clearAuthState(false)
    });
  }

  private ensureCsrf(): Observable<CsrfResponse> {
    return this.http.get<CsrfResponse>(`${this.apiUrl}/csrf`, { withCredentials: true }).pipe(
      tap((csrf) => {
        if (csrf?.token) {
          sessionStorage.setItem('XSRF-TOKEN', csrf.token);
        }
      })
    );
  }

  private csrfHeaders(csrf: CsrfResponse): HttpHeaders {
    return new HttpHeaders({ [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token });
  }

  private applyAuthenticatedUser(usuario: UsuarioDTO): void {
    this.removeLegacyTokens();
    this.usuarioSubject.next(usuario);
    this.isLoggedInSubject.next(true);
    this.persistUsuarioStorage(usuario);
  }

  private clearAuthState(clearStorage: boolean): void {
    this.removeLegacyTokens();
    this.tokenSubject.next(null);
    this.usuarioSubject.next(null);
    this.isLoggedInSubject.next(false);

    if (clearStorage) {
      this.clearUsuarioStorage();
    }
  }

  private hydrateUserFromStorage(): Observable<UsuarioDTO | null> {
    const id = localStorage.getItem('usuario_id');
    const email = localStorage.getItem('usuario_email');

    if (!id || !email) {
      this.clearAuthState(false);
      return of(null);
    }

    const usuario = {
      id: Number(id),
      nome: localStorage.getItem('usuario_nome') || '',
      email,
      telefone: localStorage.getItem('usuario_telefone') || '',
      cpf: localStorage.getItem('usuario_cpf') || '',
      genero: localStorage.getItem('usuario_genero') || '',
      dataNascimento: localStorage.getItem('usuario_dataNascimento') || '',
      endereco: localStorage.getItem('usuario_endereco') || '',
      cep: localStorage.getItem('usuario_cep') || '',
      rua: localStorage.getItem('usuario_rua') || '',
      bairro: localStorage.getItem('usuario_bairro') || '',
      cidade: localStorage.getItem('usuario_cidade') || '',
      estado: localStorage.getItem('usuario_estado') || '',
      numero: localStorage.getItem('usuario_numero') || '',
      complemento: localStorage.getItem('usuario_complemento') || '',
      bio: localStorage.getItem('usuario_bio') || '',
      tipoUsuario: localStorage.getItem('usuario_tipo') || '',
      ativo: true,
      // Tri-state: ausência da chave (conta legada) = undefined, não false.
      emailConfirmado: localStorage.getItem('usuario_emailConfirmado') === null
        ? undefined
        : localStorage.getItem('usuario_emailConfirmado') === 'true',
      dataCadastro: localStorage.getItem('usuario_dataCadastro') || undefined
    };

    this.usuarioSubject.next(usuario);
    this.isLoggedInSubject.next(true);
    return of(usuario);
  }

  private hasStoredUserHint(): boolean {
    return !!localStorage.getItem('usuario_id') && !!localStorage.getItem('usuario_email');
  }

  private persistUsuarioStorage(usuario: UsuarioDTO): void {
    localStorage.setItem('usuario_id', String(usuario.id ?? ''));
    localStorage.setItem('usuario_nome', usuario.nome || '');
    localStorage.setItem('usuario_email', usuario.email || '');
    localStorage.setItem('usuario_telefone', usuario.telefone || '');
    localStorage.setItem('usuario_cpf', usuario.cpf || '');
    localStorage.setItem('usuario_genero', usuario.genero || '');
    localStorage.setItem('usuario_dataNascimento', usuario.dataNascimento || '');
    localStorage.setItem('usuario_endereco', usuario.endereco || '');
    localStorage.setItem('usuario_cep', usuario.cep || '');
    localStorage.setItem('usuario_rua', usuario.rua || '');
    localStorage.setItem('usuario_bairro', usuario.bairro || '');
    localStorage.setItem('usuario_cidade', usuario.cidade || '');
    localStorage.setItem('usuario_estado', usuario.estado || '');
    localStorage.setItem('usuario_numero', usuario.numero || '');
    localStorage.setItem('usuario_complemento', usuario.complemento || '');
    localStorage.setItem('usuario_bio', usuario.bio || '');
    localStorage.setItem('usuario_tipo', usuario.tipoUsuario || '');
    if (usuario.emailConfirmado !== undefined && usuario.emailConfirmado !== null) {
      localStorage.setItem('usuario_emailConfirmado', String(usuario.emailConfirmado));
    }
    if (usuario.dataCadastro) {
      localStorage.setItem('usuario_dataCadastro', usuario.dataCadastro);
    }
  }

  private clearUsuarioStorage(): void {
    [
      'usuario_id',
      'usuario_nome',
      'usuario_email',
      'usuario_telefone',
      'usuario_cpf',
      'usuario_genero',
      'usuario_dataNascimento',
      'usuario_endereco',
      'usuario_cep',
      'usuario_rua',
      'usuario_numero',
      'usuario_complemento',
      'usuario_bairro',
      'usuario_cidade',
      'usuario_estado',
      'usuario_bio',
      'usuario_tipo',
      'usuario_emailConfirmado',
      'usuario_dataCadastro'
    ].forEach((key) => localStorage.removeItem(key));
  }

  private removeLegacyTokens(): void {
    [
      'auth_token',
      'token',
      'app_token',
      'refresh_token',
      'refreshToken'
    ].forEach((key) => localStorage.removeItem(key));
  }
}
