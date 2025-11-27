package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.UserNotFoundException;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por operações de autenticação.
 * NÃO é responsável pelo registro de usuários (usar UsuarioService).
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtTokenService jwtTokenService,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Autentica um usuário e gera um token JWT.
     *
     * @param loginRequest dados de login (email e senha)
     * @return token JWT gerado
     * @throws AuthenticationException se as credenciais forem inválidas
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional(readOnly = true)
    public String authenticateAndGetToken(LoginRequestDTO loginRequest) throws AuthenticationException {
        // Validar que o usuário existe
        usuarioRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + loginRequest.getEmail()));

        // Criar token de autenticação com email e senha
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getSenha()
                );

        // Autenticar - lança AuthenticationException se as credenciais forem inválidas
        Authentication authentication = authenticationManager.authenticate(authToken);

        // Gerar e retornar o token JWT
        return jwtTokenService.generateToken(authentication.getName());
    }

    /**
     * Busca o usuário autenticado atual
     *
     * @param email do usuário
     * @return UsuarioDTO com dados do usuário
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obterUsuarioAutenticado(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + email));
        return toDTO(usuario);
    }

    /**
     * Converte entidade Usuario para DTO
     */
    private UsuarioDTO toDTO(Usuario entity) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setTipoUsuario(entity.getTipoUsuario());
        dto.setTelefone(entity.getTelefone());
        dto.setEndereco(entity.getEndereco());
        dto.setCpf(entity.getCpf());
        dto.setGenero(entity.getGenero());
        dto.setDataNascimento(entity.getDataNascimento());
        dto.setAtivo(entity.isAtivo());
        if (entity.getDataCadastro() != null) {
            dto.setDataCadastro(entity.getDataCadastro().toLocalDate());
        }
        return dto;
    }
}