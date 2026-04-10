import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface SocialAuthResponse {
  accessToken: string;
  refreshToken?: string;
  usuarioId: number;
  email: string;
  nome: string;
  tipoUsuario?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SocialLoginService {
  private apiUrl = 'http://localhost:8080/api/auth/social-login';
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
        console.log('✅ Google Sign-In SDK carregado');
        // @ts-ignore
        if (window.google && window.google.accounts) {
          console.log('✅ Google accounts API disponível');
        }
      };
      script.onerror = () => {
        console.error('❌ Erro ao carregar Google Sign-In SDK');
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
          console.log('✅ Facebook SDK inicializado');
        };
      `;
      window.document.head.appendChild(script);

      const fbScript = window.document.createElement('script');
      fbScript.src = 'https://connect.facebook.net/pt_BR/sdk.js';
      fbScript.async = true;
      fbScript.defer = true;
      fbScript.crossOrigin = 'anonymous';
      fbScript.onerror = () => {
        console.error('❌ Erro ao carregar Facebook SDK');
      };
      window.document.body.appendChild(fbScript);
    }

    console.log('📱 OAuth Scripts carregados. Google Client ID:', 
      this.googleClientId === 'YOUR_GOOGLE_CLIENT_ID' ? '⚠️ NÃO CONFIGURADO' : '✅ Configurado');
  }

  /**
   * Login via Google
   */
  loginWithGoogle(token: string): Observable<SocialAuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<SocialAuthResponse>(
      `${this.apiUrl}/google`,
      { token },
      { headers }
    );
  }

  /**
   * Login via Facebook
   */
  loginWithFacebook(token: string): Observable<SocialAuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<SocialAuthResponse>(
      `${this.apiUrl}/facebook`,
      { token },
      { headers }
    );
  }

  /**
   * Login via Apple
   */
  loginWithApple(token: string, identityToken?: string): Observable<SocialAuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<SocialAuthResponse>(
      `${this.apiUrl}/apple`,
      { token, identityToken },
      { headers }
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
      console.error(message, 'googleClientId:', this.googleClientId);
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
            console.error(message, response);
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
            console.error(message, error);
            if (onError) {
              onError(message);
            }
          }
        });

        tokenClient.requestAccessToken({ prompt: 'consent' });
        return;
      } catch (error) {
        console.warn('Falha no fluxo OAuth2 popup. Tentando fallback ID token...', error);
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
        console.error(message, response);
        if (onError) {
          onError(message);
        }
      },
      error_callback: () => {
        const message = 'Erro ao inicializar o Google Sign-In.';
        console.error(message);
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
          console.warn(message, notification);
          if (onError) {
            onError(message);
          }
        }

        if (notification?.isSkippedMoment?.()) {
          const reason = notification.getSkippedReason?.() || 'motivo desconhecido';
          const message = `Google Sign-In foi ignorado ou bloqueado: ${reason}`;
          console.warn(message, notification);
          if (onError) {
            onError(message);
          }
        }
      });
    } catch (error) {
      const message = 'Falha ao abrir o prompt do Google Sign-In.';
      console.error(message, error);
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
        console.error('Token inválido');
        return null;
      }

      // Decodificar payload (segunda parte)
      const payload = parts[1];
      // Adicionar padding se necessário
      const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
      const decoded = atob(padded);
      
      console.log('[Google Token Decoded]', decoded);
      return JSON.parse(decoded);
    } catch (error) {
      console.error('Erro ao decodificar Google token:', error);
      return null;
    }
  }

  /**
   * Armazena tokens e dados do usuário após autenticação social
   */
  storeSocialAuthTokens(response: SocialAuthResponse): void {
    localStorage.setItem('auth_token', response.accessToken);
    localStorage.setItem('app_token', response.accessToken);
    if (response.refreshToken) {
      localStorage.setItem('refresh_token', response.refreshToken);
    }
    localStorage.setItem('usuario_id', response.usuarioId.toString());
    localStorage.setItem('usuario_email', response.email);
    localStorage.setItem('usuario_nome', response.nome);
  }

  /**
   * Faz logout social
   */
  socialLogout(): void {
    localStorage.removeItem('app_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('usuario_id');
    localStorage.removeItem('usuario_email');
    localStorage.removeItem('usuario_nome');
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
