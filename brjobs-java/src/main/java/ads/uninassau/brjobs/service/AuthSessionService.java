package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.EmailNotConfirmedException;
import ads.uninassau.brjobs.exception.SocialOnlyAccountException;
import ads.uninassau.brjobs.model.AuthAuditEvent;
import ads.uninassau.brjobs.model.AuthRefreshToken;
import ads.uninassau.brjobs.model.SocialLogin;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.AuthAuditEventRepository;
import ads.uninassau.brjobs.repository.AuthRefreshTokenRepository;
import ads.uninassau.brjobs.repository.SocialLoginRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.security.JwtTokenService;
import ads.uninassau.brjobs.validator.UsuarioValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthSessionService {

    public static final String ACCESS_COOKIE = "ACCESS_TOKEN";
    public static final String REFRESH_COOKIE = "REFRESH_TOKEN";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthAuditEventRepository auditEventRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.refresh-token-ttl:PT168H}")
    private Duration refreshTokenTtl;

    @Value("${auth.access-token-ttl:PT2H}")
    private Duration accessTokenTtl;

    @Value("${auth.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    @Transactional
    public SessionResult login(LoginRequestDTO request, HttpServletRequest servletRequest) {
        String email = UsuarioValidator.normalizarEmail(request.getEmail());

        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isSomenteLoginSocial)
                .ifPresent(usuario -> {
                    audit("auth_login_failed", usuario, servletRequest, "{\"provider\":\"local\",\"reason\":\"social_only_account\"}");
                    throw new SocialOnlyAccountException(mensagemContaSomenteSocial(usuario));
                });

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getSenha()));
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

            // Senha correta, mas e-mail ainda não confirmado: bloqueia (só quando explicitamente
            // false — contas antigas/sociais com null ou true passam normalmente).
            if (Boolean.FALSE.equals(usuario.getEmailConfirmado())) {
                audit("auth_login_failed", usuario, servletRequest, "{\"provider\":\"local\",\"reason\":\"email_not_confirmed\"}");
                throw new EmailNotConfirmedException("Confirme seu e-mail para acessar a conta.");
            }

            audit("auth_login_success", usuario, servletRequest, "{\"provider\":\"local\"}");
            return issueSession(usuario, servletRequest, UUID.randomUUID());
        } catch (AuthenticationException ex) {
            audit("auth_login_failed", null, servletRequest, "{\"provider\":\"local\"}");
            throw ex;
        }
    }

    /**
     * Mensagem orientando o dono de uma conta criada via login social a entrar
     * pelo provedor ou definir uma senha local (que vincula tudo à mesma conta).
     */
    private String mensagemContaSomenteSocial(Usuario usuario) {
        String provedores = socialLoginRepository.findByUsuario(usuario).stream()
                .map(SocialLogin::getProvider)
                .distinct()
                .map(p -> Character.toUpperCase(p.charAt(0)) + p.substring(1))
                .reduce((a, b) -> a + " ou " + b)
                .orElse("Google ou Facebook");
        return "Esta conta foi criada com login " + provedores + ". Entre com " + provedores
                + " ou defina uma senha pela opção 'Esqueci minha senha'.";
    }

    @Transactional
    public SessionResult issueSocialSession(Usuario usuario, HttpServletRequest request, String provider) {
        audit("auth_login_success", usuario, request, "{\"provider\":\"" + provider + "\"}");
        return issueSession(usuario, request, UUID.randomUUID());
    }

    @Transactional
    public SessionResult refresh(String rawRefreshToken, HttpServletRequest request) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            audit("auth_refresh_failed", null, request, "{\"reason\":\"missing\"}");
            throw new IllegalArgumentException("Refresh token ausente");
        }

        AuthRefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> {
                    audit("auth_refresh_failed", null, request, "{\"reason\":\"not_found\"}");
                    return new IllegalArgumentException("Refresh token invalido");
                });

        LocalDateTime now = LocalDateTime.now();
        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            audit("auth_refresh_reuse_detected", current.getUsuario(), request, "{\"familyId\":\"" + current.getFamilyId() + "\"}");
            throw new IllegalArgumentException("Refresh token reutilizado");
        }

        if (current.getExpiresAt().isBefore(now)) {
            current.setRevokedAt(now);
            refreshTokenRepository.save(current);
            audit("auth_refresh_failed", current.getUsuario(), request, "{\"reason\":\"expired\"}");
            throw new IllegalArgumentException("Refresh token expirado");
        }

        current.setRevokedAt(now);
        AuthRefreshToken next = createRefreshToken(current.getUsuario(), request, current.getFamilyId());
        current.setReplacedByToken(next);
        refreshTokenRepository.save(current);
        audit("auth_refresh_success", current.getUsuario(), request, "{\"familyId\":\"" + current.getFamilyId() + "\"}");
        return new SessionResult(jwtTokenService.generateToken(current.getUsuario().getEmail()), next.getTokenHash(), nextRawToken.get(), authService.obterUsuarioAutenticado(current.getUsuario().getEmail()));
    }

    private final ThreadLocal<String> nextRawToken = new ThreadLocal<>();

    private SessionResult issueSession(Usuario usuario, HttpServletRequest request, UUID familyId) {
        AuthRefreshToken refresh = createRefreshToken(usuario, request, familyId);
        return new SessionResult(jwtTokenService.generateToken(usuario.getEmail()), refresh.getTokenHash(), nextRawToken.get(), authService.obterUsuarioAutenticado(usuario.getEmail()));
    }

    private AuthRefreshToken createRefreshToken(Usuario usuario, HttpServletRequest request, UUID familyId) {
        String rawToken = randomToken();
        nextRawToken.set(rawToken);

        AuthRefreshToken refresh = new AuthRefreshToken();
        refresh.setUsuario(usuario);
        refresh.setTokenHash(hash(rawToken));
        refresh.setFamilyId(familyId);
        refresh.setExpiresAt(LocalDateTime.now().plus(refreshTokenTtl));
        refresh.setCreatedIp(clientIp(request));
        refresh.setUserAgentHash(hash(request.getHeader("User-Agent")));
        return refreshTokenRepository.save(refresh);
    }

    @Transactional
    public void logout(String rawRefreshToken, HttpServletRequest request) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
                audit("auth_logout", token.getUsuario(), request, "{}");
            });
        }
    }

    public void writeSessionCookies(HttpServletResponse response, SessionResult result) {
        addCookie(response, ACCESS_COOKIE, result.accessToken(), accessTokenTtl, "/");
        addCookie(response, REFRESH_COOKIE, result.refreshToken(), refreshTokenTtl, "/");
    }

    public void clearSessionCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, "", Duration.ZERO, "/");
        addCookie(response, REFRESH_COOKIE, "", Duration.ZERO, "/");
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        if (value == null) {
            value = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash", e);
        }
    }

    private void audit(String eventType, Usuario usuario, HttpServletRequest request, String metadata) {
        try {
            AuthAuditEvent event = new AuthAuditEvent();
            event.setEventType(eventType);
            event.setUsuario(usuario);
            event.setIp(clientIp(request));
            event.setUserAgentHash(hash(request.getHeader("User-Agent")));
            event.setMetadataJson(metadata);
            auditEventRepository.save(event);
            log.info("auth_event type={} userId={}", eventType, usuario != null ? usuario.getId() : null);
        } catch (Exception e) {
            log.warn("Failed to persist auth audit event type={}", eventType, e);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record SessionResult(String accessToken, String refreshTokenHash, String refreshToken, UsuarioDTO usuario) {
    }
}
