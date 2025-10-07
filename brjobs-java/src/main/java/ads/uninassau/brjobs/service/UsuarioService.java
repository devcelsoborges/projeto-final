package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioDTO criarUsuario(UsuarioDTO usuarioDTO) {
        if (usuarioRepository.findByEmail(usuarioDTO.getEmail()).isPresent()) {
            throw new IllegalStateException("E-mail já está em uso.");
        }

        Usuario usuario = toEntity(usuarioDTO);

        if (usuarioDTO.getSenha() != null && !usuarioDTO.getSenha().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        }

        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o ID: " + id));
        return toDTO(usuario);
    }

    @Transactional
    public UsuarioDTO atualizarUsuario(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o ID: " + id));

        usuarioExistente.setTipoUsuario(usuarioDTO.getTipoUsuario());
        usuarioExistente.setNome(usuarioDTO.getNome());
        usuarioExistente.setEmail(usuarioDTO.getEmail());
        usuarioExistente.setTelefone(usuarioDTO.getTelefone());
        usuarioExistente.setEndereco(usuarioDTO.getEndereco());
        usuarioExistente.setCpf(usuarioDTO.getCpf());
        usuarioExistente.setFuncao(usuarioDTO.getFuncao());
        usuarioExistente.setDataNascimento(usuarioDTO.getDataNascimento());
        usuarioExistente.setGenero(usuarioDTO.getGenero());
        usuarioExistente.setExperienciaProfissional(usuarioDTO.getExperienciaProfissional());
        usuarioExistente.setEspecialidades(usuarioDTO.getEspecialidades());

        if (usuarioDTO.getSenha() != null && !usuarioDTO.getSenha().isEmpty()) {
            usuarioExistente.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
        }

        try {
            if (usuarioDTO.getFotoPerfil() != null) {
                usuarioExistente.setFotoPerfil(usuarioDTO.getFotoPerfil().getBytes());
            }
            if (usuarioDTO.getCurriculo() != null) {
                usuarioExistente.setCurriculo(usuarioDTO.getCurriculo().getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar arquivos", e);
        }

        usuarioExistente = usuarioRepository.save(usuarioExistente);
        return toDTO(usuarioExistente);
    }

    @Transactional
    public boolean deletarUsuario(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private Usuario toEntity(UsuarioDTO dto) {
        Usuario entity = new Usuario();
        entity.setTipoUsuario(dto.getTipoUsuario());
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        entity.setSenha(dto.getSenha());
        entity.setTelefone(dto.getTelefone());
        entity.setEndereco(dto.getEndereco());
        entity.setCpf(dto.getCpf());
        entity.setFuncao(dto.getFuncao());
        entity.setDataNascimento(dto.getDataNascimento());
        entity.setGenero(dto.getGenero());
        entity.setExperienciaProfissional(dto.getExperienciaProfissional());
        entity.setEspecialidades(dto.getEspecialidades());

        try {
            if (dto.getFotoPerfil() != null) {
                entity.setFotoPerfil(dto.getFotoPerfil().getBytes());
            }
            if (dto.getCurriculo() != null) {
                entity.setCurriculo(dto.getCurriculo().getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar arquivos", e);
        }

        return entity;
    }

    private UsuarioDTO toDTO(Usuario entity) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(entity.getId());
        dto.setTipoUsuario(entity.getTipoUsuario());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setTelefone(entity.getTelefone());
        dto.setEndereco(entity.getEndereco());
        dto.setCpf(entity.getCpf());
        dto.setFuncao(entity.getFuncao());
        dto.setDataNascimento(entity.getDataNascimento());
        dto.setGenero(entity.getGenero());
        dto.setExperienciaProfissional(entity.getExperienciaProfissional());
        dto.setEspecialidades(entity.getEspecialidades());
        // NÃO inclui arquivos no DTO de retorno (opcional)
        return dto;
    }
}
