package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.EmailNotConfirmedException;
import ads.uninassau.brjobs.exception.SocialOnlyAccountException;
import ads.uninassau.brjobs.exception.UserNotFoundException;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

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

    @Value("${app.password-reset.expires-minutes:60}")
    private int passwordResetExpiresMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

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
        // Validar que o usuário existe (busca case-insensitive: mesma conta do login social)
        String email = UsuarioValidator.normalizarEmail(loginRequest.getEmail());

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + email));

        // Conta criada via login social sem senha local: orientar em vez de "senha inválida"
        if (usuario.isSomenteLoginSocial()) {
            throw new SocialOnlyAccountException(
                    "Esta conta foi criada com login social. Entre com o provedor (Google/Facebook) "
                            + "ou defina uma senha pela opção 'Esqueci minha senha'.");
        }


        // Criar token de autenticação com email e senha
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        usuario.getEmail(),
                        loginRequest.getSenha()
                );

        
        // Autenticar - lança AuthenticationException se as credenciais forem inválidas
        try {
            Authentication authentication = authenticationManager.authenticate(authToken);
            // Senha correta, mas e-mail não confirmado: bloqueia (só quando explicitamente false).
            if (Boolean.FALSE.equals(usuario.getEmailConfirmado())) {
                throw new EmailNotConfirmedException("Confirme seu e-mail para acessar a conta.");
            }
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
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
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
        dto.setEmailConfirmado(entity.getEmailConfirmado());
        if (entity.getDataCadastro() != null) {
            dto.setDataCadastro(entity.getDataCadastro().toLocalDate());
        }
        return dto;
    }

    /**
     * Gera token JWT para um e-mail específico (usado em login social).
     */
    public String generateTokenForEmail(String email) {
        return jwtTokenService.generateToken(UsuarioValidator.normalizarEmail(email));
    }

    /**
     * Gera um token forte de redefinição, guarda apenas o hash e envia o link por e-mail.
     * Resposta do chamador é sempre genérica (não revela se o e-mail existe).
     */
    @Transactional
    public void solicitarRecuperacaoSenha(String rawEmail) {
        String email = UsuarioValidator.normalizarEmail(rawEmail);
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            String rawToken = gerarToken();
            usuario.setPasswordResetTokenHash(hash(rawToken));
            usuario.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(passwordResetExpiresMinutes));
            usuario.setPasswordResetCode(null); // descontinua o código antigo
            usuarioRepository.save(usuario);

            boolean sentByEmail = passwordResetEmailService.sendResetLink(email, usuario.getNome(), rawToken);
            log.info("password_reset_link_gerado userId={} emailEnviado={}", usuario.getId(), sentByEmail);
        });
    }

    /**
     * Redefine a senha a partir do token do link. O token vai apenas hasheado no banco;
     * é de uso único (limpo após o reset) e expira em {@code passwordResetExpiresMinutes}.
     */
    @Transactional
    public void redefinirSenhaComToken(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Link inválido ou expirado.");
        }

        Usuario usuario = usuarioRepository.findByPasswordResetTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou expirado."));

        boolean valido = usuario.getPasswordResetExpiresAt() != null
                && LocalDateTime.now().isBefore(usuario.getPasswordResetExpiresAt());
        if (!valido) {
            throw new IllegalArgumentException("Link inválido ou expirado.");
        }

        UsuarioValidator.validarSenha(newPassword);
        usuario.setSenha(passwordEncoder.encode(newPassword));
        usuario.setPasswordResetTokenHash(null);
        usuario.setPasswordResetCode(null);
        usuario.setPasswordResetExpiresAt(null);
        usuarioRepository.save(usuario);
        log.info("password_reset_concluido userId={}", usuario.getId());
    }

    /** Token aleatório de 32 bytes (256 bits) em Base64 URL-safe — inviável de força-bruta. */
    private String gerarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash do token", e);
        }
    }
}
