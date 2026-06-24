package ads.uninassau.brjobs.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garante que o filtro JWT autentica os endpoints que dependem do token (/me, /logout,
 * /validate) e pula apenas os públicos. Regressão: antes o filtro pulava todo o prefixo
 * /api/v1/auth/, deixando o GET /api/v1/auth/me sempre 401.
 */
@DisplayName("JwtAuthenticationFilter.shouldNotFilter")
class JwtAuthenticationFilterShouldNotFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    private HttpServletRequest req(String method, String uri) {
        HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
        Mockito.when(r.getMethod()).thenReturn(method);
        Mockito.when(r.getRequestURI()).thenReturn(uri);
        return r;
    }

    @Test
    @DisplayName("/me e /validate DEVEM passar pelo filtro (precisam do token)")
    void endpointsAutenticadosDevemSerFiltrados() {
        assertFalse(filter.shouldNotFilter(req("GET", "/api/v1/auth/me")));
        assertFalse(filter.shouldNotFilter(req("GET", "/api/auth/me")));
        assertFalse(filter.shouldNotFilter(req("GET", "/api/auth/validate")));
    }

    @Test
    @DisplayName("Endpoints públicos de auth (e logout) NÃO passam pelo filtro")
    void endpointsPublicosNaoDevemSerFiltrados() {
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/auth/login")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/auth/register")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/auth/refresh")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/auth/logout")));
        assertTrue(filter.shouldNotFilter(req("GET", "/api/v1/auth/csrf")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/auth/social/google")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/v1/auth/social/facebook")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/auth/login")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/auth/logout")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/auth/social-login/google")));
        assertTrue(filter.shouldNotFilter(req("POST", "/api/auth/forgot-password/request")));
    }

    @Test
    @DisplayName("OPTIONS (preflight CORS) nunca é filtrado")
    void optionsNuncaFiltrado() {
        assertTrue(filter.shouldNotFilter(req("OPTIONS", "/api/v1/auth/me")));
    }

    @Test
    @DisplayName("Rotas protegidas comuns passam pelo filtro")
    void rotasProtegidasSaoFiltradas() {
        assertFalse(filter.shouldNotFilter(req("GET", "/api/usuarios/me")));
        assertFalse(filter.shouldNotFilter(req("POST", "/api/publicacoes")));
    }
}
