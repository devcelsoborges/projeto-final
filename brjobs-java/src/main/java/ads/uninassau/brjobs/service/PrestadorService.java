package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.CadastroPrestadorDTO;
import ads.uninassau.brjobs.dto.PrestadorResponseDTO;
import ads.uninassau.brjobs.exception.InvalidUserTypeException;
import ads.uninassau.brjobs.exception.UserNotFoundException;
import ads.uninassau.brjobs.model.Prestador;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.PrestadorRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelas operações de Prestador.
 * Requer que o usuário já esteja criado com tipoUsuario == PRESTADOR.
 */
@Service
public class PrestadorService {

    private final PrestadorRepository prestadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final FileService fileService;

    public PrestadorService(PrestadorRepository prestadorRepository,
                           UsuarioRepository usuarioRepository,
                           FileService fileService) {
        this.prestadorRepository = prestadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.fileService = fileService;
    }

    /**
     * Cria um registro de prestador para um usuário existente.
     * O usuário deve ter tipoUsuario == PRESTADOR.
     *
     * @param usuarioId ID do usuário
     * @param dto dados específicos do prestador
     * @param curriculo arquivo de currículo (opcional)
     * @return dados do prestador criado
     * @throws UserNotFoundException se o usuário não existir
     * @throws InvalidUserTypeException se o usuário não for PRESTADOR
     */
    @Transactional
    public PrestadorResponseDTO criarPrestador(Long usuarioId, CadastroPrestadorDTO dto, MultipartFile curriculo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UserNotFoundException(usuarioId));

        // Validar que o usuário é um prestador
        if (!usuario.isPrestador()) {
            throw new InvalidUserTypeException(
                "Apenas usuários do tipo PRESTADOR podem ter registro de prestador. Tipo atual: " + usuario.getTipoUsuario().getDescricao()
            );
        }

        // Verificar se já existe prestador para este usuário
        if (usuario.getPrestador() != null) {
            throw new IllegalStateException("Este usuário já possui um registro de prestador ativo.");
        }

        // Criar novo prestador
        Prestador prestador = new Prestador();
        prestador.setUsuario(usuario);
        prestador.setFuncao(dto.getFuncao());
        prestador.setExperienciaProfissional(dto.getExperienciaProfissional());
        prestador.setEspecialidades(dto.getEspecialidades());
        prestador.setAtivo(true);

        // Processar currículo se fornecido
        if (curriculo != null && !curriculo.isEmpty()) {
            byte[] curriculoBytes = fileService.processarCurriculo(curriculo);
            prestador.setCurriculo(curriculoBytes);
        }

        prestador = prestadorRepository.save(prestador);
        usuario.setPrestador(prestador);
        usuarioRepository.save(usuario);

        return toDTO(prestador);
    }

    /**
     * Atualiza dados do prestador.
     * NÃO atualiza currículo (usar método específico para isso).
     */
    @Transactional
    public PrestadorResponseDTO atualizarPrestador(Long prestadorId, CadastroPrestadorDTO dto) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado com ID: " + prestadorId));

        prestador.setFuncao(dto.getFuncao());
        prestador.setExperienciaProfissional(dto.getExperienciaProfissional());
        prestador.setEspecialidades(dto.getEspecialidades());

        prestador = prestadorRepository.save(prestador);
        return toDTO(prestador);
    }

    /**
     * Atualiza o currículo do prestador
     */
    @Transactional
    public void atualizarCurriculo(Long prestadorId, MultipartFile curriculo) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado com ID: " + prestadorId));

        byte[] curriculoBytes = fileService.processarCurriculo(curriculo);
        prestador.setCurriculo(curriculoBytes);
        prestadorRepository.save(prestador);
    }

    /**
     * Busca prestador por ID
     */
    @Transactional(readOnly = true)
    public PrestadorResponseDTO buscarPorId(Long id) {
        Prestador prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado com ID: " + id));
        return toDTO(prestador);
    }

    /**
     * Busca prestador por usuário ID
     */
    @Transactional(readOnly = true)
    public PrestadorResponseDTO buscarPorUsuarioId(Long usuarioId) {
        Prestador prestador = prestadorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado para o usuário ID: " + usuarioId));
        return toDTO(prestador);
    }

    /**
     * Lista todos os prestadores ativos
     */
    @Transactional(readOnly = true)
    public List<PrestadorResponseDTO> listarPrestadoresAtivos() {
        return prestadorRepository.findAll()
                .stream()
                .filter(Prestador::isAtivo)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista todos os prestadores (ativos e inativos)
     */
    @Transactional(readOnly = true)
    public List<PrestadorResponseDTO> listarTodosPrestadores() {
        return prestadorRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Desativa um prestador (soft delete)
     */
    @Transactional
    public void desativarPrestador(Long prestadorId) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado com ID: " + prestadorId));
        prestador.desativar();
        prestadorRepository.save(prestador);
    }

    /**
     * Ativa um prestador previamente desativado
     */
    @Transactional
    public void ativarPrestador(Long prestadorId) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado com ID: " + prestadorId));
        prestador.ativar();
        prestadorRepository.save(prestador);
    }

    /**
     * Converte entidade Prestador para DTO
     */
    private PrestadorResponseDTO toDTO(Prestador prestador) {
        PrestadorResponseDTO dto = new PrestadorResponseDTO();
        dto.setId(prestador.getId());
        dto.setUsuarioId(prestador.getUsuario().getId());
        dto.setFuncao(prestador.getFuncao());
        dto.setExperienciaProfissional(prestador.getExperienciaProfissional());
        dto.setEspecialidades(prestador.getEspecialidades());
        dto.setDescricao(prestador.getDescricao());
        dto.setAtivo(prestador.isAtivo());
        if (prestador.getDataCadastro() != null) {
            dto.setDataCadastro(prestador.getDataCadastro());
        }

        return dto;
    }
}
