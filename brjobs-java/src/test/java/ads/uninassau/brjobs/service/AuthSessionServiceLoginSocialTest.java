package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.exception.SocialOnlyAccountException;
import ads.uninassau.brjobs.model.SocialLogin;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.AuthAuditEventRepository;
import ads.uninassau.brjobs.repository.AuthRefreshTokenRepository;
import ads.uninassau.brjobs.repository.SocialLoginRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.security.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Login local em conta criada via login social")
class AuthSessionServiceLoginSocialTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthRefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthAuditEventRepository auditEventRepository;

    @Mock
    private SocialLoginRepository socialLoginRepository;

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private AuthSessionService authSessionService;

    @Test
    @DisplayName("Deve orientar a entrar com o provedor quando a conta não tem senha local")
    void loginComSenhaEmContaSocialDeveLancarSocialOnlyAccountException() {
        Usuario contaSocial = Usuario.builder()
                .id(42L)
                .nome("Joao do Google")
                .email("joao@example.com")
                .senha("OAUTH2_GOOGLE")
                .tipoUsuario(TipoUsuario.CONTRATANTE)
                .ativo(true)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("joao@example.com")).thenReturn(Optional.of(contaSocial));
        when(socialLoginRepository.findByUsuario(contaSocial)).thenReturn(List.of(
                SocialLogin.builder().usuario(contaSocial).provider("google").providerId("123").email("joao@example.com").nome("Joao").build()
        ));

        LoginRequestDTO request = new LoginRequestDTO("joao@example.com", "Senha@123");

        SocialOnlyAccountException ex = assertThrows(SocialOnlyAccountException.class,
                () -> authSessionService.login(request, servletRequest));

        assertTrue(ex.getMessage().contains("Google"), "mensagem deve citar o provedor da conta");
        assertTrue(ex.getMessage().contains("Esqueci minha senha"), "mensagem deve orientar a definir senha");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Login em conta com senha local deve seguir para a autenticação normal")
    void loginComContaLocalNaoDeveSerBloqueado() {
        Usuario contaLocal = Usuario.builder()
                .id(7L)
                .nome("Maria")
                .email("maria@example.com")
                .senha("$2a$10$hashBcrypt")
                .tipoUsuario(TipoUsuario.CONTRATANTE)
                .ativo(true)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("maria@example.com")).thenReturn(Optional.of(contaLocal));
        // authenticate falha para encerrar o fluxo sem montar a sessão completa
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        LoginRequestDTO request = new LoginRequestDTO("maria@example.com", "senhaErrada");

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authSessionService.login(request, servletRequest));

        verify(authenticationManager).authenticate(any());
    }
}
