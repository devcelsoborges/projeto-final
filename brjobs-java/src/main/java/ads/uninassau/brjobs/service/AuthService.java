package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.UserNotFoundException;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.validator.UsuarioValidator;
import ads.uninassau.brjobs.security.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Serviço responsável por operações de autenticação.
 * NÃO é responsável pelo registro de usuários (usar UsuarioService).
 */
@Service
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailService passwordResetEmailService;

    @Value("${app.password-reset.expires-minutes:15}")
    private int passwordResetExpiresMinutes;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtTokenService jwtTokenService,
                       AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       PasswordResetEmailService passwordResetEmailService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetEmailService = passwordResetEmailService;
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
        try {
            Authentication authentication = authenticationManager.authenticate(authToken);
            return jwtTokenService.generateToken(authentication.getName());
        } catch (AuthenticationException ex) {
            throw ex;
        }
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
        dto.setCep(entity.getCep());
        dto.setRua(entity.getRua());
        dto.setBairro(entity.getBairro());
        dto.setCidade(entity.getCidade());
        dto.setEstado(entity.getEstado());
        dto.setNumero(entity.getNumero());
        dto.setComplemento(entity.getComplemento());
        dto.setBio(entity.getBio());
        dto.setCpf(entity.getCpf());
        dto.setGenero(entity.getGenero());
        dto.setDataNascimento(entity.getDataNascimento());
        dto.setAtivo(entity.isAtivo());
        if (entity.getDataCadastro() != null) {
            dto.setDataCadastro(entity.getDataCadastro().toLocalDate());
        }
        return dto;
    }

    /**
     * Gera um token JWT de demonstração para login social.
     * TODO: Integrar com provedores OAuth reais (Google, Facebook, Apple)
     * 
     * @return token JWT para o usuário de demo
     */
    public String generateDemoToken() {
        return jwtTokenService.generateToken("demo@brjobs.com");
    }

    /**
     * Gera token JWT para um e-mail específico (usado em login social).
     */
    public String generateTokenForEmail(String email) {
        return jwtTokenService.generateToken(email);
    }

    /**
     * Busca usuário por e-mail e cria um usuário social mínimo quando não existir.
     * O usuário social é criado como CONTRATANTE com dados padrão obrigatórios.
     */
    @Transactional
    public Usuario findOrCreateSocialUser(String email, String nome) {
        return usuarioRepository.findByEmail(email)
                .orElseGet(() -> {
                    Usuario novo = new Usuario();
                    novo.setNome((nome == null || nome.isBlank()) ? email.split("@")[0] : nome);
                    novo.setEmail(email);
                    novo.setSenha("SOCIAL_LOGIN");
                    novo.setTipoUsuario(TipoUsuario.CONTRATANTE);
                    novo.setAtivo(true);
                    return usuarioRepository.save(novo);
                });
    }

    @Transactional
    public String solicitarRecuperacaoSenha(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> {
                    String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
                    usuario.setPasswordResetCode(code);
                    usuario.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(passwordResetExpiresMinutes));
                    usuarioRepository.save(usuario);

                    boolean sentByEmail = passwordResetEmailService.sendResetCode(email, code, passwordResetExpiresMinutes);

                    log.info("Password reset code gerado para {}: {} (email enviado: {})", email, code, sentByEmail);
                    return code;
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean verificarCodigoRecuperacao(String email, String code) {
        return usuarioRepository.findByEmail(email)
                .map(usuario ->
                        usuario.getPasswordResetCode() != null
                                && usuario.getPasswordResetCode().equals(code)
                                && usuario.getPasswordResetExpiresAt() != null
                                && LocalDateTime.now().isBefore(usuario.getPasswordResetExpiresAt()))
                .orElse(false);
    }

    @Transactional
    public void redefinirSenhaComCodigo(String email, String code, String newPassword) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido ou expirado."));

        boolean codigoValido = usuario.getPasswordResetCode() != null
                && usuario.getPasswordResetCode().equals(code)
                && usuario.getPasswordResetExpiresAt() != null
                && LocalDateTime.now().isBefore(usuario.getPasswordResetExpiresAt());

        if (!codigoValido) {
            throw new IllegalArgumentException("Código inválido ou expirado.");
        }

        UsuarioValidator.validarSenha(newPassword);
        usuario.setSenha(passwordEncoder.encode(newPassword));
        usuario.setPasswordResetCode(null);
        usuario.setPasswordResetExpiresAt(null);
        usuarioRepository.save(usuario);
    }
}
