package ads.uninassau.brjobs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmDTO {

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "O formato do email é inválido.")
    private String email;

    @NotBlank(message = "O código é obrigatório.")
    @Size(min = 6, max = 6, message = "O código deve ter 6 dígitos.")
    private String code;

    @NotBlank(message = "A nova senha é obrigatória.")
    private String newPassword;
}
