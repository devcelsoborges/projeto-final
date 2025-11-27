package ads.uninassau.brjobs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private String message;

    // Construtor auxiliar para sucesso (Token OK)
    public LoginResponseDTO(String token) {
        this.token = token;
        this.message = "Login realizado com sucesso.";
    }
}