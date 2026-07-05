package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * Mensagens ainda NÃO notificadas por e-mail, ainda NÃO lidas e criadas antes do
     * corte (período de carência). Base do job de notificação "offline": se o
     * destinatário leu no app dentro da carência, a mensagem sai deste conjunto.
     */
    List<ChatMessage> findByNotificadoFalseAndLidoFalseAndCreatedAtBefore(LocalDateTime cutoff);

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

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.destinatario.id = :destinatarioId AND m.remetente.id = :remetenteId AND m.lido = false")
    long countUnreadBySender(@Param("destinatarioId") Long destinatarioId, @Param("remetenteId") Long remetenteId);

    /**
     * Não-lidas do destinatário agrupadas por remetente, em UMA query. Substitui o N+1 de
     * um {@link #countUnreadBySender} por conversa ao montar a lista em /chat/conversas.
     * Cada linha: [remetenteId (Long), total (Long)].
     */
    @Query("SELECT m.remetente.id, COUNT(m) FROM ChatMessage m " +
           "WHERE m.destinatario.id = :destinatarioId AND m.lido = false " +
           "GROUP BY m.remetente.id")
    List<Object[]> countUnreadGroupedBySender(@Param("destinatarioId") Long destinatarioId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.lido = true WHERE m.destinatario.id = :destinatarioId AND m.remetente.id = :remetenteId AND m.lido = false")
    int markConversationAsRead(@Param("destinatarioId") Long destinatarioId, @Param("remetenteId") Long remetenteId);
}
