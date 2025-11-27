package ads.uninassau.brjobs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para resposta de dados do prestador
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrestadorResponseDTO {

    private Long id;
    private Long usuarioId;
    private String funcao;
    private String experienciaProfissional;
    private String especialidades;
    private String descricao;
    private boolean ativo;
    private LocalDateTime dataCadastro;
}

