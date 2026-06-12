package ads.uninassau.brjobs.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicacaoServicoDTO {
    private Long id;
    private String tipoPublicacao;
    private String titulo;
    private String descricao;
    private String categoria;
    private String enderecoPublicacao;
    private String cepPublicacao;
    private String cidadePublicacao;
    private String estadoPublicacao;
    private Double latitude;
    private Double longitude;
    private String geocodeProvider;
    private String geocodePrecision;
    private Double distanceKm;
    private Double preco;
    private Double orcamentoMin;
    private Double orcamentoMax;
    private String status;
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioBairro;
    private String usuarioCidade;
    private String usuarioEndereco;
    private Boolean isHighlighted;
    private LocalDateTime highlightExpiresAt;
    private Long highlightPlanId;
    private String highlightPlanName;
    private Integer highlightPriority;
    private LocalDateTime dataCriacao;
}
