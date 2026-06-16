package ads.uninassau.brjobs.repository;

import ads.uninassau.brjobs.model.SocialLogin;
import ads.uninassau.brjobs.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {

    /**
     * Busca login social por provider + provider_id (único)
     */
    Optional<SocialLogin> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Busca login solar por usuario + provider (único)
     */
    Optional<SocialLogin> findByUsuarioAndProvider(Usuario usuario, String provider);

    /**
     * Lista todas as contas sociais de um usuário
     */
    List<SocialLogin> findByUsuario(Usuario usuario);

    /**
     * Busca por email e provider
     */
    Optional<SocialLogin> findByEmailAndProvider(String email, String provider);

    /**
     * Remove conexão social
     */
    void deleteByUsuarioAndProvider(Usuario usuario, String provider);
}
