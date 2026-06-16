package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.ServicoDTO;
import ads.uninassau.brjobs.dto.ServicoListaDTO;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.ServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servicos")
@RequiredArgsConstructor
public class ServicoController {

    @Autowired
    private ServicoService servicoService;

    /**
     * Busca serviços com filtros e paginação
     * GET /api/v1/servicos?categoria=pintura&search=sp&page=1&size=10&sort=recente
     */
    @GetMapping
    public ResponseEntity<PageImpl<ServicoListaDTO>> buscar(
        @RequestParam(required = false) String categoria,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "recente") String sort
    ) {
        PageImpl<ServicoListaDTO> result = servicoService.buscar(categoria, search, page, size, sort);
        return ResponseEntity.ok(result);
    }

    /**
     * Cria novo serviço (requer autenticação e tenant = criador)
     * POST /api/v1/servicos
     */
    @PostMapping
    @ValidateTenant
    public ResponseEntity<ServicoDTO> criarServico(
        @RequestAttribute("tenant_id") Long tenantId,
        @RequestBody ServicoDTO servicoDTO
    ) {
        ServicoDTO novoServico = servicoService.criarComTenant(
            tenantId,
            servicoDTO.getTitulo(),
            servicoDTO.getDescricao(),
            servicoDTO.getCategoria(),
            servicoDTO.getPreco()
        );
        return new ResponseEntity<>(novoServico, HttpStatus.CREATED);
    }

    /**
     * Atualiza serviço existente (apenas dono)
     * PUT /api/v1/servicos/:id
     */
    @PutMapping("/{id}")
    @ValidateTenant
    public ResponseEntity<ServicoDTO> atualizarServico(
        @RequestAttribute("tenant_id") Long tenantId,
        @PathVariable Long id,
        @RequestBody ServicoDTO servicoDTO
    ) {
        ServicoDTO atualizado = servicoService.atualizarComTenant(
            tenantId,
            id,
            servicoDTO.getTitulo(),
            servicoDTO.getDescricao(),
            servicoDTO.getCategoria(),
            servicoDTO.getPreco()
        );
        return ResponseEntity.ok(atualizado);
    }

    /**
     * Deleta serviço (apenas dono)
     * DELETE /api/v1/servicos/:id
     */
    @DeleteMapping("/{id}")
    @ValidateTenant
    public ResponseEntity<Void> deletarServico(
        @RequestAttribute("tenant_id") Long tenantId,
        @PathVariable Long id
    ) {
        servicoService.deletarComTenant(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/todos")
    public ResponseEntity<List<ServicoDTO>> listarServicos() {
        List<ServicoDTO> servicos = servicoService.listarServicos();
        return ResponseEntity.ok(servicos);
    }
}