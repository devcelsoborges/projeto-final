package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.CadastroPrestadorDTO;
import ads.uninassau.brjobs.dto.PrestadorResponseDTO;
import ads.uninassau.brjobs.service.PrestadorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller responsável pelas operações específicas de Prestador.
 * Um Prestador está sempre associado a um usuário com tipoUsuario == PRESTADOR.
 */
@RestController
@RequestMapping("/api/prestadores")
@CrossOrigin(origins = "http://localhost:4200")
public class PrestadorController {

    @Autowired
    private PrestadorService prestadorService;

    /**
     * Cria um registro de prestador para um usuário existente
     * POST /api/prestadores/{usuarioId}
     *
     * O usuário deve ter sido criado previamente via POST /api/usuarios/prestador
     */
    @PostMapping("/{usuarioId}")
    public ResponseEntity<PrestadorResponseDTO> criarPrestador(
            @PathVariable Long usuarioId,
            @Valid @RequestBody CadastroPrestadorDTO dto,
            @RequestParam(required = false) MultipartFile curriculo) {

        PrestadorResponseDTO prestador = prestadorService.criarPrestador(usuarioId, dto, curriculo);
        return ResponseEntity.status(HttpStatus.CREATED).body(prestador);
    }

    /**
     * Lista todos os prestadores ativos
     * GET /api/prestadores
     */
    @GetMapping
    public ResponseEntity<List<PrestadorResponseDTO>> listarPrestadoresAtivos() {
        List<PrestadorResponseDTO> prestadores = prestadorService.listarPrestadoresAtivos();
        return ResponseEntity.ok(prestadores);
    }

    /**
     * Busca um prestador por ID
     * GET /api/prestadores/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PrestadorResponseDTO> buscarPorId(@PathVariable Long id) {
        PrestadorResponseDTO prestador = prestadorService.buscarPorId(id);
        return ResponseEntity.ok(prestador);
    }

    /**
     * Busca prestador de um usuário específico
     * GET /api/prestadores/usuario/{usuarioId}
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<PrestadorResponseDTO> buscarPorUsuarioId(@PathVariable Long usuarioId) {
        PrestadorResponseDTO prestador = prestadorService.buscarPorUsuarioId(usuarioId);
        return ResponseEntity.ok(prestador);
    }

    /**
     * Atualiza dados do prestador
     * PUT /api/prestadores/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PrestadorResponseDTO> atualizarPrestador(
            @PathVariable Long id,
            @Valid @RequestBody CadastroPrestadorDTO dto) {

        PrestadorResponseDTO prestador = prestadorService.atualizarPrestador(id, dto);
        return ResponseEntity.ok(prestador);
    }

    /**
     * Atualiza o currículo do prestador
     * POST /api/prestadores/{id}/curriculo (multipart/form-data)
     */
    @PostMapping("/{id}/curriculo")
    public ResponseEntity<Void> atualizarCurriculo(
            @PathVariable Long id,
            @RequestParam MultipartFile curriculo) {

        prestadorService.atualizarCurriculo(id, curriculo);
        return ResponseEntity.noContent().build();
    }

    /**
     * Desativa um prestador
     * PATCH /api/prestadores/{id}/desativar
     */
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativarPrestador(@PathVariable Long id) {
        prestadorService.desativarPrestador(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ativa um prestador previamente desativado
     * PATCH /api/prestadores/{id}/ativar
     */
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativarPrestador(@PathVariable Long id) {
        prestadorService.ativarPrestador(id);
        return ResponseEntity.noContent().build();
    }
}

