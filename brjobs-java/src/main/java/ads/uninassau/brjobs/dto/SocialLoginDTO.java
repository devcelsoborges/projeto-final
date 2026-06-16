package ads.uninassau.brjobs.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para login social (Google, Facebook, Apple)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginDTO {
    
    private String provider; // "google", "facebook", "apple"
    private String idToken;  // ID Token do OAuth2 provedor
    private String accessToken; // Access Token opcional (para Facebook)
    private String code;     // Authorization code opcional
    
    // Dados extraídos do token (já validados)
    private String email;
    private String nome;
    private String fotoUrl;
    private String providerId; // ID único do usuário no provedor
}
