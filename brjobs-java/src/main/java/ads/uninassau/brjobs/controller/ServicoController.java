package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.ServicoDTO;
import ads.uninassau.brjobs.service.ServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    @Autowired
    private ServicoService servicoService;

    @PostMapping
    public ResponseEntity<ServicoDTO> criarServico(@RequestBody ServicoDTO servicoDTO) {
        ServicoDTO novoServico = servicoService.criarServico(servicoDTO);
        return new ResponseEntity<>(novoServico, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ServicoDTO>> listarServicos() {
        List<ServicoDTO> servicos = servicoService.listarServicos();
        return ResponseEntity.ok(servicos);
    }
}