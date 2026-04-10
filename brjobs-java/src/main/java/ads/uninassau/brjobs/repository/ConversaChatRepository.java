package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.ConversaChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversaChatRepository extends JpaRepository<ConversaChat, Long> {

    /**
     * Encontra conversa entre dois usuários (ordem não importa, DB garante usuario_1_id < usuario_2_id)
     */
    @Query("SELECT c FROM ConversaChat c WHERE " +
           "(c.usuario1.id = :user1 AND c.usuario2.id = :user2) OR " +
           "(c.usuario1.id = :user2 AND c.usuario2.id = :user1)")
    Optional<ConversaChat> findByUsuarios(@Param("user1") Long user1, @Param("user2") Long user2);

    /**
     * Encontra todas as conversas do usuário (como usuario_1 ou usuario_2), ordenadas por atualização recente
     */
    @Query("SELECT c FROM ConversaChat c WHERE " +
           "c.usuario1.id = :userId OR c.usuario2.id = :userId " +
           "ORDER BY c.updatedAt DESC")
    List<ConversaChat> findByUsuario(@Param("userId") Long userId);

    /**
     * Encontra conversas ativas (com mensagens recentes)
     */
    @Query("SELECT c FROM ConversaChat c WHERE " +
           "(c.usuario1.id = :userId OR c.usuario2.id = :userId) " +
           "AND c.ultimaMensagem IS NOT NULL " +
           "ORDER BY c.updatedAt DESC")
    List<ConversaChat> findActiveConversations(@Param("userId") Long userId);
}
