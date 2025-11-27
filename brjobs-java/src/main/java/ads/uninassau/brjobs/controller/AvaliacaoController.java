package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.AvaliacaoDTO;
import ads.uninassau.brjobs.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsável pelas operações de Avaliação.
 * Permite criar, atualizar, deletar e consultar avaliações de prestadores.
 */
@RestController
@RequestMapping("/api/avaliacoes")
@CrossOrigin(origins = "http://localhost:4200")
public class AvaliacaoController {

    @Autowired
    private AvaliacaoService avaliacaoService;

    /**
     * Cria uma nova avaliação para um prestador
     * POST /api/avaliacoes
     */
    @PostMapping
    public ResponseEntity<AvaliacaoDTO> criarAvaliacao(@Valid @RequestBody AvaliacaoDTO avaliacaoDTO) {
        AvaliacaoDTO avaliacao = avaliacaoService.criarAvaliacao(avaliacaoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacao);
    }

    /**
     * Lista todas as avaliações
     * GET /api/avaliacoes
     */
    @GetMapping
    public ResponseEntity<List<AvaliacaoDTO>> listarAvaliacoes() {
        List<AvaliacaoDTO> avaliacoes = avaliacaoService.listarAvaliacoes();
        return ResponseEntity.ok(avaliacoes);
    }

    /**
     * Busca uma avaliação específica por ID
     * GET /api/avaliacoes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoDTO> buscarPorId(@PathVariable Long id) {
        AvaliacaoDTO avaliacao = avaliacaoService.buscarPorId(id);
        return ResponseEntity.ok(avaliacao);
    }

    /**
     * Lista todas as avaliações de um prestador específico
     * GET /api/avaliacoes/prestador/{prestadorId}
     */
    @GetMapping("/prestador/{prestadorId}")
    public ResponseEntity<List<AvaliacaoDTO>> listarAvaliacoesPorPrestador(@PathVariable Long prestadorId) {
        List<AvaliacaoDTO> avaliacoes = avaliacaoService.listarAvaliacoesPorPrestador(prestadorId);
        return ResponseEntity.ok(avaliacoes);
    }

    /**
     * Lista todas as avaliações feitas por um usuário
     * GET /api/avaliacoes/usuario/{usuarioId}
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<AvaliacaoDTO>> listarAvaliacoesPorUsuario(@PathVariable Long usuarioId) {
        List<AvaliacaoDTO> avaliacoes = avaliacaoService.listarAvaliacoesPorUsuario(usuarioId);
        return ResponseEntity.ok(avaliacoes);
    }

    /**
     * Atualiza uma avaliação existente
     * PUT /api/avaliacoes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AvaliacaoDTO> atualizarAvaliacao(@PathVariable Long id, @Valid @RequestBody AvaliacaoDTO avaliacaoDTO) {
        AvaliacaoDTO avaliacao = avaliacaoService.atualizarAvaliacao(id, avaliacaoDTO);
        return ResponseEntity.ok(avaliacao);
    }

    /**
     * Deleta uma avaliação
     * DELETE /api/avaliacoes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAvaliacao(@PathVariable Long id) {
        avaliacaoService.deletarAvaliacao(id);
        return ResponseEntity.noContent().build();
    }
}

