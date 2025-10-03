package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.SolicitacaoServicoDTO;
import ads.uninassau.brjobs.service.SolicitacaoServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
public class SolicitacaoServicoController {

    @Autowired
    private SolicitacaoServicoService solicitacaoServicoService;

    @PostMapping
    public ResponseEntity<SolicitacaoServicoDTO> criarSolicitacao(@RequestBody SolicitacaoServicoDTO solicitacaoDTO) {
        SolicitacaoServicoDTO novaSolicitacao = solicitacaoServicoService.criarSolicitacao(solicitacaoDTO);
        return new ResponseEntity<>(novaSolicitacao, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoServicoDTO>> listarSolicitacoes() {
        List<SolicitacaoServicoDTO> solicitacoes = solicitacaoServicoService.listarSolicitacoes();
        return ResponseEntity.ok(solicitacoes);
    }
}