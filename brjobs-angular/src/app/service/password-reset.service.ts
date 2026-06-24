import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

interface PasswordResetRequestResponse {
  message: string;
}

interface ApiMessageResponse {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordResetService {
  private readonly apiUrl = `${environment.apiUrl}/api/auth/forgot-password`;

  constructor(private readonly http: HttpClient) {}

  /** Passo 1: solicita o e-mail com o link de redefinição. */
  requestCode(email: string): Observable<PasswordResetRequestResponse> {
    return this.http.post<PasswordResetRequestResponse>(`${this.apiUrl}/request`, { email });
  }

  /** Passo 2 (via link): redefine a senha usando o token do link. */
  resetPassword(token: string, newPassword: string): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/reset`, { token, newPassword });
  }
}
