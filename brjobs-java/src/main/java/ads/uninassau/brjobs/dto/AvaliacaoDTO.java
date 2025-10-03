package ads.uninassau.brjobs.dto;
import lombok.Data;


@Data
public class AvaliacaoDTO {
    private Long id;
    private Integer nota;
    private String comentario;
    private Long solicitacaoId;
    private Long usuarioId;
    private Long prestadorId;
}
