package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.AvaliacaoDTO;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * Cria avaliação com filtro de palavrões e validação por tenant
     * POST /api/v1/avaliacoes
     * Body: { "prestadorId": 5, "nota": 5, "comentario": "Ótimo trabalho!" }
     */
    @PostMapping("/v1")
    @ValidateTenant
    public ResponseEntity<AvaliacaoDTO> criarAvaliacaoComValidacao(
        @RequestAttribute("tenant_id") Long tenantId,
        @RequestBody AvaliacaoDTO dto
    ) {
        AvaliacaoDTO novaAvaliacao = avaliacaoService.criarComValidacao(
            tenantId,
            dto.getPrestadorId(),
            dto.getNota(),
            dto.getComentario()
        );
        return new ResponseEntity<>(novaAvaliacao, HttpStatus.CREATED);
    }

    /**
     * Lista avaliações recebidas pelo usuário logado (prestador)
     * GET /api/v1/avaliacoes/recebidas
     */
    @GetMapping("/v1/recebidas")
    @ValidateTenant
    public ResponseEntity<List<AvaliacaoDTO>> listarRecebidas(
        @RequestAttribute("tenant_id") Long tenantId
    ) {
        List<AvaliacaoDTO> avaliacoes = avaliacaoService.listarAvaliacoesRecebidas(tenantId);
        return ResponseEntity.ok(avaliacoes);
    }

    /**
     * Obtém média e contagem de avaliações de um prestador
     * GET /api/v1/avaliacoes/prestador/:prestadorId/stats
     */
    @GetMapping("/v1/prestador/{prestadorId}/stats")
    public ResponseEntity<?> obterStats(@PathVariable Long prestadorId) {
        Double media = avaliacaoService.obterMedia(prestadorId);
        Long count = avaliacaoService.contarAvaliacoes(prestadorId);
        return ResponseEntity.ok(new Object() {
            public final Double media_avaliacao = media != null ? media : 0.0;
            public final Long total_avaliacoes = count != null ? count : 0L;
        });
    }
}

