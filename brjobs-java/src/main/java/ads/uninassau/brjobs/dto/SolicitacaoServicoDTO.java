package ads.uninassau.brjobs.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolicitacaoServicoDTO {
    private Long id;
    private String status;
    private Long usuarioId;
    private Long servicoId;
    private LocalDateTime dataSolicitacao;
}
