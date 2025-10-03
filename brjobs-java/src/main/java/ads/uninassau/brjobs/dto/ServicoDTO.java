package ads.uninassau.brjobs.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ServicoDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private Double preco;
    private Long prestadorId;

}
