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
    private final AccountActivationService accountActivationService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          AccountActivationService accountActivationService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountActivationService = accountActivationService;
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
        String email = UsuarioValidator.normalizarEmail(dto.getEmail());
        validarDadosParaCadastro(email, dto.getCpf(), dto.getTelefone(), dto.getDataNascimento(), dto.getSenha(), dto.getConfirmacaoSenha());

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.CONTRATANTE);
        usuario.setNome(dto.getNome());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setTelefone(dto.getTelefone());
        usuario.setCpf(dto.getCpf());
        usuario.setGenero(dto.getGenero());
        usuario.setDataNascimento(dto.getDataNascimento());
        usuario.setEndereco(dto.getEndereco());
        usuario.setCep(dto.getCep());
        usuario.setRua(dto.getRua());
        usuario.setBairro(dto.getBairro());
        usuario.setCidade(dto.getCidade());
        usuario.setEstado(dto.getEstado());
        usuario.setNumero(dto.getNumero());
        usuario.setComplemento(dto.getComplemento());
        usuario.setBio(dto.getBio());
        usuario.setAtivo(true);

        usuario = usuarioRepository.save(usuario);
        accountActivationService.enviarConfirmacao(usuario);
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
        String email = UsuarioValidator.normalizarEmail(dto.getEmail());
        validarDadosParaCadastro(email, dto.getCpf(), dto.getTelefone(), dto.getDataNascimento(), dto.getSenha(), dto.getConfirmacaoSenha());

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PRESTADOR);
        usuario.setNome(dto.getNome());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setTelefone(dto.getTelefone());
        usuario.setCpf(dto.getCpf());
        usuario.setGenero(dto.getGenero());
        usuario.setDataNascimento(dto.getDataNascimento());
        usuario.setEndereco(dto.getEndereco());
        usuario.setCep(dto.getCep());
        usuario.setRua(dto.getRua());
        usuario.setBairro(dto.getBairro());
        usuario.setCidade(dto.getCidade());
        usuario.setEstado(dto.getEstado());
        usuario.setNumero(dto.getNumero());
        usuario.setComplemento(dto.getComplemento());
        usuario.setBio(dto.getBio());
        usuario.setAtivo(true);

        usuario = usuarioRepository.save(usuario);
        accountActivationService.enviarConfirmacao(usuario);
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
     * Atualiza dados básicos do usuário (não atualiza senha, foto ou currículo).
     * Merge parcial: campos ausentes (null) no DTO preservam o valor atual;
     * string em branco limpa explicitamente o campo (exceto nome, que é obrigatório).
     *
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public UsuarioDTO atualizarDadosBasicos(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome());
        }
        usuario.setTelefone(mesclarCampo(dto.getTelefone(), usuario.getTelefone()));
        usuario.setEndereco(mesclarCampo(dto.getEndereco(), usuario.getEndereco()));
        usuario.setCep(mesclarCampo(dto.getCep(), usuario.getCep()));
        usuario.setRua(mesclarCampo(dto.getRua(), usuario.getRua()));
        usuario.setBairro(mesclarCampo(dto.getBairro(), usuario.getBairro()));
        usuario.setCidade(mesclarCampo(dto.getCidade(), usuario.getCidade()));
        usuario.setEstado(mesclarCampo(dto.getEstado(), usuario.getEstado()));
        usuario.setNumero(mesclarCampo(dto.getNumero(), usuario.getNumero()));
        usuario.setComplemento(mesclarCampo(dto.getComplemento(), usuario.getComplemento()));
        usuario.setBio(mesclarCampo(dto.getBio(), usuario.getBio()));
        usuario.setGenero(mesclarCampo(dto.getGenero(), usuario.getGenero()));
        if (dto.getDataNascimento() != null) {
            usuario.setDataNascimento(dto.getDataNascimento());
        }

        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    /**
     * Regra de merge parcial para campos opcionais de texto:
     * null preserva o valor atual; string em branco limpa o campo.
     */
    private static String mesclarCampo(String novoValor, String valorAtual) {
        if (novoValor == null) {
            return valorAtual;
        }
        return novoValor.isBlank() ? null : novoValor;
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
     * Busca os bytes da foto de perfil do usuário
     *
     * @return bytes da foto, ou null se o usuário não possuir foto
     * @throws UserNotFoundException se o usuário não for encontrado
     */
    public byte[] buscarFotoPerfil(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return usuario.getFotoPerfil();
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
     * Valida dados comuns para cadastro.
     * A unicidade do e-mail é checada de forma case-insensitive: o mesmo e-mail (ainda
     * que com caixa diferente) nunca gera uma segunda conta. Se já existir uma conta
     * com este e-mail — inclusive criada via login social — o cadastro é rejeitado com
     * 409; o caminho seguro para uma conta social ganhar senha local é "Esqueci minha
     * senha" (que verifica a posse do e-mail). Assim evitamos account takeover por um
     * cadastro anônimo definir senha numa conta que não é sua.
     */
    private void validarDadosParaCadastro(String email, String cpf, String telefone, LocalDate dataNascimento, String senha, String confirmacaoSenha) {
        // Validar email
        if (!UsuarioValidator.validarEmail(email)) {
            throw new IllegalArgumentException("O formato do email é inválido.");
        }
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            // Mantemos o 409 (nunca deixar um cadastro anônimo definir senha numa conta
            // existente = account takeover) e orientamos o caminho seguro. Mensagem NEUTRA:
            // não revela se a conta é social ou local (evita enumeração de tipo de conta).
            throw new EmailAlreadyInUseException(
                    "Este e-mail já está em uso. Se a conta é sua, faça login (inclusive com Google/Facebook) "
                            + "ou use \"Esqueci minha senha\" para definir/recuperar o acesso.");
        }

        // Validar CPF
        if (cpf != null && !cpf.isBlank() && !UsuarioValidator.validarCPF(cpf)) {
            throw new IllegalArgumentException("O formato do CPF é inválido. Utilize o formato XXX.XXX.XXX-XX");
        }
        if (cpf != null && !cpf.isBlank() && usuarioRepository.findByCpf(cpf).isPresent()) {
            throw new CPFAlreadyInUseException(cpf);
        }

        // Validar telefone
        if (telefone != null && !telefone.isBlank() && !UsuarioValidator.validarTelefone(telefone)) {
            throw new IllegalArgumentException("O formato do telefone é inválido. Utilize o formato (XX) XXXXX-XXXX");
        }

        // Validar data de nascimento
        if (dataNascimento != null) {
            UsuarioValidator.validarDataNascimento(dataNascimento);
        }

        if (confirmacaoSenha != null && !confirmacaoSenha.isBlank() && !senha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

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
        dto.setCep(usuario.getCep());
        dto.setRua(usuario.getRua());
        dto.setBairro(usuario.getBairro());
        dto.setCidade(usuario.getCidade());
        dto.setEstado(usuario.getEstado());
        dto.setNumero(usuario.getNumero());
        dto.setComplemento(usuario.getComplemento());
        dto.setBio(usuario.getBio());
        dto.setCpf(usuario.getCpf());
        dto.setGenero(usuario.getGenero());
        dto.setDataNascimento(usuario.getDataNascimento());
        dto.setAtivo(usuario.isAtivo());
        dto.setEmailConfirmado(usuario.getEmailConfirmado());
        if (usuario.getDataCadastro() != null) {
            dto.setDataCadastro(usuario.getDataCadastro().toLocalDate());
        }
        if (usuario.getFotoPerfil() != null && usuario.getFotoPerfil().length > 0) {
            dto.setFotoPerfilUrl("/api/usuarios/" + usuario.getId() + "/foto");
        }
        return dto;
    }
}
