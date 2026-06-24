package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountActivationService - confirmação de cadastro")
class AccountActivationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AccountActivationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "templateId", "account-activation");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "https://brjobs.com.br");
    }

    @Test
    @DisplayName("enviarConfirmacao gera token, marca não confirmado e envia template com confirm_url")
    void enviarConfirmacaoGeraTokenEEnviaTemplate() {
        Usuario u = Usuario.builder().id(7L).nome("Joao Silva").email("joao@example.com").build();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mailService.sendTemplate(anyString(), anyString(), anyString(), any(), isNull())).thenReturn(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);

        service.enviarConfirmacao(u);

        assertFalse(u.getEmailConfirmado(), "deve nascer não confirmado");
        assertTrue(u.getEmailConfirmationToken() != null && !u.getEmailConfirmationToken().isBlank());
        assertTrue(u.getEmailConfirmationExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));

        verify(mailService).sendTemplate(eq("joao@example.com"), eq("account-activation"),
                anyString(), vars.capture(), isNull());
        assertTrue(((String) vars.getValue().get("confirm_url")).contains(u.getEmailConfirmationToken()));
        assertTrue(((String) vars.getValue().get("confirm_url")).startsWith("https://brjobs.com.br/confirmar-email?token="));
        // first_name = primeiro nome
        org.junit.jupiter.api.Assertions.assertEquals("Joao", vars.getValue().get("first_name"));
    }

    @Test
    @DisplayName("confirmar com token válido marca confirmado e limpa o token")
    void confirmarTokenValido() {
        Usuario u = Usuario.builder().id(7L).email("joao@example.com")
                .emailConfirmado(false)
                .emailConfirmationToken("tok123")
                .emailConfirmationExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(usuarioRepository.findByEmailConfirmationToken("tok123")).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean ok = service.confirmar("tok123");

        assertTrue(ok);
        assertTrue(u.getEmailConfirmado());
        assertNull(u.getEmailConfirmationToken());
        assertNull(u.getEmailConfirmationExpiresAt());
    }

    @Test
    @DisplayName("confirmar com token expirado retorna false e não confirma")
    void confirmarTokenExpirado() {
        Usuario u = Usuario.builder().id(7L)
                .emailConfirmado(false)
                .emailConfirmationToken("tok123")
                .emailConfirmationExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(usuarioRepository.findByEmailConfirmationToken("tok123")).thenReturn(Optional.of(u));

        boolean ok = service.confirmar("tok123");

        assertFalse(ok);
        assertFalse(u.getEmailConfirmado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmar com token inexistente ou em branco retorna false")
    void confirmarTokenInvalido() {
        assertFalse(service.confirmar(null));
        assertFalse(service.confirmar("   "));
        when(usuarioRepository.findByEmailConfirmationToken("xxx")).thenReturn(Optional.empty());
        assertFalse(service.confirmar("xxx"));
    }
}
