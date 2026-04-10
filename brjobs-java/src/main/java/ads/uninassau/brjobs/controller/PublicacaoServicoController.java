package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.PublicacaoServicoDTO;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.PublicacaoServicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/publicacoes")
@RequiredArgsConstructor
@Slf4j
public class PublicacaoServicoController {

    private final PublicacaoServicoService publicacaoServicoService;

    @GetMapping
    public ResponseEntity<List<PublicacaoServicoDTO>> listar(@RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(publicacaoServicoService.listar(tipo));
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<PublicacaoServicoDTO>> listarPaginado(
        @RequestParam(required = false) String tipo,
        @RequestParam(required = false) String termo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(publicacaoServicoService.buscarPaginado(tipo, termo, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacaoServicoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(publicacaoServicoService.buscarPorId(id));
    }

    @GetMapping("/minhas")
    @ValidateTenant
    public ResponseEntity<List<PublicacaoServicoDTO>> listarMinhas(@RequestAttribute("tenant_id") Long tenantId) {
        return ResponseEntity.ok(publicacaoServicoService.listarMinhas(tenantId));
    }

    @PostMapping
    @ValidateTenant
    public ResponseEntity<PublicacaoServicoDTO> criar(
        @RequestAttribute("tenant_id") Long tenantId,
        @RequestBody PublicacaoServicoDTO dto
    ) {
        log.info("Publicacao POST: tenantId={} tipo={} titulo={}", tenantId, dto.getTipoPublicacao(), dto.getTitulo());
        PublicacaoServicoDTO criado = publicacaoServicoService.criarComTenant(tenantId, dto);
        return new ResponseEntity<>(criado, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @ValidateTenant
    public ResponseEntity<Void> encerrar(
        @RequestAttribute("tenant_id") Long tenantId,
        @PathVariable Long id
    ) {
        publicacaoServicoService.encerrarComTenant(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
