import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { environment } from '../environments/environment';

interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface SocialAuthResponse {
  id: number;
  usuarioId?: number;
  email: string;
  nome: string;
  tipoUsuario?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SocialLoginService {
  private apiUrl = `${environment.apiUrl}/api/v1/auth/social`;
  private googleClientId = environment.oauth.google.clientId;
  private facebookAppId = environment.oauth.facebook.appId;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadSocialScripts();
  }

  /**
   * Carrega scripts de terceiros para OAuth
   */
  private loadSocialScripts(): void {
    // Google Sign-In
    if (!window.document.getElementById('google-script')) {
      const script = window.document.createElement('script');
      script.id = 'google-script';
      script.src = 'https://accounts.google.com/gsi/client';
      script.onload = () => {
// @ts-ignore
        if (window.google && window.google.accounts) {
}
      };
      script.onerror = () => {
};
      window.document.head.appendChild(script);
    }

    // Facebook SDK
    if (!window.document.getElementById('facebook-sdk')) {
      const script = window.document.createElement('script');
      script.id = 'facebook-sdk';
      script.innerHTML = `
        window.fbAsyncInit = function() {
          FB.init({
            appId: '${this.facebookAppId}',
            xfbml: true,
            version: 'v18.0'
          });
};
      `;
      window.document.head.appendChild(script);

      const fbScript = window.document.createElement('script');
      fbScript.src = 'https://connect.facebook.net/pt_BR/sdk.js';
      fbScript.async = true;
      fbScript.defer = true;
      fbScript.crossOrigin = 'anonymous';
      fbScript.onerror = () => {
};
      window.document.body.appendChild(fbScript);
    }
}

  /**
   * Login via Google
   */
  loginWithGoogle(token: string): Observable<SocialAuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post<SocialAuthResponse>(
        `${this.apiUrl}/google`,
        { token },
        { headers: headers.set(csrf.headerName || 'X-XSRF-TOKEN', csrf.token), withCredentials: true }
      ))
    );
  }

  /**
   * Login via Facebook
   */
  loginWithFacebook(token: string): Observable<SocialAuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post<SocialAuthResponse>(
        `${this.apiUrl}/facebook`,
        { token },
        { headers: headers.set(csrf.headerName || 'X-XSRF-TOKEN', csrf.token), withCredentials: true }
      ))
    );
  }

  private ensureCsrf(): Observable<CsrfResponse> {
    return this.http.get<CsrfResponse>(`${environment.apiUrl}/api/v1/auth/csrf`, { withCredentials: true }).pipe(
      switchMap((csrf) => {
        if (csrf?.token) {
          sessionStorage.setItem('XSRF-TOKEN', csrf.token);
        }
        return [csrf];
      })
    );
  }

  /**
   * Inicia o fluxo de login do Google
   */
  initiateGoogleSignIn(
    callback: (credential: string) => void,
    onError?: (message: string) => void,
    onCancel?: () => void
  ): void {
    // @ts-ignore
    if (!window.google || !window.google.accounts) {
      const message = 'Google Sign-In SDK nao carregado.';
if (onError) {
        onError(message);
      }
      return;
    }

    // Fluxo principal: OAuth2 popup (mais confiável para clique explícito).
    // @ts-ignore
    if (window.google.accounts.oauth2) {
      try {
        // @ts-ignore
        const tokenClient = window.google.accounts.oauth2.initTokenClient({
          client_id: this.googleClientId,
          scope: 'openid email profile',
          callback: (response: any) => {
            if (response?.access_token) {
              callback(response.access_token);
              return;
            }

            const description = response?.error_description || response?.error || 'sem detalhes';
            const message = `Google OAuth nao retornou access token: ${description}`;
if (onError) {
              onError(message);
            }
          },
          error_callback: (error: any) => {
            const detail = error?.message || error?.type || 'erro desconhecido';

            // Fechamento voluntário do popup não deve gerar erro visual.
            const normalizedDetail = String(detail).toLowerCase();
            if (
              normalizedDetail.includes('popup window closed') ||
              normalizedDetail.includes('popup_closed') ||
              normalizedDetail.includes('popup closed') ||
              normalizedDetail.includes('user closed')
            ) {
              if (onCancel) {
                onCancel();
              }
              return;
            }

            const message = `Falha no popup do Google OAuth: ${detail}`;
if (onError) {
              onError(message);
            }
          }
        });

        tokenClient.requestAccessToken({ prompt: 'consent' });
        return;
      } catch (error) {
}
    }

    // @ts-ignore
    window.google.accounts.id.initialize({
      client_id: this.googleClientId,
      callback: (response: any) => {
        if (response?.credential) {
          callback(response.credential);
          return;
        }

        const message = 'Google nao retornou credencial.';
if (onError) {
          onError(message);
        }
      },
      error_callback: () => {
        const message = 'Erro ao inicializar o Google Sign-In.';
if (onError) {
          onError(message);
        }
      }
    });

    try {
      // @ts-ignore
      window.google.accounts.id.prompt((notification: any) => {
        if (notification?.isNotDisplayed?.()) {
          const reason = notification.getNotDisplayedReason?.() || 'motivo desconhecido';
          const message = `Google Sign-In nao foi exibido neste navegador: ${reason}`;
if (onError) {
            onError(message);
          }
        }

        if (notification?.isSkippedMoment?.()) {
          const reason = notification.getSkippedReason?.() || 'motivo desconhecido';
          const message = `Google Sign-In foi ignorado ou bloqueado: ${reason}`;
if (onError) {
            onError(message);
          }
        }
      });
    } catch (error) {
      const message = 'Falha ao abrir o prompt do Google Sign-In.';
if (onError) {
        onError(message);
      }
    }
  }

  /**
   * Inicia o fluxo de login do Facebook
   */
  initiateFacebookSignIn(callback: (token: string) => void): void {
    // @ts-ignore
    if (window.FB) {
      // @ts-ignore
      window.FB.login((response: any) => {
        if (response.authResponse) {
          const token = response.authResponse.accessToken;
          callback(token);
        }
      }, { scope: 'public_profile,email' });
    }
  }

  /**
   * Decodifica JWT do Google para extrair informações do usuário
   */
  decodeGoogleToken(token: string): any {
    try {
      // JWT tem 3 partes separadas por ponto: header.payload.signature
      const parts = token.split('.');
      if (parts.length !== 3) {
return null;
      }

      // Decodificar payload (segunda parte)
      const payload = parts[1];
      // Adicionar padding se necessário
      const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
      const decoded = atob(padded);
return JSON.parse(decoded);
    } catch (error) {
return null;
    }
  }

  /**
   * Armazena tokens e dados do usuário após autenticação social
   */
  storeSocialAuthTokens(response: SocialAuthResponse): void {
    const usuarioId = response.id ?? response.usuarioId;
    localStorage.removeItem('auth_token');
    localStorage.removeItem('app_token');
    localStorage.removeItem('token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('refreshToken');
    localStorage.setItem('usuario_id', String(usuarioId ?? ''));
    localStorage.setItem('usuario_email', response.email);
    localStorage.setItem('usuario_nome', response.nome);
  }

  /**
   * Faz logout social
   */
  socialLogout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('app_token');
    localStorage.removeItem('token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('usuario_id');
    localStorage.removeItem('usuario_email');
    localStorage.removeItem('usuario_nome');
    localStorage.removeItem('usuario_telefone');
    localStorage.removeItem('usuario_cpf');
    localStorage.removeItem('usuario_genero');
    localStorage.removeItem('usuario_dataNascimento');
    localStorage.removeItem('usuario_endereco');
    localStorage.removeItem('usuario_cep');
    localStorage.removeItem('usuario_rua');
    localStorage.removeItem('usuario_numero');
    localStorage.removeItem('usuario_complemento');
    localStorage.removeItem('usuario_bairro');
    localStorage.removeItem('usuario_cidade');
    localStorage.removeItem('usuario_estado');
    localStorage.removeItem('usuario_tipo');
    localStorage.removeItem('usuario_dataCadastro');
    this.router.navigate(['/login']);
  }
}

// Global type definitions
declare global {
  interface Window {
    google?: any;
    FB?: any;
  }
}
