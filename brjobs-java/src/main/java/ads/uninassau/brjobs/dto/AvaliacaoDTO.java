package ads.uninassau.brjobs.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para transferência de dados de Avaliação
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoDTO {

    private Long id;

    @NotNull(message = "A nota é obrigatória.")
    @Min(value = 1, message = "A nota mínima é 1.")
    @Max(value = 5, message = "A nota máxima é 5.")
    private Integer nota;

    @Size(max = 1000, message = "O comentário não pode exceder 1000 caracteres.")
    private String comentario;

    @NotNull(message = "O ID da solicitação é obrigatório.")
    private Long solicitacaoId;

    @NotNull(message = "O ID do usuário é obrigatório.")
    private Long usuarioId;

    @NotNull(message = "O ID do prestador é obrigatório.")
    private Long prestadorId;

    private LocalDateTime dataCriacao;
}
