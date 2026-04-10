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
import { AuthService } from '../service/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Obter o token do AuthService
    const token = this.authService.getToken();

    // Se houver token e a requisição não for para login/registro, adicionar header
    // OBS: /api/auth/me PRECISA do token!
    if (token && !this.isPublicEndpoint(request)) {
      console.debug('Adicionando Authorization header ao request:', request.url);
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    } else if (token) {
      console.debug('Requisição pública, token não será enviado:', request.url);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Se erro 401 (Unauthorized), fazer logout
        if (error.status === 401 && !this.isPublicEndpoint(request)) {
          console.error('Erro 401 - Token inválido ou expirado');
          this.authService.logout();
          this.router.navigate(['/login']);
        }

        return throwError(() => error);
      })
    );
  }

  /**
   * Verifica se a URL é um endpoint público que NÃO precisa de token
   * Endpoints que PRECISAM de token: /api/auth/me (obter dados do usuário)
   */
  private isPublicEndpoint(request: HttpRequest<unknown>): boolean {
    const url = request.url;
    const method = request.method.toUpperCase();
    const isReadOnly = method === 'GET' || method === 'OPTIONS';

    // APIs externas nunca devem receber o Authorization do sistema.
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

    // Publicações são públicas apenas em leitura.
    if (url.includes('/api/v1/publicacoes')) {
      return isReadOnly;
    }

    // Visualização pública de dados do autor/prestador e avaliações.
    if (isReadOnly && (
      url.includes('/api/usuarios/') ||
      url.includes('/api/prestadores/usuario/') ||
      url.includes('/api/avaliacoes/prestador/') ||
      url.includes('/api/avaliacoes/v1/prestador/')
    )) {
      return true;
    }

    return false;
  }
}
