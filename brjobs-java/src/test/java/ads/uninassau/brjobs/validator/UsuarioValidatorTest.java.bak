package ads.uninassau.brjobs.validator;

import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioValidator Tests")
class UsuarioValidatorTest {

    @InjectMocks
    private UsuarioValidator usuarioValidator;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setNome("João Silva");
        usuario.setEmail("joao@example.com");
        usuario.setCpf("12345678900");
        usuario.setTelefone("(11) 99999-9999");
        usuario.setSenha("Senha123@");
        usuario.setTipo(TipoUsuario.CONTRATANTE);
    }

    @Test
    @DisplayName("Deve validar email válido")
    void testValidarEmailValido() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarEmail("joao@example.com");

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar email inválido")
    void testValidarEmailInvalido() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarEmail("emailinvalido");

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve validar CPF válido")
    void testValidarCPFValido() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarCPF("12345678900");

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar CPF inválido")
    void testValidarCPFInvalido() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarCPF("12345678");

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve validar telefone válido")
    void testValidarTelefoneValido() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarTelefone("(11) 99999-9999");

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar telefone inválido")
    void testValidarTelefoneInvalido() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarTelefone("123");

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve validar senha forte")
    void testValidarSenhaForte() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarSenha("SenhaForte123@");

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar senha fraca")
    void testValidarSenhaFraca() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarSenha("123");

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve validar usuário completo")
    void testValidarUsuarioCompleto() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarUsuario(usuario);

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar usuário com nome vazio")
    void testValidarUsuarioComNomeVazio() {
        // Arrange
        usuario.setNome("");

        // Act
        boolean resultado = usuarioValidator.validarUsuario(usuario);

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve validar idade mínima (18 anos)")
    void testValidarIdadeMinima() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarIdade(18);

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar idade menor que 18 anos")
    void testValidarIdadeMenor() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarIdade(17);

        // Assert
        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve validar idade máxima (120 anos)")
    void testValidarIdadeMaxima() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarIdade(120);

        // Assert
        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve rejeitar idade maior que 120 anos")
    void testValidarIdadeMaior() {
        // Arrange & Act
        boolean resultado = usuarioValidator.validarIdade(121);

        // Assert
        assertFalse(resultado);
    }
}

