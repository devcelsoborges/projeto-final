package ads.uninassau.brjobs.dto;

import lombok.Data;

@Data
public class GeocodeRequestDTO {
    private String endereco;
    private String cidade;
    private String estado;
    private String cep;
}
