package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.RelatorioGanhosDTO;
import ads.uninassau.brjobs.security.ValidateTenant;
import ads.uninassau.brjobs.service.GanhosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ganhos")
@RequiredArgsConstructor
public class GanhosController {

    private final GanhosService ganhosService;

    /**
     * Gera relatório de ganhos para um mês específico
     * GET /api/v1/ganhos?ano=2026&mes=4
     */
    @GetMapping
    @ValidateTenant
    public ResponseEntity<RelatorioGanhosDTO> gerar(
        @RequestAttribute("tenant_id") Long tenantId,
        @RequestParam int ano,
        @RequestParam int mes
    ) {
        RelatorioGanhosDTO relatorio = ganhosService.gerar(tenantId, ano, mes);
        return ResponseEntity.ok(relatorio);
    }

    /**
     * Gera relatório do mês/ano corrente
     * GET /api/v1/ganhos/corrente
     */
    @GetMapping("/corrente")
    @ValidateTenant
    public ResponseEntity<RelatorioGanhosDTO> gerarCorrente(
        @RequestAttribute("tenant_id") Long tenantId
    ) {
        // Gera para o mês/ano atual
        int ano = java.time.LocalDate.now().getYear();
        int mes = java.time.LocalDate.now().getMonthValue();
        
        RelatorioGanhosDTO relatorio = ganhosService.gerar(tenantId, ano, mes);
        return ResponseEntity.ok(relatorio);
    }
}
