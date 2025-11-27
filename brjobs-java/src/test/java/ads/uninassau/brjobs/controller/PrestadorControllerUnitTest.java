package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.model.Prestador;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.service.PrestadorService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrestadorController Unit Tests")
class PrestadorControllerUnitTest {

    @Mock
    private PrestadorService prestadorService;

    @InjectMocks
    private PrestadorController prestadorController;

    private Prestador prestador;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Maria Santos");
        usuario.setEmail("maria@example.com");
        usuario.setTipo(TipoUsuario.PRESTADOR);

        prestador = new Prestador();
        prestador.setId(1L);
        prestador.setUsuario(usuario);
        prestador.setFuncao("Encanador");
        prestador.setDescricao("Serviços de encanamento residencial");
        prestador.setNotaMedia(4.5f);
        prestador.setQuantidadeAvaliacoes(10);
        prestador.setAtivo(true);
        prestador.setDataCadastro(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve listar todos os prestadores com sucesso")
    void testListarPrestadoresComSucesso() {
        // Arrange
        List<Prestador> prestadores = Arrays.asList(prestador);
        when(prestadorService.listar()).thenReturn(prestadores);

        // Act
        ResponseEntity<?> response = prestadorController.listar();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(prestadorService, times(1)).listar();
    }

    @Test
    @DisplayName("Deve buscar prestador por ID com sucesso")
    void testBuscarPorIdComSucesso() {
        // Arrange
        when(prestadorService.obterPorId(1L)).thenReturn(prestador);

        // Act
        ResponseEntity<?> response = prestadorController.obterPorId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(prestadorService, times(1)).obterPorId(1L);
    }

    @Test
    @DisplayName("Deve criar novo prestador com sucesso")
    void testCriarPrestadorComSucesso() {
        // Arrange
        when(prestadorService.criar(anyLong(), any(Prestador.class))).thenReturn(prestador);

        // Act
        ResponseEntity<?> response = prestadorController.criar(1L, prestador);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(prestadorService, times(1)).criar(anyLong(), any(Prestador.class));
    }

    @Test
    @DisplayName("Deve atualizar prestador com sucesso")
    void testAtualizarComSucesso() {
        // Arrange
        Prestador prestadorAtualizado = new Prestador();
        prestadorAtualizado.setFuncao("Encanador Senior");
        when(prestadorService.atualizar(1L, prestadorAtualizado)).thenReturn(prestadorAtualizado);

        // Act
        ResponseEntity<?> response = prestadorController.atualizar(1L, prestadorAtualizado);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(prestadorService, times(1)).atualizar(1L, prestadorAtualizado);
    }

    @Test
    @DisplayName("Deve buscar prestador por usuário com sucesso")
    void testBuscarPorUsuarioComSucesso() {
        // Arrange
        when(prestadorService.obterPorUsuarioId(1L)).thenReturn(prestador);

        // Act
        ResponseEntity<?> response = prestadorController.obterPorUsuario(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(prestadorService, times(1)).obterPorUsuarioId(1L);
    }

    @Test
    @DisplayName("Deve desativar prestador com sucesso")
    void testDesativarComSucesso() {
        // Arrange
        prestador.setAtivo(false);
        when(prestadorService.desativar(1L)).thenReturn(prestador);

        // Act
        ResponseEntity<?> response = prestadorController.desativar(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(prestadorService, times(1)).desativar(1L);
    }

    @Test
    @DisplayName("Deve ativar prestador com sucesso")
    void testAtivarComSucesso() {
        // Arrange
        prestador.setAtivo(true);
        when(prestadorService.ativar(1L)).thenReturn(prestador);

        // Act
        ResponseEntity<?> response = prestadorController.ativar(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(prestadorService, times(1)).ativar(1L);
    }
}

