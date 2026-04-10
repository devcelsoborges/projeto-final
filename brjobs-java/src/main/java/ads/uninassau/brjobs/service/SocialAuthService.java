package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.AuthResponseDTO;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.model.SocialLogin;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.repository.SocialLoginRepository;
import ads.uninassau.brjobs.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final UsuarioRepository usuarioRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final JwtTokenService jwtTokenService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthResponseDTO loginComGoogle(String idToken) {
        try {
            JsonNode claims = validarGoogleToken(idToken);
            String email = claims.get("email").asText();
            String providerId = claims.get("sub").asText();
            String nome = claims.get("name").asText();

            Usuario usuario = buscarOuCriarUsuario(email, nome, "google");
            
            socialLoginRepository.findByProviderAndProviderId("google", providerId)
                .orElseGet(() -> {
                    SocialLogin novo = SocialLogin.builder()
                        .usuario(usuario)
                        .provider("google")
                        .providerId(providerId)
                        .email(email)
                        .nome(nome)
                        .createdAt(LocalDateTime.now())
                        .build();
                    return socialLoginRepository.save(novo);
                });

            String jwtToken = jwtTokenService.generateToken(usuario.getEmail());

            return AuthResponseDTO.builder()
                .token(jwtToken)
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .build();

        } catch (Exception ex) {
            log.error("Erro ao fazer login com Google", ex);
            return AuthResponseDTO.builder()
                .error("Invalid token: " + ex.getMessage())
                .build();
        }
    }

    @Transactional
    public AuthResponseDTO loginComFacebook(String accessToken) {
        try {
            String facebookUserUrl = "https://graph.facebook.com/v18.0/me?fields=id,name,email&access_token=" + accessToken;
            String response = restTemplate.getForObject(facebookUserUrl, String.class);
            JsonNode userData = objectMapper.readTree(response);

            String providerId = userData.get("id").asText();
            String email = userData.get("email").asText();
            String nome = userData.get("name").asText();

            Usuario usuario = buscarOuCriarUsuario(email, nome, "facebook");
            
            socialLoginRepository.findByProviderAndProviderId("facebook", providerId)
                .orElseGet(() -> {
                    SocialLogin novo = SocialLogin.builder()
                        .usuario(usuario)
                        .provider("facebook")
                        .providerId(providerId)
                        .email(email)
                        .nome(nome)
                        .createdAt(LocalDateTime.now())
                        .build();
                    return socialLoginRepository.save(novo);
                });

            String jwtToken = jwtTokenService.generateToken(usuario.getEmail());

            return AuthResponseDTO.builder()
                .token(jwtToken)
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
                        .createdAt(LocalDateTime.now())
                        .build();
                    return socialLoginRepository.save(novo);
                });

            String jwtToken = jwtTokenService.generateToken(usuario.getEmail());

            return AuthResponseDTO.builder()
                .token(jwtToken)
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

    private JsonNode validarGoogleToken(String idToken) throws Exception {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid token");

        String payload = parts[1];
        payload += "==".substring((payload.length() * 8) % 6);
        byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
        
        return objectMapper.readTree(decodedBytes);
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
}