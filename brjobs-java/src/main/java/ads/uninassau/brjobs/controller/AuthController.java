package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.AuthResponseDTO;
import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.LoginResponseDTO;
import ads.uninassau.brjobs.dto.PasswordResetConfirmDTO;
import ads.uninassau.brjobs.dto.PasswordResetRequestDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.AccountActivationService;
import ads.uninassau.brjobs.service.AuthService;
import ads.uninassau.brjobs.service.SocialAuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller responsável por operações de autenticação.
 * NÃO é responsável pelo registro de usuários (usar UsuarioController).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SocialAuthService socialAuthService;

    @Autowired
    private AccountActivationService accountActivationService;

    /**
     * Endpoint para login e obtenção do token JWT.
     * POST /api/auth/login
     *
     * @param loginRequest contém email e senha
     * @return 200 OK com token JWT ou 401 UNAUTHORIZED
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            // Autenticar e gerar token
            String token = authService.authenticateAndGetToken(loginRequest);
            return ResponseEntity.ok(new LoginResponseDTO(token));

        } catch (AuthenticationException e) {
            // Credenciais inválidas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO(null, "Email ou senha inválidos."));
        }
    }

    /**
     * Endpoint para obter dados do usuário autenticado.
     * GET /api/auth/me
     * Requer token JWT no header Authorization
     *
     * @return 200 OK com dados do usuário
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            UsuarioDTO usuario = authService.obterUsuarioAutenticado(email);
            return ResponseEntity.ok(usuario);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Endpoint para validar se o token JWT é válido.
     * GET /api/auth/validate
     *
     * @return 200 OK se válido, 401 UNAUTHORIZED se inválido
     */
    @GetMapping("/validate")
    public ResponseEntity<Void> validarToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Endpoint para logout (opcional, principalmente para frontend)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        authService.solicitarRecuperacaoSenha(request.getEmail());
        // Resposta sempre genérica: não revela se o e-mail existe (anti-enumeração).
        return ResponseEntity.ok(Map.of(
                "message", "Se o e-mail existir, enviamos um link para redefinir a senha."
        ));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetConfirmDTO request) {
        authService.redefinirSenhaComToken(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    /**
     * Confirma o cadastro a partir do token do e-mail (template account-activation).
     * POST /api/auth/confirmar-email  Body: { "token": "..." }
     */
    @PostMapping("/confirmar-email")
    public ResponseEntity<Map<String, String>> confirmarEmail(@RequestBody Map<String, String> body) {
        boolean confirmado = accountActivationService.confirmar(body.get("token"));
        if (!confirmado) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Link de confirmação inválido ou expirado."));
        }
        return ResponseEntity.ok(Map.of("message", "Cadastro confirmado com sucesso."));
    }

    /**
     * Reenvia o e-mail de confirmação de cadastro.
     * POST /api/auth/reenviar-confirmacao  Body: { "email": "..." }
     */
    @PostMapping("/reenviar-confirmacao")
    public ResponseEntity<Map<String, String>> reenviarConfirmacao(@RequestBody Map<String, String> body) {
        accountActivationService.reenviarPorEmail(body.get("email"));
        // Resposta sempre genérica (anti-enumeração).
        return ResponseEntity.ok(Map.of(
                "message", "Se houver uma conta pendente para este e-mail, reenviamos a confirmação."
        ));
    }

    /**
     * Endpoint para login social via Google
     * POST /api/auth/social-login/google
     *
     * @param request contém o token do Google (credential JWT)
     * @return 200 OK com token JWT do BRJobs
     */
    @PostMapping("/social-login/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
        return autenticarSocial("google", request.get("token"));
    }

    /**
     * Endpoint para login social via Facebook
     * POST /api/auth/social-login/facebook
     *
     * @param request contém o token do Facebook
     * @return 200 OK com token JWT do BRJobs
     */
    @PostMapping("/social-login/facebook")
    public ResponseEntity<?> loginWithFacebook(@RequestBody Map<String, String> request) {
        return autenticarSocial("facebook", request.get("token"));
    }

    /**
     * Fluxo legado de login social (retorna JWT no corpo). Delega ao
     * {@link SocialAuthService}, que valida o token no provedor e vincula pelo
     * e-mail à mesma conta usada no cadastro local e nos demais provedores,
     * evitando contas duplicadas.
     */
    private ResponseEntity<?> autenticarSocial(String provider, String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Token do " + provider + " é obrigatório"));
        }

        try {
            AuthResponseDTO social = "google".equals(provider)
                    ? socialAuthService.loginComGoogle(token)
                    : socialAuthService.loginComFacebook(token);

            if (social == null || social.getError() != null || social.getEmail() == null) {
                String reason = social != null && social.getError() != null
                        ? social.getError()
                        : "Token do " + provider + " inválido";
                log.warn("social_login_legacy_failed provider={} reason={}", provider, reason);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", reason));
            }

            String token2 = authService.generateTokenForEmail(social.getEmail());
            log.info("social_login_legacy_success provider={} usuarioId={}", provider, social.getUsuarioId());
            return ResponseEntity.ok(Map.of(
                    "accessToken", token2,
                    "usuarioId", social.getUsuarioId(),
                    "email", social.getEmail(),
                    "nome", social.getNome() != null ? social.getNome() : ""
            ));
        } catch (Exception e) {
            String reason = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Falha ao autenticar com " + provider + ".";
            log.warn("social_login_legacy_failed provider={} reason={}", provider, reason);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", reason));
        }
    }
}
