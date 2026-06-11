import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';

interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface AuthResponseDTO {
  id: number;
  usuarioId?: number;
  email: string;
  nome: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SocialAuthService {
  private apiUrl = '/api/v1/auth/social';
  private googleScriptLoaded = false;
  private facebookScriptLoaded = false;

  constructor(private http: HttpClient) {
    this.carregarScripts();
  }

  private carregarScripts(): void {
    if (!this.googleScriptLoaded) {
      const googleScript = document.createElement('script');
      googleScript.src = 'https://accounts.google.com/gsi/client';
      googleScript.async = true;
      googleScript.defer = true;
      document.head.appendChild(googleScript);
      this.googleScriptLoaded = true;
    }

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
  }

  async loginComGoogle(): Promise<AuthResponseDTO> {
    return new Promise((resolve, reject) => {
      (window as any).google.accounts.id.initialize({
        client_id: 'SEU_GOOGLE_CLIENT_ID',
        callback: (response: any) => {
          this.enviarTokenGoogle(response.credential).subscribe({
            next: resolve,
            error: reject
          });
        }
      });

      (window as any).google.accounts.id.prompt();
    });
  }

  enviarTokenGoogle(idToken: string): Observable<AuthResponseDTO> {
    return this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post<AuthResponseDTO>(`${this.apiUrl}/google`, { token: idToken }, {
        withCredentials: true,
        headers: { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token }
      }))
    );
  }

  async loginComFacebook(): Promise<AuthResponseDTO> {
    return new Promise((resolve, reject) => {
      (window as any).FB.login((response: any) => {
        if (response.authResponse) {
          this.enviarTokenFacebook(response.authResponse.accessToken).subscribe({
            next: resolve,
            error: reject
          });
        } else {
          reject(new Error('Login Facebook cancelado'));
        }
      }, { scope: 'public_profile,email' });
    });
  }

  enviarTokenFacebook(accessToken: string): Observable<AuthResponseDTO> {
    return this.ensureCsrf().pipe(
      switchMap((csrf) => this.http.post<AuthResponseDTO>(`${this.apiUrl}/facebook`, { token: accessToken }, {
        withCredentials: true,
        headers: { [csrf.headerName || 'X-XSRF-TOKEN']: csrf.token }
      }))
    );
  }

  desconectarSocial(provider: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${provider}`, { withCredentials: true });
  }

  private ensureCsrf(): Observable<CsrfResponse> {
    return this.http.get<CsrfResponse>('/api/v1/auth/csrf', { withCredentials: true }).pipe(
      switchMap((csrf) => {
        if (csrf?.token) {
          sessionStorage.setItem('XSRF-TOKEN', csrf.token);
        }
        return [csrf];
      })
    );
  }
}

declare var FB: any;
