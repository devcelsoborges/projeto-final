package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.model.*;
import ads.uninassau.brjobs.service.AvaliacaoService;
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
@DisplayName("AvaliacaoController Unit Tests")
class AvaliacaoControllerUnitTest {

    @Mock
    private AvaliacaoService avaliacaoService;

    @InjectMocks
    private AvaliacaoController avaliacaoController;

    private Avaliacao avaliacao;
    private Prestador prestador;
    private Usuario usuarioPrestador;
    private Usuario usuarioContratante;

    @BeforeEach
    void setUp() {
        // Setup Usuario Prestador
        usuarioPrestador = new Usuario();
        usuarioPrestador.setId(1L);
        usuarioPrestador.setNome("Maria Santos");
        usuarioPrestador.setEmail("maria@example.com");
        usuarioPrestador.setTipo(TipoUsuario.PRESTADOR);

        // Setup Prestador
        prestador = new Prestador();
        prestador.setId(1L);
        prestador.setUsuario(usuarioPrestador);
        prestador.setFuncao("Encanador");
        prestador.setNotaMedia(4.5f);
        prestador.setQuantidadeAvaliacoes(10);

        // Setup Usuario Contratante
        usuarioContratante = new Usuario();
        usuarioContratante.setId(2L);
        usuarioContratante.setNome("João Silva");
        usuarioContratante.setEmail("joao@example.com");
        usuarioContratante.setTipo(TipoUsuario.CONTRATANTE);

        // Setup Avaliação
        avaliacao = new Avaliacao();
        avaliacao.setId(1L);
        avaliacao.setNota(5);
        avaliacao.setComentario("Excelente trabalho!");
        avaliacao.setPrestador(prestador);
        avaliacao.setUsuario(usuarioContratante);
        avaliacao.setDataCriacao(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve listar todas as avaliações com sucesso")
    void testListarAvaliacoesComSucesso() {
        // Arrange
        List<Avaliacao> avaliacoes = Arrays.asList(avaliacao);
        when(avaliacaoService.listar()).thenReturn(avaliacoes);

        // Act
        ResponseEntity<?> response = avaliacaoController.listar();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(avaliacaoService, times(1)).listar();
    }

    @Test
    @DisplayName("Deve buscar avaliação por ID com sucesso")
    void testBuscarPorIdComSucesso() {
        // Arrange
        when(avaliacaoService.obterPorId(1L)).thenReturn(avaliacao);

        // Act
        ResponseEntity<?> response = avaliacaoController.obterPorId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(avaliacaoService, times(1)).obterPorId(1L);
    }

    @Test
    @DisplayName("Deve criar avaliação com sucesso")
    void testCriarAvaliacaoComSucesso() {
        // Arrange
        when(avaliacaoService.criar(any(Avaliacao.class), any(Prestador.class),
                                    any(Usuario.class), any()))
                .thenReturn(avaliacao);

        // Act
        ResponseEntity<?> response = avaliacaoController.criar(avaliacao);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(avaliacaoService, times(1)).criar(any(Avaliacao.class), any(Prestador.class),
                                                   any(Usuario.class), any());
    }

    @Test
    @DisplayName("Deve atualizar avaliação com sucesso")
    void testAtualizarAvaliacaoComSucesso() {
        // Arrange
        Avaliacao avaliacaoAtualizada = new Avaliacao();
        avaliacaoAtualizada.setId(1L);
        avaliacaoAtualizada.setNota(4);
        avaliacaoAtualizada.setComentario("Muito bom!");
        when(avaliacaoService.atualizar(1L, avaliacaoAtualizada)).thenReturn(avaliacaoAtualizada);

        // Act
        ResponseEntity<?> response = avaliacaoController.atualizar(1L, avaliacaoAtualizada);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(avaliacaoService, times(1)).atualizar(1L, avaliacaoAtualizada);
    }

    @Test
    @DisplayName("Deve deletar avaliação com sucesso")
    void testDeletarAvaliacaoComSucesso() {
        // Act
        ResponseEntity<?> response = avaliacaoController.deletar(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(avaliacaoService, times(1)).deletar(1L);
    }

    @Test
    @DisplayName("Deve buscar avaliações por prestador com sucesso")
    void testBuscarPorPrestadorComSucesso() {
        // Arrange
        List<Avaliacao> avaliacoes = Arrays.asList(avaliacao);
        when(avaliacaoService.obterPorPrestador(1L)).thenReturn(avaliacoes);

        // Act
        ResponseEntity<?> response = avaliacaoController.obterPorPrestador(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(avaliacaoService, times(1)).obterPorPrestador(1L);
    }

    @Test
    @DisplayName("Deve buscar avaliações por usuário com sucesso")
    void testBuscarPorUsuarioComSucesso() {
        // Arrange
        List<Avaliacao> avaliacoes = Arrays.asList(avaliacao);
        when(avaliacaoService.obterPorUsuario(2L)).thenReturn(avaliacoes);

        // Act
        ResponseEntity<?> response = avaliacaoController.obterPorUsuario(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(avaliacaoService, times(1)).obterPorUsuario(2L);
    }
}

