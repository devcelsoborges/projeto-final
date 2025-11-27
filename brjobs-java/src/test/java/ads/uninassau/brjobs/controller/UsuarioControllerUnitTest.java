package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController Unit Tests")
class UsuarioControllerUnitTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João Silva");
        usuario.setEmail("joao@example.com");
        usuario.setCpf("12345678900");
        usuario.setTelefone("(11) 99999-9999");
        usuario.setTipo(TipoUsuario.CONTRATANTE);
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve retornar lista de usuários com sucesso")
    void testListarUsuariosComSucesso() {
        // Arrange
        List<Usuario> usuarios = Arrays.asList(usuario);
        when(usuarioService.listar()).thenReturn(usuarios);

        // Act
        ResponseEntity<?> response = usuarioController.listar();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(usuarioService, times(1)).listar();
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void testBuscarPorIdComSucesso() {
        // Arrange
        when(usuarioService.obterPorId(1L)).thenReturn(usuario);

        // Act
        ResponseEntity<?> response = usuarioController.obterPorId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(usuarioService, times(1)).obterPorId(1L);
    }

    @Test
    @DisplayName("Deve criar novo contratante com sucesso")
    void testCriarContratanteComSucesso() {
        // Arrange
        when(usuarioService.criarUsuario(anyString(), anyString(), anyString(),
                                         anyString(), anyString(), any(TipoUsuario.class)))
                .thenReturn(usuario);

        // Act
        ResponseEntity<?> response = usuarioController.criarContratante(usuario);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(usuarioService, times(1)).criarUsuario(anyString(), anyString(), anyString(),
                                                       anyString(), anyString(), any(TipoUsuario.class));
    }

    @Test
    @DisplayName("Deve criar novo prestador com sucesso")
    void testCriarPrestadorComSucesso() {
        // Arrange
        usuario.setTipo(TipoUsuario.PRESTADOR);
        when(usuarioService.criarUsuario(anyString(), anyString(), anyString(),
                                         anyString(), anyString(), any(TipoUsuario.class)))
                .thenReturn(usuario);

        // Act
        ResponseEntity<?> response = usuarioController.criarPrestador(usuario);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(usuarioService, times(1)).criarUsuario(anyString(), anyString(), anyString(),
                                                       anyString(), anyString(), any(TipoUsuario.class));
    }

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void testAtualizarComSucesso() {
        // Arrange
        Usuario usuarioAtualizado = new Usuario();
        usuarioAtualizado.setNome("João Silva Atualizado");
        when(usuarioService.atualizar(1L, usuarioAtualizado)).thenReturn(usuarioAtualizado);

        // Act
        ResponseEntity<?> response = usuarioController.atualizar(1L, usuarioAtualizado);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(usuarioService, times(1)).atualizar(1L, usuarioAtualizado);
    }

    @Test
    @DisplayName("Deve deletar usuário com sucesso")
    void testDeletarComSucesso() {
        // Act
        ResponseEntity<?> response = usuarioController.deletar(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(usuarioService, times(1)).deletar(1L);
    }

    @Test
    @DisplayName("Deve buscar usuário por email com sucesso")
    void testBuscarPorEmailComSucesso() {
        // Arrange
        when(usuarioService.obterPorEmail("joao@example.com")).thenReturn(usuario);

        // Act
        ResponseEntity<?> response = usuarioController.obterPorEmail("joao@example.com");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(usuarioService, times(1)).obterPorEmail("joao@example.com");
    }
}

