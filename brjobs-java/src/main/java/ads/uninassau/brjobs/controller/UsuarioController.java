package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.CadastroContratanteDTO;
import ads.uninassau.brjobs.dto.CadastroPrestadorDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.FileService;
import ads.uninassau.brjobs.service.UsuarioService;
import ads.uninassau.brjobs.service.PrestadorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller responsável pelas operações de usuário.
 * Disponibiliza endpoints para criação, atualização e consulta de usuários.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PrestadorService prestadorService;

    @Autowired
    private FileService fileService;

    /**
     * Registra um novo usuário contratante
     * POST /api/usuarios/contratante
     */
    @PostMapping("/contratante")
    public ResponseEntity<UsuarioDTO> registrarContratante(@Valid @RequestBody CadastroContratanteDTO dto) {
        UsuarioDTO usuarioDTO = usuarioService.criarContratante(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDTO);
    }

    /**
     * Registra um novo usuário prestador
     * POST /api/usuarios/prestador
     *
     * O registro de Prestador (com campos específicos) deve ser feito separadamente
     * através da rota POST /api/prestadores
     */
    @PostMapping("/prestador")
    public ResponseEntity<UsuarioDTO> registrarPrestador(@Valid @RequestBody CadastroPrestadorDTO dto) {
        UsuarioDTO usuarioDTO = usuarioService.criarPrestador(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDTO);
    }

    /**
     * Lista todos os usuários ativos
     * GET /api/usuarios
     */
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Busca um usuário específico por ID
     * GET /api/usuarios/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        UsuarioDTO usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Busca um usuário por email
     * GET /api/usuarios/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioDTO> buscarPorEmail(@PathVariable String email) {
        UsuarioDTO usuario = usuarioService.buscarPorEmail(email);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Atualiza dados básicos do usuário (nome, telefone, endereço, etc)
     * PUT /api/usuarios/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizarDadosBasicos(@PathVariable Long id, @Valid @RequestBody UsuarioDTO dto) {
        UsuarioDTO usuarioAtualizado = usuarioService.atualizarDadosBasicos(id, dto);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    /**
     * Atualiza a senha do usuário
     * PATCH /api/usuarios/{id}/senha
     */
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> atualizarSenha(@PathVariable Long id, @RequestParam String novaSenha) {
        usuarioService.atualizarSenha(id, novaSenha);
        return ResponseEntity.noContent().build();
    }

    /**
     * Atualiza a foto de perfil do usuário
     * POST /api/usuarios/{id}/foto (multipart/form-data)
     */
    @PostMapping("/{id}/foto")
    public ResponseEntity<Void> atualizarFotoPerfil(@PathVariable Long id, @RequestParam MultipartFile foto) {
        byte[] fotoBytes = fileService.processarFotoPerfil(foto);
        usuarioService.atualizarFotoPerfil(id, fotoBytes);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deleta um usuário (soft delete)
     * DELETE /api/usuarios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ativa um usuário previamente desativado
     * PATCH /api/usuarios/{id}/ativar
     */
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativarUsuario(@PathVariable Long id) {
        usuarioService.ativarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
