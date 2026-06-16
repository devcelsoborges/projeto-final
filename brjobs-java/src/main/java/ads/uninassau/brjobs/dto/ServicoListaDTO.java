package ads.uninassau.brjobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicoListaDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private Double preco;
    private String categoria;
    private LocalDateTime dataCriacao;

    // Dados do prestador (nested)
    private PrestaServicoDTO prestador;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrestaServicoDTO {
        private Long id;
        private String nome;
        private Double avaliacaoMedia;
        private Long numAvaliacoes;
        private String fotoUrl;
    }
}
