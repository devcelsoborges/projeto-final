package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200") // Angular rodando na porta 4200
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Cadastro de usuário com todos os campos e arquivos
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UsuarioDTO> criarUsuario(
            @RequestParam String tipoUsuario,
            @RequestParam String nome,
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam String telefone,
            @RequestParam String endereco,
            @RequestParam String cpf,
            @RequestParam(required = false) String funcao,
            @RequestParam String dataNascimento,
            @RequestParam String genero,
            @RequestParam(required = false) String experienciaProfissional,
            @RequestParam(required = false) String especialidades,
            @RequestParam(required = false) MultipartFile fotoPerfil,
            @RequestParam(required = false) MultipartFile curriculo
    ) {
        UsuarioDTO usuarioDTO = new UsuarioDTO();
        usuarioDTO.setTipoUsuario(tipoUsuario);
        usuarioDTO.setNome(nome);
        usuarioDTO.setEmail(email);
        usuarioDTO.setSenha(senha);
        usuarioDTO.setTelefone(telefone);
        usuarioDTO.setEndereco(endereco);
        usuarioDTO.setCpf(cpf);
        usuarioDTO.setFuncao(funcao);
        usuarioDTO.setDataNascimento(dataNascimento);
        usuarioDTO.setGenero(genero);
        usuarioDTO.setExperienciaProfissional(experienciaProfissional);
        usuarioDTO.setEspecialidades(especialidades);
        usuarioDTO.setFotoPerfil(fotoPerfil);
        usuarioDTO.setCurriculo(curriculo);

        UsuarioDTO novoUsuario = usuarioService.criarUsuario(usuarioDTO);
        return ResponseEntity.status(201).body(novoUsuario);
    }

    // Listar todos os usuários
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    // Buscar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarUsuarioPorId(@PathVariable Long id) {
        UsuarioDTO usuarioDTO = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuarioDTO);
    }

    // Atualizar usuário (todos os campos + arquivos)
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(
            @PathVariable Long id,
            @RequestParam String tipoUsuario,
            @RequestParam String nome,
            @RequestParam String email,
            @RequestParam(required = false) String senha,
            @RequestParam String telefone,
            @RequestParam String endereco,
            @RequestParam String cpf,
            @RequestParam(required = false) String funcao,
            @RequestParam String dataNascimento,
            @RequestParam String genero,
            @RequestParam(required = false) String experienciaProfissional,
            @RequestParam(required = false) String especialidades,
            @RequestParam(required = false) MultipartFile fotoPerfil,
            @RequestParam(required = false) MultipartFile curriculo
    ) {
        UsuarioDTO usuarioDTO = new UsuarioDTO();
        usuarioDTO.setTipoUsuario(tipoUsuario);
        usuarioDTO.setNome(nome);
        usuarioDTO.setEmail(email);
        usuarioDTO.setSenha(senha);
        usuarioDTO.setTelefone(telefone);
        usuarioDTO.setEndereco(endereco);
        usuarioDTO.setCpf(cpf);
        usuarioDTO.setFuncao(funcao);
        usuarioDTO.setDataNascimento(dataNascimento);
        usuarioDTO.setGenero(genero);
        usuarioDTO.setExperienciaProfissional(experienciaProfissional);
        usuarioDTO.setEspecialidades(especialidades);
        usuarioDTO.setFotoPerfil(fotoPerfil);
        usuarioDTO.setCurriculo(curriculo);

        UsuarioDTO usuarioAtualizado = usuarioService.atualizarUsuario(id, usuarioDTO);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    // Deletar usuário por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        if (usuarioService.deletarUsuario(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
