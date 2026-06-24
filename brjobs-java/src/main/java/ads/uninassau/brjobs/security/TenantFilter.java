package ads.uninassau.brjobs.security;

import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter que extrai tenant_id do JWT token e o disponibiliza para toda a cadeia de requisição.
 *
 * Fluxo:
 * 1. Extrair Authorization header
 * 2. Validar token JWT (signature, expiration)
 * 3. Extrair email do token (future: user_id claim será adicionado)
 * 4. Armazenar em request.setAttribute("tenant_id", userId)
 * 5. Continuar pipeline
 *
 * Se token inválido → return 401 Unauthorized
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. Extrair JWT do header Authorization ou do cookie HttpOnly ACCESS_TOKEN.
        String token = getJwtFromRequest(request);

        if (token != null) {
            // 2. Validar token (signature + expiration)
            if (jwtTokenService.validateToken(token)) {
                try {
                    // 3. Extrair email do token
                    String email = jwtTokenService.getUsernameFromToken(token);

                    // 4. Resolver ID real do usuário para uso como tenant_id
                    //    (case-insensitive: coerente com a unificação de contas por e-mail)
                    Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário do token não encontrado"));

                    request.setAttribute("tenant_id", usuario.getId());
                    request.setAttribute("user_email", email);

                    log.debug("TenantFilter: tenant_id='{}' extraído com sucesso", usuario.getId());

                } catch (Exception e) {
                    log.warn("TenantFilter: Erro ao extrair claims do token: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                    return;
                }
            } else {
                log.warn("TenantFilter: Token JWT inválido ou expirado");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado");
                return;
            }
        } else {
            // Não há token → pode ser rota pública
            log.trace("TenantFilter: Sem Authorization header (possível rota pública)");
        }

        // Continuar pipeline. Exceções de negócio devem subir para os handlers globais.
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Rotas públicas que não requerem validação de tenant
        String path = request.getRequestURI();
        boolean publicacaoPublicaGet = "GET".equalsIgnoreCase(request.getMethod())
            && path.startsWith("/api/v1/publicacoes")
            && !path.startsWith("/api/v1/publicacoes/minhas");
        return path.startsWith("/api/auth/") ||     // login tradicional e social-login
               path.startsWith("/api/v1/auth/") ||  // login/register/refresh v1
               publicacaoPublicaGet ||               // listagem pública de publicações
               path.startsWith("/swagger-ui") ||      // Swagger UI
               path.startsWith("/api-docs") ||        // OpenAPI docs
               path.equals("/health");                // Health check
    }
}
