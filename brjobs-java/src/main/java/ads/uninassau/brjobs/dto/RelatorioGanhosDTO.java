package ads.uninassau.brjobs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioGanhosDTO {
    private YearMonth mes;
    private BigDecimal totalFaturado;
    private Integer numServicos;
    private List<BreakdownCategoriaDTO> porCategoria;
    private List<BreakdownClienteDTO> porCliente;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreakdownCategoriaDTO {
        private String categoria;
        private BigDecimal total;
        private Integer numServicos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreakdownClienteDTO {
        private Long clienteId;
        private String clienteNome;
        private BigDecimal total;
        private Integer numServicos;
    }
}
