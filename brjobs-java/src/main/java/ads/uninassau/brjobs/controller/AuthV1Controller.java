package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.CadastroContratanteDTO;
import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.dto.AuthResponseDTO;
import ads.uninassau.brjobs.exception.EmailNotConfirmedException;
import ads.uninassau.brjobs.exception.SocialOnlyAccountException;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.service.AuthService;
import ads.uninassau.brjobs.service.AuthSessionService;
import ads.uninassau.brjobs.service.SocialAuthService;
import ads.uninassau.brjobs.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthV1Controller {

    private final AuthSessionService authSessionService;
    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            AuthSessionService.SessionResult session = authSessionService.login(loginRequest, request);
            authSessionService.writeSessionCookies(response, session);
            return ResponseEntity.ok(session.usuario());
        } catch (EmailNotConfirmedException e) {
            // Credenciais corretas, mas e-mail não confirmado: o front usa o code para
            // mostrar a opção de reenviar o e-mail de confirmação.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "code", "EMAIL_NOT_CONFIRMED",
                            "email", loginRequest.getEmail()
                    ));
        } catch (SocialOnlyAccountException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "E-mail ou senha invalidos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CadastroContratanteDTO cadastroRequest,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        UsuarioDTO criado = usuarioService.criarContratante(cadastroRequest);
        Usuario usuario = usuarioRepository.findById(criado.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario recem-criado nao encontrado"));
        AuthSessionService.SessionResult session = authSessionService.issueSocialSession(usuario, request, "local-register");
        authSessionService.writeSessionCookies(response, session);
        log.info("auth_register_success userId={} provider=local", usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(session.usuario());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = authSessionService.readCookie(request, AuthSessionService.REFRESH_COOKIE);
            AuthSessionService.SessionResult session = authSessionService.refresh(refreshToken, request);
            authSessionService.writeSessionCookies(response, session);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            authSessionService.clearSessionCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sessao expirada."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authSessionService.readCookie(request, AuthSessionService.REFRESH_COOKIE);
        authSessionService.logout(refreshToken, request);
        authSessionService.clearSessionCookies(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/social/google")
    public ResponseEntity<?> google(@RequestBody Map<String, String> body,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String token = firstNonBlank(
                body.get("idToken"),
                body.get("credential"),
                body.get("accessToken"),
                body.get("token")
        );
        log.info(
                "google_social_login_request bodyKeys={} authHeaderPresent={}",
                body.keySet(),
                request.getHeader("Authorization") != null
        );
        try {
            AuthResponseDTO social = socialAuthService.loginComGoogle(token);
            return socialSessionResponse(social, "google", request, response);
        } catch (Exception ex) {
            String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "Falha ao autenticar com google.";
            log.warn("social_login_failed provider=google reason={}", message);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", message));
        }
    }

    @PostMapping("/social/facebook")
    public ResponseEntity<?> facebook(@RequestBody Map<String, String> body,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String token = body.getOrDefault("accessToken", body.get("token"));
        AuthResponseDTO social = socialAuthService.loginComFacebook(token);
        return socialSessionResponse(social, "facebook", request, response);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(authService.obterUsuarioAutenticado(authentication.getName()));
    }

    private ResponseEntity<?> socialSessionResponse(AuthResponseDTO social,
                                                    String provider,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        if (social == null || social.getError() != null || social.getEmail() == null) {
            String message = social != null && social.getError() != null
                    ? social.getError()
                    : "Falha ao autenticar com " + provider + ".";
            log.warn("social_login_failed provider={} reason={}", provider, message);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", message));
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(social.getEmail())
                .orElseThrow(() -> new IllegalStateException("Usuario social nao encontrado"));
        AuthSessionService.SessionResult session = authSessionService.issueSocialSession(usuario, request, provider);
        authSessionService.writeSessionCookies(response, session);
        return ResponseEntity.ok(session.usuario());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
