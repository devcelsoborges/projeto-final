import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private router: Router) { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const requestWithCookies = this.shouldUseCredentials(request)
      ? request.clone({
          withCredentials: true,
          setHeaders: this.csrfHeaders(request)
        })
      : request;

    return next.handle(requestWithCookies).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !this.isPublicEndpoint(requestWithCookies)) {
          this.clearLegacyAuth();
          this.router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
  }

  private shouldUseCredentials(request: HttpRequest<unknown>): boolean {
    return request.url.includes('/api/');
  }

  private csrfHeaders(request: HttpRequest<unknown>): Record<string, string> {
    const method = request.method.toUpperCase();
    if (method === 'GET' || method === 'HEAD' || method === 'OPTIONS') {
      return {};
    }

    const token = this.readCookie('XSRF-TOKEN') || sessionStorage.getItem('XSRF-TOKEN');
    return token ? { 'X-XSRF-TOKEN': token } : {};
  }

  private readCookie(name: string): string | null {
    const prefix = `${name}=`;
    const value = document.cookie
      .split(';')
      .map((cookie) => cookie.trim())
      .find((cookie) => cookie.startsWith(prefix));

    return value ? decodeURIComponent(value.substring(prefix.length)) : null;
  }

  private isPublicEndpoint(request: HttpRequest<unknown>): boolean {
    const url = request.url;
    const method = request.method.toUpperCase();
    const isReadOnly = method === 'GET' || method === 'OPTIONS';

    if (url.startsWith('http://') || url.startsWith('https://')) {
      const isExternal = !url.includes('/api/');
      if (isExternal) {
        return true;
      }
    }

    if (url.includes('/api/v1/auth/login') ||
        url.includes('/api/v1/auth/refresh') ||
        url.includes('/api/v1/auth/social/google') ||
        url.includes('/api/v1/auth/social/facebook') ||
        url.includes('/api/usuarios/contratante') ||
        url.includes('/api/usuarios/prestador')) {
      return true;
    }

    if (url.includes('/api/v1/publicacoes')) {
      return isReadOnly && !url.includes('/api/v1/publicacoes/minhas');
    }

    if (url.includes('/api/highlight/plans')) {
      return isReadOnly;
    }

    if (isReadOnly && (
      url.includes('/api/usuarios/') ||
      url.includes('/api/prestadores/usuario/') ||
      url.includes('/api/avaliacoes/prestador/') ||
      url.includes('/api/avaliacoes/v1/prestador/') ||
      url.includes('/api/avaliacoes/usuario/') ||
      url.includes('/api/avaliacoes/v1/usuario/')
    )) {
      return true;
    }

    return false;
  }

  private clearLegacyAuth(): void {
    [
      'auth_token',
      'token',
      'app_token',
      'refresh_token',
      'refreshToken'
    ].forEach((key) => localStorage.removeItem(key));
  }
}
