package ads.uninassau.brjobs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetConfirmDTO {

    @NotBlank(message = "O token é obrigatório.")
    private String token;

    @NotBlank(message = "A nova senha é obrigatória.")
    private String newPassword;
}
