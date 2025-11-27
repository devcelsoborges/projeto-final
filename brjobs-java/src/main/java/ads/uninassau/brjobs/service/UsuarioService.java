package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.CadastroContratanteDTO;
import ads.uninassau.brjobs.dto.CadastroPrestadorDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.CPFAlreadyInUseException;
import ads.uninassau.brjobs.exception.EmailAlreadyInUseException;
import ads.uninassau.brjobs.exception.UserNotFoundException;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.validator.UsuarioValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelas operações CRUD de usuários e validações de negócio.
 * Não trata diretamente de uploads de arquivo (delegado para FileService).
 * Não trata de operações específicas de Prestador (delegado para PrestadorService).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria um novo usuário contratante.
     * Valida email, CPF, telefone, data de nascimento e senha.
     *
     * @param dto dados do contratante
     * @return UsuarioDTO com os dados do usuário criado
     * @throws EmailAlreadyInUseException se o email já estiver em uso
     * @throws CPFAlreadyInUseException se o CPF já estiver em uso
     */
    @Transactional
    public UsuarioDTO criarContratante(CadastroContratanteDTO dto) {
        validarDadosParaCadastro(dto.getEmail(), dto.getCpf(), dto.getTelefone(), dto.getDataNascimento(), dto.getSenha());

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.CONTRATANTE);
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setTelefone(dto.getTelefone());
        usuario.setCpf(dto.getCpf());
        usuario.setGenero(dto.getGenero());
        usuario.setDataNascimento(dto.getDataNascimento());
        usuario.setEndereco(dto.getEndereco());
        usuario.setAtivo(true);

        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    /**
     * Cria um novo usuário prestador.
     * Valida email, CPF, telefone, data de nascimento e senha.
     * O prestador será criado posteriormente via PrestadorService.
     *
     * @param dto dados do prestador
     * @return UsuarioDTO com os dados do usuário criado
     * @throws EmailAlreadyInUseException se o email já estiver em uso
     * @throws CPFAlreadyInUseException se o CPF já estiver em uso
     */
    @Transactional
    public UsuarioDTO criarPrestador(CadastroPrestadorDTO dto) {
        validarDadosParaCadastro(dto.getEmail(), dto.getCpf(), dto.getTelefone(), dto.getDataNascimento(), dto.getSenha());

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PRESTADOR);
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setTelefone(dto.getTelefone());
        usuario.setCpf(dto.getCpf());
        usuario.setGenero(dto.getGenero());
        usuario.setDataNascimento(dto.getDataNascimento());
        usuario.setEndereco(dto.getEndereco());
        usuario.setAtivo(true);

        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    /**
     * Lista todos os usuários ativos
     */
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .filter(Usuario::isAtivo)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca um usuário por ID
     *
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    public UsuarioDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toDTO(usuario);
    }

    /**
     * Busca um usuário por email
     */
    public UsuarioDTO buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + email));
        return toDTO(usuario);
    }

    /**
     * Atualiza dados básicos do usuário (não atualiza senha, foto ou currículo)
     *
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public UsuarioDTO atualizarDadosBasicos(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        usuario.setNome(dto.getNome());
        usuario.setTelefone(dto.getTelefone());
        usuario.setEndereco(dto.getEndereco());
        usuario.setGenero(dto.getGenero());
        usuario.setDataNascimento(dto.getDataNascimento());

        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    /**
     * Atualiza a senha do usuário
     * Valida a força da nova senha
     *
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public void atualizarSenha(Long id, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        UsuarioValidator.validarSenha(novaSenha);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    /**
     * Atualiza a foto de perfil do usuário
     *
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public void atualizarFotoPerfil(Long id, byte[] fotoPerfil) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        usuario.setFotoPerfil(fotoPerfil);
        usuarioRepository.save(usuario);
    }

    /**
     * Deleta (soft delete) um usuário
     */
    @Transactional
    public void deletarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        usuario.desativar();
        usuarioRepository.save(usuario);
    }

    /**
     * Ativa um usuário previamente desativado
     */
    @Transactional
    public void ativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        usuario.ativar();
        usuarioRepository.save(usuario);
    }

    /**
     * Valida dados comuns para cadastro
     */
    private void validarDadosParaCadastro(String email, String cpf, String telefone, LocalDate dataNascimento, String senha) {
        // Validar email
        if (!UsuarioValidator.validarEmail(email)) {
            throw new IllegalArgumentException("O formato do email é inválido.");
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyInUseException(email);
        }

        // Validar CPF
        if (!UsuarioValidator.validarCPF(cpf)) {
            throw new IllegalArgumentException("O formato do CPF é inválido. Utilize o formato XXX.XXX.XXX-XX");
        }
        if (usuarioRepository.findByCpf(cpf).isPresent()) {
            throw new CPFAlreadyInUseException(cpf);
        }

        // Validar telefone
        if (!UsuarioValidator.validarTelefone(telefone)) {
            throw new IllegalArgumentException("O formato do telefone é inválido. Utilize o formato (XX) XXXXX-XXXX");
        }

        // Validar data de nascimento
        UsuarioValidator.validarDataNascimento(dataNascimento);

        // Validar senha
        UsuarioValidator.validarSenha(senha);
    }

    /**
     * Converte entidade Usuario para DTO
     */
    private UsuarioDTO toDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setTipoUsuario(usuario.getTipoUsuario());
        dto.setNome(usuario.getNome());
        dto.setEmail(usuario.getEmail());
        dto.setTelefone(usuario.getTelefone());
        dto.setEndereco(usuario.getEndereco());
        dto.setCpf(usuario.getCpf());
        dto.setGenero(usuario.getGenero());
        dto.setDataNascimento(usuario.getDataNascimento());
        dto.setAtivo(usuario.isAtivo());
        if (usuario.getDataCadastro() != null) {
            dto.setDataCadastro(usuario.getDataCadastro().toLocalDate());
        }
        return dto;
    }
}
