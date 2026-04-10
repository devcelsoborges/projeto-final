package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.ChatMessageDTO;
import ads.uninassau.brjobs.model.ChatMessage;
import ads.uninassau.brjobs.model.ConversaChat;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.ChatMessageRepository;
import ads.uninassau.brjobs.repository.ConversaChatRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversaChatRepository conversaChatRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Envia mensagem de um usuário (tenant) para outro
     * Valida isolamento: remetenteId = tenant_id (do JWT)
     */
    @Transactional
    public ChatMessageDTO enviarMensagem(Long remetenteId, Long destinatarioId, String conteudo) {
        // Validar que remetente e destinatário são diferentes
        if (remetenteId.equals(destinatarioId)) {
            throw new IllegalArgumentException("Não pode enviar mensagem para si mesmo");
        }

        // Buscar usuários
        Usuario remetente = usuarioRepository.findById(remetenteId)
            .orElseThrow(() -> new IllegalArgumentException("Remetente não encontrado"));
        Usuario destinatario = usuarioRepository.findById(destinatarioId)
            .orElseThrow(() -> new IllegalArgumentException("Destinatário não encontrado"));

        // Criar mensagem
        ChatMessage msg = ChatMessage.builder()
            .remetente(remetente)
            .destinatario(destinatario)
            .conteudo(conteudo)
            .lido(false)
            .notificado(false)
            .build();

        chatMessageRepository.save(msg);

        // Atualizar ou criar conversa
        ConversaChat conversa = conversaChatRepository.findByUsuarios(remetenteId, destinatarioId)
            .orElseGet(() -> {
                ConversaChat novaConversa = ConversaChat.builder()
                    .usuario1(remetenteId < destinatarioId ? remetente : destinatario)
                    .usuario2(remetenteId < destinatarioId ? destinatario : remetente)
                    .build();
                return conversaChatRepository.save(novaConversa);
            });

        conversa.setUltimaMensagem(msg);
        conversaChatRepository.save(conversa);

        log.info("ChatService: Mensagem enviada de {} para {}", remetenteId, destinatarioId);

        return mapToDTO(msg);
    }

    /**
     * Marca mensagem como lida
     */
    @Transactional
    public void marcarComoLida(Long mensagemId, Long usuarioId) {
        ChatMessage msg = chatMessageRepository.findById(mensagemId)
            .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));

        // Validar que usuário é o destinatário
        if (!msg.getDestinatario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Acesso negado: usuário não é destinatário");
        }

        msg.setLido(true);
        chatMessageRepository.save(msg);
    }

    /**
     * Obtém histórico de conversa entre dois usuários (com paginação)
     */
    public List<ChatMessageDTO> obterConversa(Long usuarioId, Long outroUsuarioId, int limit) {
        List<ChatMessage> mensagens = chatMessageRepository.findConversationHistory(usuarioId, outroUsuarioId, limit);
        return mensagens.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtém lista de conversas ativas de um usuário
     */
    public List<ConversaChat> obterConversas(Long usuarioId) {
        return conversaChatRepository.findActiveConversations(usuarioId);
    }

    /**
     * Conta mensagens não-lidas de um usuário
     */
    public long contarNaoLidas(Long usuarioId) {
        return chatMessageRepository.countUnreadFor(usuarioId);
    }

    /**
     * Mapeia entidade para DTO
     */
    private ChatMessageDTO mapToDTO(ChatMessage msg) {
        return ChatMessageDTO.builder()
            .id(msg.getId())
            .remetenteId(msg.getRemetente().getId())
            .remetenteName(msg.getRemetente().getNome())
            .destinatarioId(msg.getDestinatario().getId())
            .conteudo(msg.getConteudo())
            .lido(msg.getLido())
            .criadoEm(msg.getCreatedAt())
            .build();
    }
}
