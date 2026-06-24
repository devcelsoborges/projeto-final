package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.CadastroContratanteDTO;
import ads.uninassau.brjobs.dto.CadastroPrestadorDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.EmailAlreadyInUseException;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unificação de contas por e-mail no cadastro local.
 *
 * Regra de segurança: o cadastro local NÃO pode definir senha numa conta existente
 * (própria ou criada via login social) — isso seria account takeover por um endpoint
 * público sem prova de posse do e-mail. Logo, e-mail já existente -> 409. A forma de
 * uma conta social ganhar senha local é "Esqueci minha senha" (verifica o e-mail).
 * O que esta classe garante: e-mail é normalizado e a unicidade é case-insensitive,
 * então o mesmo e-mail nunca gera uma segunda conta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unificação de contas por e-mail (cadastro local)")
class UsuarioServiceContaUnificadaTest {

    private static final String EMAIL = "joao@example.com";
    private static final String SENHA_VALIDA = "Senha@123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountActivationService accountActivationService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario contaSocial;

    @BeforeEach
    void setUp() {
        // Conta criada por login com Google: só nome/email, senha placeholder
        contaSocial = Usuario.builder()
                .id(42L)
                .tipoUsuario(TipoUsuario.CONTRATANTE)
                .nome("Joao do Google")
                .email(EMAIL)
                .senha("OAUTH2_GOOGLE")
                .ativo(true)
                .build();
    }

    private CadastroContratanteDTO cadastroContratante() {
        CadastroContratanteDTO dto = new CadastroContratanteDTO();
        dto.setNome("João da Silva");
        dto.setEmail(EMAIL);
        dto.setSenha(SENHA_VALIDA);
        dto.setConfirmacaoSenha(SENHA_VALIDA);
        dto.setTelefone("(81) 99999-0000");
        return dto;
    }

    @Test
    @DisplayName("Cadastro com e-mail de conta social existente deve falhar com 409 (não pode setar senha na conta alheia)")
    void cadastroComEmailDeContaSocialDeveLancar409() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(contaSocial));

        assertThrows(EmailAlreadyInUseException.class,
                () -> usuarioService.criarContratante(cadastroContratante()));

        // não deve tocar/sobrescrever a conta existente
        verify(usuarioRepository, never()).save(any());
        assertEquals("OAUTH2_GOOGLE", contaSocial.getSenha(), "senha da conta social não pode ser alterada");
        assertTrue(contaSocial.isSomenteLoginSocial());
    }

    @Test
    @DisplayName("Cadastro com e-mail de conta local existente deve falhar com 409")
    void cadastroComEmailDeContaLocalDeveLancar409() {
        contaSocial.setSenha("$2a$10$hashBcryptDeContaLocal");
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(contaSocial));

        assertThrows(EmailAlreadyInUseException.class,
                () -> usuarioService.criarContratante(cadastroContratante()));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("E-mail com caixa/espacos diferentes deve detectar a conta existente (case-insensitive, sem duplicar)")
    void cadastroComEmailCaixaDiferenteDeveDetectarExistente() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(contaSocial));

        CadastroContratanteDTO dto = cadastroContratante();
        dto.setEmail("  JOAO@Example.COM  "); // mesmo e-mail, caixa e espacos diferentes

        assertThrows(EmailAlreadyInUseException.class,
                () -> usuarioService.criarContratante(dto));

        // a checagem de unicidade usa o e-mail normalizado (minúsculo, sem espaços)
        verify(usuarioRepository).findByEmailIgnoreCase(EMAIL);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cadastro novo deve persistir o e-mail normalizado (minusculo e sem espacos)")
    void cadastroNovoDevePersistirEmailNormalizado() {
        when(usuarioRepository.findByEmailIgnoreCase("novo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("$2a$10$hashDaSenha");

        CadastroContratanteDTO dto = cadastroContratante();
        dto.setEmail("  Novo@Example.com ");

        UsuarioDTO resultado = usuarioService.criarContratante(dto);

        assertEquals("novo@example.com", resultado.getEmail());
    }

    @Test
    @DisplayName("Cadastro de prestador com e-mail novo deve criar conta PRESTADOR normalizada")
    void cadastroPrestadorNovoDeveCriarContaNormalizada() {
        when(usuarioRepository.findByEmailIgnoreCase("maria@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("$2a$10$hashDaSenha");

        CadastroPrestadorDTO dto = new CadastroPrestadorDTO();
        dto.setNome("Maria Prestadora");
        dto.setEmail("MARIA@example.com");
        dto.setSenha(SENHA_VALIDA);
        dto.setConfirmacaoSenha(SENHA_VALIDA);

        UsuarioDTO resultado = usuarioService.criarPrestador(dto);

        assertEquals("maria@example.com", resultado.getEmail());
        assertEquals(TipoUsuario.PRESTADOR, resultado.getTipoUsuario());
    }

    @Test
    @DisplayName("isSomenteLoginSocial deve reconhecer placeholders sociais e hash BCrypt")
    void isSomenteLoginSocialDeveClassificarCorretamente() {
        assertTrue(Usuario.builder().senha("OAUTH2_GOOGLE").build().isSomenteLoginSocial());
        assertTrue(Usuario.builder().senha("OAUTH2_FACEBOOK").build().isSomenteLoginSocial());
        assertTrue(Usuario.builder().senha("SOCIAL_LOGIN").build().isSomenteLoginSocial());
        assertTrue(Usuario.builder().build().isSomenteLoginSocial());
        assertFalse(Usuario.builder().senha("$2a$10$hashBcryptQualquer").build().isSomenteLoginSocial());
    }
}
