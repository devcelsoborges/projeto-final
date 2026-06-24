package ads.uninassau.brjobs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Torna esta classe um componente gerenciado pelo Spring
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // Pula apenas os endpoints públicos que EMITEM/renovam sessão ou não dependem do
        // access token. Endpoints como /me, /logout e /validate precisam que o filtro rode
        // para autenticar a partir do token (cookie ACCESS_TOKEN ou header Authorization) —
        // por isso NÃO entram nesta lista. (A regra anterior pulava todo o prefixo
        // /api/v1/auth/ e deixava o /me sempre 401.)
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/csrf")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/logout")
                || path.startsWith("/api/v1/auth/social/")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/logout")
                || path.startsWith("/api/auth/social-login/")
                || path.startsWith("/api/auth/forgot-password/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            // 1. Valida se o token existe e é válido
            if (jwt != null && tokenService.validateToken(jwt)) {
                try {
                    String username = tokenService.getUsernameFromToken(jwt);

                    // 2. Carrega os detalhes do usuário
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 3. Cria o objeto de autenticação para o Spring Security
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // Adiciona detalhes da requisição para auditoria
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 4. Define a autenticação no contexto de segurança (usuário autenticado!)
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Autenticação JWT definida com sucesso para usuário: " + username);
                } catch (Exception ex) {
                    logger.error("Erro ao extrair informações do token JWT ou carregar usuário.", ex);
                }
            } else if (jwt != null) {
                // Não logar fragmento do token (é credencial). Token expirado é situação normal.
                logger.debug("Requisição com token JWT ausente ou inválido.");
            }
        } catch (Exception ex) {
            // Logs de falha de autenticação (ex: token inválido)
            logger.error("Erro geral no filtro JWT de autenticação.", ex);
        }

        // Permite que a requisição siga para o próximo filtro/controller
        filterChain.doFilter(request, response);
    }

    // Método auxiliar para extrair o token do cabeçalho Authorization
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Verifica se o cabeçalho existe e começa com "Bearer "
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Retorna a string do token, excluindo "Bearer "
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
}
