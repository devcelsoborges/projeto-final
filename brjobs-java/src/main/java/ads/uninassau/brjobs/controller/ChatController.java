package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.ChatMessageDTO;
import ads.uninassau.brjobs.model.ConversaChat;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Envia mensagem de um usuário para outro
     * POST /api/v1/chat/enviar
     * Body: { "destinatarioId": 5, "conteudo": "Olá, tudo bem?" }
     */
    @PostMapping("/enviar")
    @ValidateTenant
    public ResponseEntity<ChatMessageDTO> enviarMensagem(
        @RequestAttribute("tenant_id") Long tenantId,
        @RequestParam Long destinatarioId,
        @RequestBody ChatMessageDTO dto
    ) {
        ChatMessageDTO resposta = chatService.enviarMensagem(tenantId, destinatarioId, dto.getConteudo());
        return new ResponseEntity<>(resposta, HttpStatus.CREATED);
    }

    /**
     * Marca mensagem como lida
     * PUT /api/v1/chat/marcar-lida/:id
     */
    @PutMapping("/marcar-lida/{id}")
    @ValidateTenant
    public ResponseEntity<Void> marcarComoLida(
        @RequestAttribute("tenant_id") Long tenantId,
        @PathVariable Long id
    ) {
        chatService.marcarComoLida(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtém histórico de conversa entre dois usuários
     * GET /api/v1/chat/conversa/:outroUsuarioId?limit=50
     */
    @GetMapping("/conversa/{outroUsuarioId}")
    @ValidateTenant
    public ResponseEntity<List<ChatMessageDTO>> obterConversa(
        @RequestAttribute("tenant_id") Long tenantId,
        @PathVariable Long outroUsuarioId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<ChatMessageDTO> mensagens = chatService.obterConversa(tenantId, outroUsuarioId, limit);
        return ResponseEntity.ok(mensagens);
    }

    /**
     * Obtém lista de conversas ativas de um usuário
     * GET /api/v1/chat/conversas
     */
    @GetMapping("/conversas")
    @ValidateTenant
    public ResponseEntity<List<ConversaChat>> obterConversas(
        @RequestAttribute("tenant_id") Long tenantId
    ) {
        List<ConversaChat> conversas = chatService.obterConversas(tenantId);
        return ResponseEntity.ok(conversas);
    }

    /**
     * Conta mensagens não-lidas
     * GET /api/v1/chat/nao-lidas
     */
    @GetMapping("/nao-lidas")
    @ValidateTenant
    public ResponseEntity<Long> contarNaoLidas(
        @RequestAttribute("tenant_id") Long tenantId
    ) {
        long count = chatService.contarNaoLidas(tenantId);
        return ResponseEntity.ok(count);
    }
}
