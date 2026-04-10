import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SocialLoginDTO {
  provider: string;
  idToken?: string;
  accessToken?: string;
  code?: string;
}

export interface AuthResponseDTO {
  token: string;
  refreshToken: string;
  usuarioId: number;
  email: string;
  nome: string;
  error?: string;
}

/**
 * Serviço para autenticação social (Google, Facebook, Apple)
 * Gerencia tokens OAuth2 e comunica com backend
 */
@Injectable({
  providedIn: 'root'
})
export class SocialAuthService {
  private apiUrl = '/api/v1/auth/social';

  // Carrega scripts dos provedores OAuth2 (GRATUITO)
  private googleScriptLoaded = false;
  private facebookScriptLoaded = false;
  private appleScriptLoaded = false;

  constructor(private http: HttpClient) {
    this.carregarScripts();
  }

  /**
   * Carrega scripts dos provedores OAuth2
   */
  private carregarScripts() {
    // Google sign-in library
    if (!this.googleScriptLoaded) {
      const googleScript = document.createElement('script');
      googleScript.src = 'https://accounts.google.com/gapi/client:platform.js';
      googleScript.async = true;
      googleScript.defer = true;
      document.head.appendChild(googleScript);
      this.googleScriptLoaded = true;
    }

    // Facebook SDK
    if (!this.facebookScriptLoaded) {
      (window as any).fbAsyncInit = () => {
        FB.init({
          appId: 'SEU_FACEBOOK_APP_ID',
          xfbml: true,
          version: 'v18.0'
        });
      };

      const facebookScript = document.createElement('script');
      facebookScript.src = 'https://connect.facebook.net/pt_BR/sdk.js#xfbml=1&version=v18.0';
      facebookScript.async = true;
      facebookScript.defer = true;
      document.head.appendChild(facebookScript);
      this.facebookScriptLoaded = true;
    }

    // Apple Sign-In
    if (!this.appleScriptLoaded) {
      const appleScript = document.createElement('script');
      appleScript.src = 'https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid.js';
      appleScript.async = true;
      appleScript.defer = true;
      document.head.appendChild(appleScript);
      this.appleScriptLoaded = true;
    }
  }

  /**
   * Inicia login com Google (GRATUITO)
   * Abre popup do Google e retorna ID Token
   */
  async loginComGoogle(): Promise<AuthResponseDTO> {
    return new Promise((resolve, reject) => {
      // Aguard carregamento do Google script
      (window as any).google.accounts.id.initialize({
        client_id: 'SEU_GOOGLE_CLIENT_ID',
        callback: (response: any) => {
          // response.credential contém o ID Token do Google
          this.enviarTokenGoogle(response.credential).subscribe(
            (authResponse) => resolve(authResponse),
            (error) => reject(error)
          );
        }
      });

      // Abre o picker do Google
      (window as any).google.accounts.id.renderButton(
        document.getElementById('googleSignInButton'),
        { 
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          width: '100'
        }
      );
    });
  }

  /**
   * Enviar ID Token do Google para backend
   */
  enviarTokenGoogle(idToken: string): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/google`, { idToken });
  }

  /**
   * Login com Facebook (GRATUITO com app gratuita)
   */
  async loginComFacebook(): Promise<AuthResponseDTO> {
    return new Promise((resolve, reject) => {
      (window as any).FB.login((response: any) => {
        if (response.authResponse) {
          const accessToken = response.authResponse.accessToken;
          this.enviarTokenFacebook(accessToken).subscribe(
            (authResponse) => resolve(authResponse),
            (error) => reject(error)
          );
        } else {
          reject(new Error('Login Facebook cancelado'));
        }
      }, { scope: 'public_profile,email' });
    });
  }

  /**
   * Enviar Access Token do Facebook para backend
   */
  enviarTokenFacebook(accessToken: string): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/facebook`, { accessToken });
  }

  /**
   * Login com Apple (GRATUITO com Apple Developer Account)
   */
  async loginComApple(): Promise<AuthResponseDTO> {
    return new Promise((resolve, reject) => {
      (window as any).AppleID.auth.init({
        clientId: 'SEU_APPLE_CLIENT_ID',
        teamId: 'SEU_APPLE_TEAM_ID',
        keyId: 'SEU_APPLE_KEY_ID',
        redirectURI: 'http://localhost:4200/login/apple-callback',
        scope: AppleIDAuthorizationScopes.EMAIL + ' ' + AppleIDAuthorizationScopes.FULL_NAME,
        usePopup: true
      });

      (window as any).AppleID.auth.signIn().then((response: any) => {
        // response.id_token é o Identity Token
        this.enviarTokenApple(response.id_token, response.code).subscribe(
          (authResponse) => resolve(authResponse),
          (error) => reject(error)
        );
      }).catch((error: any) => {
        reject(error);
      });
    });
  }

  /**
   * Enviar Identity Token do Apple para backend
   */
  enviarTokenApple(identityToken: string, authorizationCode: string): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/apple`, {
      idToken: identityToken,
      code: authorizationCode
    });
  }

  /**
   * Desconectar conta social
   */
  desconectarSocial(provider: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${provider}`);
  }
}

/**
 * Escopos do Apple ID Auth
 */
enum AppleIDAuthorizationScopes {
  EMAIL = 'email',
  FULL_NAME = 'name'
}

// Declaração global (para evitar erro do TypeScript)
declare var FB: any;
