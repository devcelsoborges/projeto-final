package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.AvaliacaoDTO;
import ads.uninassau.brjobs.service.AvaliacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    @Autowired
    private AvaliacaoService avaliacaoService;

    @PostMapping
    public ResponseEntity<AvaliacaoDTO> criarAvaliacao(@RequestBody AvaliacaoDTO avaliacaoDTO) {
        AvaliacaoDTO novaAvaliacao = avaliacaoService.criarAvaliacao(avaliacaoDTO);
        return new ResponseEntity<>(novaAvaliacao, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AvaliacaoDTO>> listarAvaliacoes() {
        List<AvaliacaoDTO> avaliacoes = avaliacaoService.listarAvaliacoes();
        return ResponseEntity.ok(avaliacoes);
    }
}