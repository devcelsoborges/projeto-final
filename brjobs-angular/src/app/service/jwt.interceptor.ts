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
    const token = this.getStoredToken();

    if (token && !this.isPublicEndpoint(request)) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !this.isPublicEndpoint(request)) {
          this.clearStoredAuth();
          this.router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
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

    if (url.includes('/api/auth/login') ||
        url.includes('/api/auth/social-login') ||
        url.includes('/api/v1/auth/') ||
        url.includes('/api/usuarios/contratante') ||
        url.includes('/api/usuarios/prestador') ||
        url.includes('/api/auth/logout')) {
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

  private getStoredToken(): string | null {
    return localStorage.getItem('auth_token')
      || localStorage.getItem('token')
      || localStorage.getItem('app_token');
  }

  private clearStoredAuth(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('token');
    localStorage.removeItem('app_token');
  }
}
