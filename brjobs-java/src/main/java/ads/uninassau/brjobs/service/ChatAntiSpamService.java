package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.exception.ChatRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatAntiSpamService {

    @Value("${app.chat.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.chat.rate-limit.max-messages-per-window:20}")
    private int maxMessagesPerWindow;

    @Value("${app.chat.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${app.chat.rate-limit.min-interval-ms:800}")
    private long minIntervalMs;

    @Value("${app.chat.rate-limit.duplicate-window-seconds:15}")
    private int duplicateWindowSeconds;

    private final ConcurrentHashMap<Long, UserChatThrottleState> userStates = new ConcurrentHashMap<>();

    public void checkAllowed(Long remetenteId, String conteudo) {
        if (!enabled || remetenteId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        long duplicateWindowMs = duplicateWindowSeconds * 1000L;
        String normalizedContent = normalizeContent(conteudo);

        UserChatThrottleState state = userStates.computeIfAbsent(remetenteId, id -> new UserChatThrottleState());

        synchronized (state) {
            while (!state.messageTimestamps.isEmpty() && now - state.messageTimestamps.peekFirst() > windowMs) {
                state.messageTimestamps.pollFirst();
            }

            if (state.lastMessageTimestamp > 0 && now - state.lastMessageTimestamp < minIntervalMs) {
                log.warn("chat_rate_limit_blocked user_id={} reason=interval", remetenteId);
                throw new ChatRateLimitException("Você está enviando mensagens muito rápido. Aguarde alguns segundos.");
            }

            if (state.messageTimestamps.size() >= maxMessagesPerWindow) {
                log.warn("chat_rate_limit_blocked user_id={} reason=window", remetenteId);
                throw new ChatRateLimitException("Limite de mensagens excedido. Tente novamente em instantes.");
            }

            if (!normalizedContent.isEmpty()
                && Objects.equals(state.lastNormalizedContent, normalizedContent)
                && now - state.lastNormalizedContentTimestamp < duplicateWindowMs) {
                log.warn("chat_rate_limit_blocked user_id={} reason=duplicate", remetenteId);
                throw new ChatRateLimitException("Mensagem duplicada enviada em sequência. Aguarde antes de repetir.");
            }

            state.messageTimestamps.addLast(now);
            state.lastMessageTimestamp = now;
            state.lastNormalizedContent = normalizedContent;
            state.lastNormalizedContentTimestamp = now;
        }
    }

    private String normalizeContent(String conteudo) {
        if (conteudo == null) {
            return "";
        }

        return conteudo
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase();
    }

    private static class UserChatThrottleState {
        private final Deque<Long> messageTimestamps = new ArrayDeque<>();
        private long lastMessageTimestamp;
        private long lastNormalizedContentTimestamp;
        private String lastNormalizedContent = "";
    }
}
