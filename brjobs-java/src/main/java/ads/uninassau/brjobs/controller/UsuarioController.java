package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioDTO> criarUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO novoUsuario = usuarioService.criarUsuario(usuarioDTO);
        return new ResponseEntity<>(novoUsuario, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarUsuarioPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO usuarioDTO) {
        return usuarioService.atualizarUsuario(id, usuarioDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        if (usuarioService.deletarUsuario(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}