package ads.uninassau.brjobs.security;

import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

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

        System.out.println("DEBUG CustomUserDetailsService: Carregando usuário com email: " + email);
        
        // Busca a entidade Usuario pelo e-mail
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("DEBUG CustomUserDetailsService: ERRO - Usuário não encontrado com email: " + email);
                    return new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email);
                });

        System.out.println("DEBUG CustomUserDetailsService: Usuário encontrado. Email: " + usuario.getEmail());
        System.out.println("DEBUG CustomUserDetailsService: Senha no banco (hash): " + usuario.getSenha().substring(0, Math.min(20, usuario.getSenha().length())) + "...");
        
        // Cria e retorna o objeto UserDetails que o Spring Security espera.
        // Se você tiver roles/autoridades, elas deverão ser mapeadas aqui.
        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getSenha(), // Senha criptografada
                new ArrayList<>() // Lista de Authorities/Roles (vazia por padrão)
        );
    }
}