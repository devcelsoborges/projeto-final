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
public class ChatMessageDTO {
    private Long id;
    private Long remetenteId;
    private String remetenteName;
    private Long destinatarioId;
    private String conteudo;
    private Boolean lido;
    private LocalDateTime criadoEm;
}
