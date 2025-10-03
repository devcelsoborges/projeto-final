package ads.uninassau.brjobs.security; // Mantemos no pacote 'security' por convenção

import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // Usado para criar uma lista vazia de Authorities/Roles

@Service // É um serviço Spring, por isso a anotação @Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carrega o usuário pelo e-mail (username) a partir do banco de dados.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Busca a entidade Usuario pelo e-mail
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email)
                );

        // Cria e retorna o objeto UserDetails que o Spring Security espera.
        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getSenha(), // Senha criptografada
                new ArrayList<>() // Permissões/Roles
        );
    }
}