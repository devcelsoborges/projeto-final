package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.CPFAlreadyInUseException;
import ads.uninassau.brjobs.exception.InvalidPasswordException;
import ads.uninassau.brjobs.exception.UserNotFoundException;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.validator.UsuarioValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService Tests")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioValidator usuarioValidator;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João Silva");
        usuario.setEmail("joao@example.com");
        usuario.setSenha("EncodedPassword123");
        usuario.setCpf("12345678900");
        usuario.setTelefone("(11) 99999-9999");
        usuario.setTipo(TipoUsuario.CONTRATANTE);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDateTime.now());

        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(1L);
        usuarioDTO.setNome("João Silva");
        usuarioDTO.setEmail("joao@example.com");
        usuarioDTO.setCpf("12345678900");
        usuarioDTO.setTipo(TipoUsuario.CONTRATANTE);
    }

    @Test
    @DisplayName("Deve criar usuário com sucesso")
    void testCriarUsuarioComSucesso() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("EncodedPassword123");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        Usuario resultado = usuarioService.criarUsuario("João Silva", "joao@example.com",
                                                        "Senha123@", "12345678900",
                                                        "(11) 99999-9999", TipoUsuario.CONTRATANTE);

        // Assert
        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        assertEquals("joao@example.com", resultado.getEmail());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void testCriarUsuarioComEmailDuplicado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.criarUsuario("João Silva", "joao@example.com",
                                       "Senha123@", "12345678900",
                                       "(11) 99999-9999", TipoUsuario.CONTRATANTE);
        });
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF já existe")
    void testCriarUsuarioComCPFDuplicado() {
        // Arrange
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByCpf(anyString())).thenReturn(Optional.of(usuario));

        // Act & Assert
        assertThrows(CPFAlreadyInUseException.class, () -> {
            usuarioService.criarUsuario("João Silva", "joao@example.com",
                                       "Senha123@", "12345678900",
                                       "(11) 99999-9999", TipoUsuario.CONTRATANTE);
        });
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void testBuscarUsuarioPorIdComSucesso() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // Act
        Usuario resultado = usuarioService.obterPorId(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("João Silva", resultado.getNome());
        verify(usuarioRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado")
    void testBuscarUsuarioNaoEncontrado() {
        // Arrange
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            usuarioService.obterPorId(999L);
        });
    }

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void testAtualizarUsuarioComSucesso() {
        // Arrange
        Usuario usuarioAtualizado = new Usuario();
        usuarioAtualizado.setId(1L);
        usuarioAtualizado.setNome("João Silva Atualizado");
        usuarioAtualizado.setEmail("joao@example.com");
        usuarioAtualizado.setTelefone("(11) 98888-8888");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioAtualizado);

        // Act
        Usuario resultado = usuarioService.atualizar(1L, usuarioAtualizado);

        // Assert
        assertNotNull(resultado);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve deletar usuário com sucesso")
    void testDeletarUsuarioComSucesso() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // Act
        usuarioService.deletar(1L);

        // Assert
        verify(usuarioRepository, times(1)).deleteById(1L);
    }
}

