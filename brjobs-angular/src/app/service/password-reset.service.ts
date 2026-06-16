import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

interface PasswordResetRequestResponse {
  message: string;
  debugCode?: string;
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

  requestCode(email: string): Observable<PasswordResetRequestResponse> {
    return this.http.post<PasswordResetRequestResponse>(`${this.apiUrl}/request`, { email });
  }

  verifyCode(email: string, code: string): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/verify`, { email, code });
  }

  resetPassword(email: string, code: string, newPassword: string): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/reset`, { email, code, newPassword });
  }
}
