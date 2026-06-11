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
public class ChatConversationDTO {
    private Long id;
    private Long contatoId;
    private String contatoNome;
    private String ultimaMensagem;
    private LocalDateTime ultimaMensagemEm;
    private Long ultimaMensagemRemetenteId;
    private Long naoLidas;
    private LocalDateTime atualizadaEm;
}
