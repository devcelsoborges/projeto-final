package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.ChatConversationDTO;
import ads.uninassau.brjobs.dto.ChatMessageDTO;
import ads.uninassau.brjobs.model.ChatMessage;
import ads.uninassau.brjobs.model.ConversaChat;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.ChatMessageRepository;
import ads.uninassau.brjobs.repository.ConversaChatRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversaChatRepository conversaChatRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChatAntiSpamService chatAntiSpamService;

    @Value("${app.chat.history.default-limit:50}")
    private int defaultHistoryLimit;

    @Value("${app.chat.history.max-limit:100}")
    private int maxHistoryLimit;

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

        String conteudoNormalizado = conteudo == null ? "" : conteudo.trim();

        if (conteudoNormalizado.isEmpty()) {
            throw new IllegalArgumentException("Conteúdo da mensagem é obrigatório");
        }

        if (conteudoNormalizado.length() > 500) {
            throw new IllegalArgumentException("Mensagem deve ter no máximo 500 caracteres");
        }

        log.info("chat_message_send_attempt user_id={} destinatario_id={} message_length={}", remetenteId, destinatarioId, conteudoNormalizado.length());

        chatAntiSpamService.checkAllowed(remetenteId, conteudoNormalizado);

        // Buscar usuários
        Usuario remetente = usuarioRepository.findById(remetenteId)
            .orElseThrow(() -> new IllegalArgumentException("Remetente não encontrado"));
        Usuario destinatario = usuarioRepository.findById(destinatarioId)
            .orElseThrow(() -> new IllegalArgumentException("Destinatário não encontrado"));

        // Criar mensagem
        ChatMessage msg = ChatMessage.builder()
            .remetente(remetente)
            .destinatario(destinatario)
            .conteudo(conteudoNormalizado)
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

        log.info("chat_message_send_success user_id={} destinatario_id={} mensagem_id={}", remetenteId, destinatarioId, msg.getId());

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
        log.info("chat_mark_read_success user_id={} mensagem_id={}", usuarioId, mensagemId);
    }

    @Transactional
    public int marcarConversaComoLida(Long usuarioId, Long outroUsuarioId) {
        int updated = chatMessageRepository.markConversationAsRead(usuarioId, outroUsuarioId);
        log.info("chat_mark_read_bulk user_id={} other_user_id={} total_updated={}", usuarioId, outroUsuarioId, updated);
        return updated;
    }

    /**
     * Obtém histórico de conversa entre dois usuários (com paginação)
     */
    public List<ChatMessageDTO> obterConversa(Long usuarioId, Long outroUsuarioId, int limit) {
        int normalizedLimit = normalizeHistoryLimit(limit);

        if (!usuarioRepository.existsById(outroUsuarioId)) {
            throw new IllegalArgumentException("Usuário da conversa não encontrado");
        }

        List<ChatMessage> mensagens = chatMessageRepository.findConversationHistory(usuarioId, outroUsuarioId, normalizedLimit);

        log.info("chat_messages_loaded user_id={} other_user_id={} messages_count={}", usuarioId, outroUsuarioId, mensagens.size());

        return mensagens.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtém lista de conversas ativas de um usuário
     */
    public List<ChatConversationDTO> obterConversas(Long usuarioId) {
        List<ConversaChat> conversas = conversaChatRepository.findActiveConversations(usuarioId);

        List<ChatConversationDTO> dtos = conversas.stream()
            .map(conversa -> mapConversationToDTO(conversa, usuarioId))
            .collect(Collectors.toList());

        log.info("chat_conversations_loaded user_id={} conversations_count={}", usuarioId, dtos.size());

        return dtos;
    }

    /**
     * Conta mensagens não-lidas de um usuário
     */
    public long contarNaoLidas(Long usuarioId) {
        long unread = chatMessageRepository.countUnreadFor(usuarioId);
        log.info("chat_unread_count_loaded user_id={} unread_total={}", usuarioId, unread);
        return unread;
    }

    private ChatConversationDTO mapConversationToDTO(ConversaChat conversa, Long usuarioId) {
        Usuario contato = conversa.getUsuario1().getId().equals(usuarioId)
            ? conversa.getUsuario2()
            : conversa.getUsuario1();

        ChatMessage ultima = conversa.getUltimaMensagem();
        long naoLidas = chatMessageRepository.countUnreadBySender(usuarioId, contato.getId());

        return ChatConversationDTO.builder()
            .id(conversa.getId())
            .contatoId(contato.getId())
            .contatoNome(contato.getNome())
            .ultimaMensagem(ultima != null ? ultima.getConteudo() : null)
            .ultimaMensagemEm(ultima != null ? ultima.getCreatedAt() : null)
            .ultimaMensagemRemetenteId(ultima != null ? ultima.getRemetente().getId() : null)
            .naoLidas(naoLidas)
            .atualizadaEm(conversa.getUpdatedAt())
            .build();
    }

    private int normalizeHistoryLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return defaultHistoryLimit;
        }

        return Math.min(requestedLimit, maxHistoryLimit);
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
