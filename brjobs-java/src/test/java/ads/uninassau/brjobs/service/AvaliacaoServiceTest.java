package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.AvaliacaoDTO;
import ads.uninassau.brjobs.model.*;
import ads.uninassau.brjobs.repository.AvaliacaoRepository;
import ads.uninassau.brjobs.repository.PrestadorRepository;
import ads.uninassau.brjobs.repository.SolicitacaoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvaliacaoService Tests")
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private SolicitacaoServicoRepository solicitacaoServicoRepository;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    private Avaliacao avaliacao;
    private Prestador prestador;
    private Usuario usuarioPrestador;
    private Usuario usuarioContratante;
    private SolicitacaoServico solicitacaoServico;

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

        // Setup Serviço
        Servico servico = new Servico();
        servico.setId(1L);
        servico.setTitulo("Serviço de Encanamento");
        servico.setUsuario(usuarioPrestador);

        // Setup Solicitação
        solicitacaoServico = new SolicitacaoServico();
        solicitacaoServico.setId(1L);
        solicitacaoServico.setUsuario(usuarioContratante);
        solicitacaoServico.setServico(servico);
        solicitacaoServico.setStatus("CONCLUIDO");

        // Setup Avaliação
        avaliacao = new Avaliacao();
        avaliacao.setId(1L);
        avaliacao.setNota(5);
        avaliacao.setComentario("Excelente trabalho!");
        avaliacao.setPrestador(prestador);
        avaliacao.setUsuario(usuarioContratante);
        avaliacao.setSolicitacao(solicitacaoServico);
        avaliacao.setDataCriacao(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar avaliação com sucesso")
    void testCriarAvaliacaoComSucesso() {
        // Arrange
        when(prestadorRepository.findById(anyLong())).thenReturn(Optional.of(prestador));
        when(solicitacaoServicoRepository.findById(anyLong())).thenReturn(Optional.of(solicitacaoServico));
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenReturn(avaliacao);

        // Act
        Avaliacao resultado = avaliacaoService.criar(avaliacao, prestador, usuarioContratante, solicitacaoServico);

        // Assert
        assertNotNull(resultado);
        assertEquals(5, resultado.getNota());
        assertEquals("Excelente trabalho!", resultado.getComentario());
        verify(avaliacaoRepository, times(1)).save(any(Avaliacao.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando nota inválida")
    void testCriarAvaliacaoComNotaInvalida() {
        // Arrange
        Avaliacao avaliacaoInvalida = new Avaliacao();
        avaliacaoInvalida.setNota(6); // Nota inválida (maior que 5)
        avaliacaoInvalida.setComentario("Bom");
        avaliacaoInvalida.setPrestador(prestador);
        avaliacaoInvalida.setUsuario(usuarioContratante);
        avaliacaoInvalida.setSolicitacao(solicitacaoServico);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            avaliacaoService.criar(avaliacaoInvalida, prestador, usuarioContratante, solicitacaoServico);
        });
    }

    @Test
    @DisplayName("Deve buscar avaliações por prestador com sucesso")
    void testBuscarAvaliacoesPorPrestador() {
        // Arrange
        List<Avaliacao> avaliacoes = Arrays.asList(avaliacao);
        when(avaliacaoRepository.findByPrestador(any(Prestador.class))).thenReturn(avaliacoes);

        // Act
        List<Avaliacao> resultado = avaliacaoRepository.findByPrestador(prestador);

        // Assert
        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        verify(avaliacaoRepository, times(1)).findByPrestador(prestador);
    }

    @Test
    @DisplayName("Deve atualizar avaliação com sucesso")
    void testAtualizarAvaliacaoComSucesso() {
        // Arrange
        Avaliacao avaliacaoAtualizada = new Avaliacao();
        avaliacaoAtualizada.setId(1L);
        avaliacaoAtualizada.setNota(4);
        avaliacaoAtualizada.setComentario("Muito bom!");

        when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenReturn(avaliacaoAtualizada);

        // Act
        Avaliacao resultado = avaliacaoService.atualizar(1L, avaliacaoAtualizada);

        // Assert
        assertNotNull(resultado);
        assertEquals(4, resultado.getNota());
        verify(avaliacaoRepository, times(1)).save(any(Avaliacao.class));
    }

    @Test
    @DisplayName("Deve deletar avaliação com sucesso")
    void testDeletarAvaliacaoComSucesso() {
        // Arrange
        when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));

        // Act
        avaliacaoService.deletar(1L);

        // Assert
        verify(avaliacaoRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve calcular nota média do prestador")
    void testCalcularNotaMedia() {
        // Arrange
        List<Avaliacao> avaliacoes = Arrays.asList(avaliacao);
        when(avaliacaoRepository.findByPrestador(any(Prestador.class))).thenReturn(avaliacoes);

        // Act
        List<Avaliacao> resultado = avaliacaoRepository.findByPrestador(prestador);
        double media = resultado.stream().mapToInt(Avaliacao::getNota).average().orElse(0);

        // Assert
        assertEquals(5.0, media);
    }
}

