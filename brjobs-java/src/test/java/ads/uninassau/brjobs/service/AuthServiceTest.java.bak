package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.exception.InvalidPasswordException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João Silva");
        usuario.setEmail("joao@example.com");
        usuario.setSenha("EncodedPassword123");
        usuario.setTipo(TipoUsuario.CONTRATANTE);
        usuario.setAtivo(true);
    }

    @Test
    @DisplayName("Deve fazer login com credenciais válidas")
    void testLoginComCredenciaisValidas() {
        // Arrange
        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha123@", "EncodedPassword123")).thenReturn(true);

        // Act
        Usuario resultado = authService.autenticar("joao@example.com", "Senha123@");

        // Assert
        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(usuarioRepository, times(1)).findByEmail("joao@example.com");
    }

    @Test
    @DisplayName("Deve lançar exceção com senha inválida")
    void testLoginComSenhaInvalida() {
        // Arrange
        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("SenhaErrada123@", "EncodedPassword123")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            authService.autenticar("joao@example.com", "SenhaErrada123@");
        });
    }

    @Test
    @DisplayName("Deve lançar exceção com email não encontrado")
    void testLoginComEmailNaoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.autenticar("notfound@example.com", "Senha123@");
        });
    }

    @Test
    @DisplayName("Deve validar token com sucesso")
    void testValidarTokenComSucesso() {
        // Arrange
        String token = "validToken";

        // Act
        boolean resultado = authService.validarToken(token);

        // Assert
        // Implementar conforme sua lógica de validação
    }

    @Test
    @DisplayName("Deve rejeitar token inválido")
    void testRejeiarTokenInvalido() {
        // Arrange
        String token = "invalidToken";

        // Act
        boolean resultado = authService.validarToken(token);

        // Assert
        assertFalse(resultado);
    }
}

