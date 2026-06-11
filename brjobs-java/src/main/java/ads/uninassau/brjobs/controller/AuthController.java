package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.LoginResponseDTO;
import ads.uninassau.brjobs.dto.PasswordResetConfirmDTO;
import ads.uninassau.brjobs.dto.PasswordResetRequestDTO;
import ads.uninassau.brjobs.dto.PasswordResetVerifyDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

/**
 * Controller responsável por operações de autenticação.
 * NÃO é responsável pelo registro de usuários (usar UsuarioController).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.password-reset.expose-code:true}")
    private boolean exposeResetCode;

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
        String code = authService.solicitarRecuperacaoSenha(request.getEmail());

        if (exposeResetCode && code != null) {
            return ResponseEntity.ok(Map.of(
                    "message", "Código de recuperação gerado.",
                    "debugCode", code
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Se o e-mail existir, você receberá instruções para redefinir a senha."
        ));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<Map<String, String>> verifyPasswordResetCode(@Valid @RequestBody PasswordResetVerifyDTO request) {
        boolean valido = authService.verificarCodigoRecuperacao(request.getEmail(), request.getCode());

        if (!valido) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Código inválido ou expirado."));
        }

        return ResponseEntity.ok(Map.of("message", "Código verificado com sucesso."));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetConfirmDTO request) {
        authService.redefinirSenhaComCodigo(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
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
        String googleToken = request.get("token");

        if (googleToken == null || googleToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Google token é obrigatório"));
        }

        try {
            // Tentar primeiro como ID token (JWT). Se falhar, tratar como access token.
            Map<String, Object> googleData = decodeGoogleToken(googleToken);
            if (googleData == null) {
                googleData = fetchGoogleUserInfoFromAccessToken(googleToken);
            }

            if (googleData == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token do Google invalido"));
            }

            String email = (String) googleData.get("email");
            String nome = (String) googleData.get("name");
            String picture = (String) googleData.get("picture");

            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token Google sem e-mail"));
            }

            Usuario usuario = authService.findOrCreateSocialUser(email, nome);
            String token = authService.generateTokenForEmail(usuario.getEmail());

            // Log dos dados extraídos
            log.info("Google login bem-sucedido - Email: {}, Nome: {}", email, nome);

            return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "usuarioId", usuario.getId(),
                "email", usuario.getEmail(),
                "nome", usuario.getNome(),
                "fotoPerfil", picture != null ? picture : ""
            ));
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }

            String reason = (e.getMessage() != null && !e.getMessage().isBlank())
                    ? e.getMessage()
                    : (root.getMessage() != null && !root.getMessage().isBlank()
                        ? root.getMessage()
                        : root.getClass().getSimpleName());

            log.error("Erro ao autenticar com Google: {}", reason, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Falha ao autenticar com Google: " + reason));
        }
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
        String facebookToken = request.get("token");

        if (facebookToken == null || facebookToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Facebook token é obrigatório"));
        }

        try {
            // TODO: Chamar um serviço para validar o token do Facebook
            // e extrair email, nome, foto, etc.

            // Para desenvolvimento, retornar um token de teste
            return ResponseEntity.ok(Map.of(
                "accessToken", authService.generateDemoToken(),
                "usuarioId", 1L,
                "email", "demo@facebook.com",
                "nome", "Demo User"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Falha ao autenticar com Facebook: " + e.getMessage()));
        }
    }


    /**
     * Decodifica um JWT do Google para extrair informações do usuário
     * @param token JWT do Google (pode não ser validado contra Google API)
     * @return Map com dados do usuário ou null se falhar
     */
    private Map<String, Object> decodeGoogleToken(String token) {
        try {
            // Dividir o token em 3 partes: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("Token possui formato inválido");
                return null;
            }

            // Decodificar a payload (parte do meio) de base64
            String payload = parts[1];

            // Adicionar padding se necessário
            int padding = (4 - payload.length() % 4) % 4;
            for (int i = 0; i < padding; i++) {
                payload += "=";
            }

            // Decodificar de base64
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, "UTF-8");

            // Parse JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonPayload = mapper.readValue(decodedPayload, Map.class);

            log.debug("Google token decodificado com sucesso: {}", jsonPayload.get("email"));
            return jsonPayload;

        } catch (Exception e) {
            log.error("Erro ao decodificar Google token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Busca dados do usuário Google a partir de access token OAuth2.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchGoogleUserInfoFromAccessToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.error("Erro ao buscar userinfo do Google via access token: {}", e.getMessage());
            return null;
        }
    }
}
