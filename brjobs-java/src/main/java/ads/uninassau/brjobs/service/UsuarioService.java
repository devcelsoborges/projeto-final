package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.entity.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public UsuarioDTO criarUsuario(UsuarioDTO usuarioDTO) {
        Usuario usuario = toEntity(usuarioDTO);
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Usuario toEntity(UsuarioDTO dto) {
        Usuario entity = new Usuario();
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        entity.setSenha(dto.getSenha());
        return entity;
    }

    private UsuarioDTO toDTO(Usuario entity) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        return dto;
    }
}