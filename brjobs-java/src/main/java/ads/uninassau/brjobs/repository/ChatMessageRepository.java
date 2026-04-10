package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Encontra histórico de mensagens entre dois usuários (ordenado por data DESC)
     */
    List<ChatMessage> findByRemetenteIdAndDestinatarioIdOrderByCreatedAtDesc(Long remetenteId, Long destinatarioId);

    /**
     * Encontra todas as mensagens não-lidas para um destinatário
     */
    List<ChatMessage> findByDestinatarioIdAndLidoFalse(Long destinatarioId);

    /**
     * Encontra mensagens não notificadas (para job em background)
     */
    List<ChatMessage> findByNotificadoFalse();

    /**
     * Query customizada para contar mensagens não-lidas
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.destinatario.id = :destinatarioId AND m.lido = false")
    long countUnreadFor(@Param("destinatarioId") Long destinatarioId);

    /**
     * Query para últimas N mensagens de uma conversa
     */
    @Query(nativeQuery = true, value = 
        "SELECT * FROM chat_messages " +
        "WHERE (remetente_id = :user1 AND destinatario_id = :user2) " +
        "   OR (remetente_id = :user2 AND destinatario_id = :user1) " +
        "ORDER BY created_at DESC LIMIT :limit")
    List<ChatMessage> findConversationHistory(
        @Param("user1") Long user1, 
        @Param("user2") Long user2,
        @Param("limit") int limit
    );
}
