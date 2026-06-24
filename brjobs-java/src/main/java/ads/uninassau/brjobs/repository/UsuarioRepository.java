package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca por e-mail de forma case-insensitive. É a chave de unificação de
     * contas: cadastro local e logins sociais (Google/Facebook) com o mesmo
     * e-mail — ainda que com diferença de maiúsculas — apontam para a mesma conta.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByCpf(String cpf);

    /** Busca por token de confirmação de cadastro (link do e-mail account-activation). */
    Optional<Usuario> findByEmailConfirmationToken(String token);

    /** Busca por hash do token de redefinição de senha (link do e-mail password-reset). */
    Optional<Usuario> findByPasswordResetTokenHash(String tokenHash);
}
