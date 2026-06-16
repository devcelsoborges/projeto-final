package ads.uninassau.brjobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de autenticação (login tradicional ou OAuth2)
 * Retorna tokens JWT e informações básicas do usuário
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    private String refreshToken;
    private Long usuarioId;
    private String email;
    private String nome;
    private String error;  // Para erros (se houver)
}
