package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.model.ChatMessage;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notificação "offline" de novas mensagens de chat por e-mail, usando o template do
 * Resend ("new-message-notification").
 *
 * Job em background (intervalo configurável) que pega mensagens NÃO lidas e NÃO
 * notificadas criadas há mais que a carência, agrupa POR CONVERSA (destinatário +
 * remetente) e envia UM e-mail por conversa, com os dados da mensagem mais recente.
 * Se o destinatário já leu no app durante a carência, a mensagem não entra (não vira spam).
 *
 * Entrega é best-effort: após tentar enviar, marca {@code notificado=true}
 * independentemente do resultado, para não reprocessar a mesma mensagem a cada ciclo.
 * Sem Resend configurado, o job não consome nada (notifica quando for habilitado).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationService {

    /** Limite do trecho da mensagem na prévia (evita estourar o layout do e-mail). */
    private static final int PREVIEW_MAX = 160;

    private final ChatMessageRepository chatMessageRepository;
    private final MailService mailService;

    @Value("${app.chat.notification.enabled:true}")
    private boolean enabled;

    @Value("${app.chat.notification.grace-minutes:2}")
    private int graceMinutes;

    @Value("${app.mail.resend.template.new-message:new-message-notification}")
    private String templateId;

    @Value("${app.frontend.chat-url:http://localhost:4200/chat}")
    private String chatUrl;

    @Value("${app.chat.notification.listing-fallback:sua conversa no BRJobs}")
    private String listingFallback;

    @Scheduled(fixedDelayString = "${app.chat.notification.interval-ms:60000}")
    @Transactional
    public void notificarMensagensPendentes() {
        if (!enabled) {
            return;
        }
        // Sem provedor de e-mail não consumimos as mensagens (notifica quando configurar).
        if (!mailService.isConfigured()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(graceMinutes);
        List<ChatMessage> pendentes = chatMessageRepository
                .findByNotificadoFalseAndLidoFalseAndCreatedAtBefore(cutoff);
        if (pendentes.isEmpty()) {
            return;
        }

        // Agrupa por CONVERSA: destinatário + remetente.
        Map<String, List<ChatMessage>> porConversa = pendentes.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getDestinatario().getId() + ":" + m.getRemetente().getId()));

        int enviados = 0;
        for (List<ChatMessage> msgs : porConversa.values()) {
            ChatMessage maisRecente = msgs.stream()
                    .max(Comparator.comparing(ChatMessage::getCreatedAt))
                    .orElse(msgs.get(0));
            Usuario destinatario = maisRecente.getDestinatario();
            Usuario remetente = maisRecente.getRemetente();

            String senderName = remetente.getNome();
            // Escapa TODAS as variáveis vindas de conteúdo do usuário (nome do remetente,
            // primeiro nome do destinatário e prévia da mensagem) — não só a prévia — para
            // evitar injeção/quebra de layout caso o template interpole sem escapar.
            Map<String, Object> variables = Map.of(
                    "first_name", escaparHtml(primeiroNome(destinatario.getNome(), destinatario.getEmail())),
                    "sender_name", escaparHtml(senderName),
                    "listing_title", listingFallback,
                    "message_preview", truncarEscapar(maisRecente.getConteudo()),
                    "message_url", chatUrl
            );

            boolean ok = mailService.sendTemplate(
                    destinatario.getEmail(),
                    templateId,
                    "Nova mensagem de " + senderName + " — BRJobs",
                    variables,
                    null
            );
            if (ok) {
                enviados++;
            }

            // Best-effort: marca como notificado mesmo em falha de envio, para não reprocessar.
            msgs.forEach(m -> m.setNotificado(true));
            chatMessageRepository.saveAll(msgs);
        }

        log.info("chat_notification_job_run conversas={} mensagens={} emails_aceitos={}",
                porConversa.size(), pendentes.size(), enviados);
    }

    /**
     * Trunca o conteúdo a {@value #PREVIEW_MAX} caracteres (com reticências) e escapa HTML —
     * a prévia vem de conteúdo digitado pelo usuário, então evita quebra de layout e injeção.
     */
    private String truncarEscapar(String conteudo) {
        String s = conteudo == null ? "" : conteudo.trim();
        if (s.length() > PREVIEW_MAX) {
            int end = PREVIEW_MAX;
            // Não cortar no meio de um par surrogate (emoji/caracteres fora do BMP).
            if (Character.isHighSurrogate(s.charAt(end - 1))) {
                end--;
            }
            s = s.substring(0, end).trim() + "…";
        }
        return escaparHtml(s);
    }

    private String escaparHtml(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String primeiroNome(String nome, String email) {
        if (nome != null && !nome.isBlank()) {
            return nome.trim().split("\\s+")[0];
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "usuário";
    }
}
