package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.security.JwtTokenService; // Novo Import
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService; // Novo campo injetado

    // Injeção via Construtor
    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) { // Novo parâmetro
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    // Método de Registro (já deve existir)
    @Transactional
    public UsuarioDTO registerUser(UsuarioDTO usuarioDTO) {
        if (usuarioRepository.findByEmail(usuarioDTO.getEmail()).isPresent()) {
            throw new IllegalStateException("E-mail já está em uso.");
        }

        Usuario usuario = toEntity(usuarioDTO);
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));

        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    /**
     * NOVO MÉTODO: Gera o token JWT para o usuário autenticado.
     * Este método é chamado pelo AuthController.
     */
    public String generateToken(String email) {
        // Apenas delega a chamada ao serviço de token.
        return jwtTokenService.generateToken(email);
    }

    // Métodos Auxiliares toEntity e toDTO (devem existir)
    private Usuario toEntity(UsuarioDTO dto) {
        // ... (lógica de conversão DTO para Entidade)
        Usuario entity = new Usuario();
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        entity.setSenha(dto.getSenha());
        return entity;
    }

    private UsuarioDTO toDTO(Usuario entity) {
        // ... (lógica de conversão Entidade para DTO)
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        return dto;
    }
}