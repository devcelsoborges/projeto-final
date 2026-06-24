package ads.uninassau.brjobs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensagem enviada pelo formulário público de contato.
 * As regras espelham as validações do frontend (contato.component.ts).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContatoRequestDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O formato do e-mail é inválido.")
    @Size(max = 254, message = "E-mail muito longo.")
    private String email;

    @NotBlank(message = "O assunto é obrigatório.")
    @Size(min = 5, max = 150, message = "O assunto deve ter entre 5 e 150 caracteres.")
    private String assunto;

    @NotBlank(message = "A mensagem é obrigatória.")
    @Size(min = 10, max = 4000, message = "A mensagem deve ter entre 10 e 4000 caracteres.")
    private String mensagem;
}
