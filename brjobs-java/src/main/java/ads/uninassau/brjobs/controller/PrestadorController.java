package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.PrestadorDTO;
import ads.uninassau.brjobs.service.PrestadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestadores")
public class PrestadorController {

    @Autowired
    private PrestadorService prestadorService;

    @PostMapping
    public ResponseEntity<PrestadorDTO> criarPrestador(@RequestBody PrestadorDTO prestadorDTO) {
        PrestadorDTO novoPrestador = prestadorService.criarPrestador(prestadorDTO);
        return new ResponseEntity<>(novoPrestador, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PrestadorDTO>> listarPrestadores() {
        List<PrestadorDTO> prestadores = prestadorService.listarTodos();
        return ResponseEntity.ok(prestadores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestadorDTO> buscarPrestadorPorId(@PathVariable Long id) {
        return prestadorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}