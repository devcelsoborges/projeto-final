package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.AuthResponseDTO;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.model.SocialLogin;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.repository.SocialLoginRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UsuarioRepository usuarioRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${oauth2.google.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthResponseDTO loginComGoogle(String token) {
        String tokenType = googleTokenType(token);
        try {
            GoogleProfile profile = carregarPerfilGoogle(token, tokenType);
            String email = profile.email();
            String providerId = profile.providerId();
            String nome = profile.nome();
            Boolean emailVerified = profile.emailVerified();

            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new IllegalArgumentException("Google payload com email nao verificado");
            }

            Usuario usuario = buscarOuCriarUsuario(email, nome, "google");

            socialLoginRepository.findByProviderAndProviderId("google", providerId)
                .orElseGet(() -> {
                    SocialLogin novo = SocialLogin.builder()
                        .usuario(usuario)
                        .provider("google")
                        .providerId(providerId)
                        .email(email)
                        .nome(nome)
                        .emailVerified(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                    return socialLoginRepository.save(novo);
                });

            return AuthResponseDTO.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .build();

        } catch (Exception ex) {
            String reason = googleFailureReason(ex);
            log.warn(
                    "google_social_login_failed reason={} tokenType={} clientIdConfigured={} tokenFingerprint={}",
                    reason,
                    tokenType,
                    googleClientIdConfigurado(),
                    tokenFingerprint(token)
            );
            throw new IllegalArgumentException(reason, ex);
        }
    }

    @Transactional
    public AuthResponseDTO loginComFacebook(String accessToken) {
        try {
            String facebookUserUrl = "https://graph.facebook.com/v18.0/me?fields=id,name,email,verified&access_token=" + accessToken;
            String response = restTemplate.getForObject(facebookUserUrl, String.class);
            JsonNode userData = objectMapper.readTree(response);

            String providerId = userData.get("id").asText();
            String email = userData.get("email").asText();
            String nome = userData.get("name").asText();
            boolean emailVerified = !userData.has("verified") || userData.get("verified").asBoolean(false);

            if (!emailVerified) {
                throw new IllegalArgumentException("Facebook payload com email nao verificado");
            }

            Usuario usuario = buscarOuCriarUsuario(email, nome, "facebook");
            
            socialLoginRepository.findByProviderAndProviderId("facebook", providerId)
                .orElseGet(() -> {
                    SocialLogin novo = SocialLogin.builder()
                        .usuario(usuario)
                        .provider("facebook")
                        .providerId(providerId)
                        .email(email)
                        .nome(nome)
                        .emailVerified(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                    return socialLoginRepository.save(novo);
                });

            return AuthResponseDTO.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .build();

        } catch (Exception ex) {
            log.error("Erro ao fazer login com Facebook", ex);
            return AuthResponseDTO.builder()
                .error("Invalid token: " + ex.getMessage())
                .build();
        }
    }

    @Transactional
    public AuthResponseDTO loginComApple(String identityToken) {
        try {
            JsonNode claims = validarAppleToken(identityToken);
            String email = claims.get("email").asText();
            String providerId = claims.get("sub").asText();
            String nome = email.split("@")[0];

            Usuario usuario = buscarOuCriarUsuario(email, nome, "apple");
            
            socialLoginRepository.findByProviderAndProviderId("apple", providerId)
                .orElseGet(() -> {
                    SocialLogin novo = SocialLogin.builder()
                        .usuario(usuario)
                        .provider("apple")
                        .providerId(providerId)
                        .email(email)
                        .nome(nome)
                        .emailVerified(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                    return socialLoginRepository.save(novo);
                });

            return AuthResponseDTO.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .build();

        } catch (Exception ex) {
            log.error("Erro ao fazer login com Apple", ex);
            return AuthResponseDTO.builder()
                .error("Invalid token: " + ex.getMessage())
                .build();
        }
    }

    private GoogleProfile carregarPerfilGoogle(String token, String tokenType) throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Google token ausente");
        }

        if (!googleClientIdConfigurado()) {
            throw new IllegalArgumentException("Google Client ID do backend nao configurado");
        }

        if ("id_token".equals(tokenType)) {
            JsonNode tokenInfo = googleTokenInfo("id_token", token);
            validarGoogleAudience(tokenInfo);
            return googleProfileFrom(tokenInfo, tokenType);
        }

        JsonNode tokenInfo = googleTokenInfo("access_token", token);
        validarGoogleAudience(tokenInfo);
        JsonNode userInfo = googleUserInfo(token);
        return googleProfileFrom(userInfo, tokenType);
    }

    private JsonNode googleTokenInfo(String parameterName, String token) throws Exception {
        URI uri = UriComponentsBuilder
                .fromUriString(GOOGLE_TOKENINFO_URL)
                .queryParam(parameterName, token)
                .build()
                .encode()
                .toUri();

        try {
            String response = restTemplate.getForObject(uri, String.class);
            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("Google tokeninfo sem resposta");
            }
            return objectMapper.readTree(response);
        } catch (HttpStatusCodeException ex) {
            log.warn("google_tokeninfo_rejected status={} tokenType={}", ex.getStatusCode(), parameterName);
            throw new IllegalArgumentException("Google token rejeitado pelo Google");
        }
    }

    private JsonNode googleUserInfo(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalArgumentException("Google userinfo sem resposta valida");
            }

            return objectMapper.readTree(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.warn("google_userinfo_rejected status={}", ex.getStatusCode());
            throw new IllegalArgumentException("Google access token sem permissao de perfil");
        }
    }

    private void validarGoogleAudience(JsonNode tokenInfo) {
        String audience = firstNonBlank(
                text(tokenInfo, "aud"),
                text(tokenInfo, "audience"),
                text(tokenInfo, "azp"),
                text(tokenInfo, "issued_to")
        );

        if (audience == null) {
            throw new IllegalArgumentException("Google token sem audience");
        }

        if (!googleClientId.equals(audience)) {
            log.warn(
                    "google_audience_mismatch expectedClientIdHash={} receivedAudienceHash={}",
                    hashForLog(googleClientId),
                    hashForLog(audience)
            );
            throw new IllegalArgumentException("Google token com audience invalida");
        }
    }

    private GoogleProfile googleProfileFrom(JsonNode data, String tokenType) {
        String email = text(data, "email");
        String providerId = firstNonBlank(text(data, "sub"), text(data, "user_id"));
        String nome = firstNonBlank(text(data, "name"), email);

        if (email == null) {
            throw new IllegalArgumentException("Google payload sem email");
        }

        if (providerId == null) {
            throw new IllegalArgumentException("Google payload sem identificador do usuario");
        }

        Boolean emailVerified = booleanText(data, "email_verified");
        return new GoogleProfile(providerId, email, nome, tokenType, emailVerified);
    }

    private String googleTokenType(String token) {
        if (token == null || token.isBlank()) {
            return "missing";
        }
        return token.split("\\.").length == 3 ? "id_token" : "access_token";
    }

    private boolean googleClientIdConfigurado() {
        return googleClientId != null
                && !googleClientId.isBlank()
                && !googleClientId.startsWith("seu-google-client-id");
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Boolean booleanText(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return false;
        }

        JsonNode value = node.get(fieldName);
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        return "true".equalsIgnoreCase(value.asText());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String tokenFingerprint(String token) {
        if (token == null || token.isBlank()) {
            return "missing";
        }
        return hashForLog(token);
    }

    private String googleFailureReason(Exception ex) {
        if (ex instanceof DataIntegrityViolationException) {
            return "Nao foi possivel criar a conta social por restricao de dados obrigatorios.";
        }

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof DataIntegrityViolationException) {
                return "Nao foi possivel criar a conta social por restricao de dados obrigatorios.";
            }
            cause = cause.getCause();
        }

        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Falha ao autenticar com Google.";
        }
        return message;
    }

    private String hashForLog(String value) {
        if (value == null || value.isBlank()) {
            return "missing";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed).substring(0, 12);
        } catch (Exception e) {
            return "hash_error";
        }
    }

    private JsonNode validarAppleToken(String identityToken) throws Exception {
        String[] parts = identityToken.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid token");

        String payload = parts[1];
        payload += "==".substring((payload.length() * 8) % 6);
        byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
        
        return objectMapper.readTree(decodedBytes);
    }

    private Usuario buscarOuCriarUsuario(String email, String nome, String provider) {
        return usuarioRepository.findByEmail(email)
            .orElseGet(() -> {
                Usuario user = Usuario.builder()
                    .email(email)
                    .nome(nome)
                    .tipoUsuario(TipoUsuario.CONTRATANTE)
                    .ativo(true)
                    .build();
                user.setSenha("OAUTH2_" + provider.toUpperCase());
                return usuarioRepository.save(user);
            });
    }

    private record GoogleProfile(String providerId, String email, String nome, String tokenType, Boolean emailVerified) {
    }
}
