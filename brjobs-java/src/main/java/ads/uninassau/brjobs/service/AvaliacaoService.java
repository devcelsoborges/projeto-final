package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.AvaliacaoDTO;
import ads.uninassau.brjobs.model.Avaliacao;
import ads.uninassau.brjobs.model.Prestador;
import ads.uninassau.brjobs.model.SolicitacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.AvaliacaoRepository;
import ads.uninassau.brjobs.repository.PrestadorRepository;
import ads.uninassau.brjobs.repository.SolicitacaoServicoRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelas operações de Avaliação.
 * Responsabilidades:
 * - Criar, atualizar e deletar avaliações
 * - Validar dados antes de persistir
 * - Manter a nota média dos prestadores atualizada
 * - Consultar avaliações por diferentes critérios
 */
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrestadorRepository prestadorRepository;
    private final SolicitacaoServicoRepository solicitacaoServicoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                           UsuarioRepository usuarioRepository,
                           PrestadorRepository prestadorRepository,
                           SolicitacaoServicoRepository solicitacaoServicoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.prestadorRepository = prestadorRepository;
        this.solicitacaoServicoRepository = solicitacaoServicoRepository;
    }

    /**
     * Cria uma nova avaliação para um prestador após conclusão de um serviço
     *
     * @param avaliacaoDTO dados da avaliação
     * @return AvaliacaoDTO com os dados da avaliação criada
     * @throws IllegalArgumentException se os dados forem inválidos
     */
    @Transactional
    public AvaliacaoDTO criarAvaliacao(AvaliacaoDTO avaliacaoDTO) {
        // Validar que nota está entre 1 e 5
        if (avaliacaoDTO.getNota() == null || avaliacaoDTO.getNota() < 1 || avaliacaoDTO.getNota() > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        }

        // Buscar relacionamentos necessários
        Usuario usuario = usuarioRepository.findById(avaliacaoDTO.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        Prestador prestador = prestadorRepository.findById(avaliacaoDTO.getPrestadorId())
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        SolicitacaoServico solicitacao = solicitacaoServicoRepository.findById(avaliacaoDTO.getSolicitacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de serviço não encontrada."));

        // Validar que a solicitação pertence ao usuário que está avaliando
        if (!solicitacao.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Apenas o contratante pode avaliar este serviço.");
        }

        // Validar que a avaliação é para o prestador correto
        // O prestador é o usuário que criou o serviço
        Usuario prestadorUsuario = solicitacao.getServico().getUsuario();
        if (!prestadorUsuario.getId().equals(prestador.getUsuario().getId())) {
            throw new IllegalArgumentException("A avaliação não corresponde ao prestador do serviço.");
        }

        // Validar que já não existe avaliação para esta solicitação
        if (avaliacaoRepository.existsBySolicitacao(solicitacao)) {
            throw new IllegalArgumentException("Uma avaliação já existe para esta solicitação.");
        }

        // Criar entidade Avaliacao
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(avaliacaoDTO.getNota());
        avaliacao.setComentario(avaliacaoDTO.getComentario());
        avaliacao.setUsuario(usuario);
        avaliacao.setPrestador(prestador);
        avaliacao.setSolicitacao(solicitacao);

        // Salvar avaliação
        avaliacao = avaliacaoRepository.save(avaliacao);

        // Atualizar nota média do prestador
        prestador.atualizarNotaMedia();
        prestadorRepository.save(prestador);

        return toDTO(avaliacao);
    }

    /**
     * Atualiza uma avaliação existente
     */
    @Transactional
    public AvaliacaoDTO atualizarAvaliacao(Long avaliacaoId, AvaliacaoDTO avaliacaoDTO) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada."));

        // Validar nova nota
        if (avaliacaoDTO.getNota() != null && (avaliacaoDTO.getNota() < 1 || avaliacaoDTO.getNota() > 5)) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        }

        // Atualizar campos
        if (avaliacaoDTO.getNota() != null) {
            avaliacao.setNota(avaliacaoDTO.getNota());
        }
        if (avaliacaoDTO.getComentario() != null) {
            avaliacao.setComentario(avaliacaoDTO.getComentario());
        }

        avaliacao = avaliacaoRepository.save(avaliacao);

        // Atualizar nota média do prestador
        Prestador prestador = avaliacao.getPrestador();
        prestador.atualizarNotaMedia();
        prestadorRepository.save(prestador);

        return toDTO(avaliacao);
    }

    /**
     * Deleta uma avaliação (soft delete ou hard delete)
     */
    @Transactional
    public void deletarAvaliacao(Long avaliacaoId) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada."));

        Prestador prestador = avaliacao.getPrestador();

        // Deletar avaliação
        avaliacaoRepository.deleteById(avaliacaoId);

        // Atualizar nota média do prestador
        prestador.atualizarNotaMedia();
        prestadorRepository.save(prestador);
    }

    /**
     * Lista todas as avaliações
     */
    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarAvaliacoes() {
        return avaliacaoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista todas as avaliações de um prestador específico
     *
     * @param prestadorId ID do prestador
     * @return lista de avaliações do prestador
     */
    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarAvaliacoesPorPrestador(Long prestadorId) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        return avaliacaoRepository.findByPrestador(prestador)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista todas as avaliações feitas por um usuário
     *
     * @param usuarioId ID do usuário
     * @return lista de avaliações do usuário
     */
    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarAvaliacoesPorUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        return avaliacaoRepository.findByUsuario(usuario)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca uma avaliação específica por ID
     */
    @Transactional(readOnly = true)
    public AvaliacaoDTO buscarPorId(Long avaliacaoId) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada."));
        return toDTO(avaliacao);
    }

    /**
     * Converte entidade Avaliacao para DTO
     */
    private AvaliacaoDTO toDTO(Avaliacao entity) {
        AvaliacaoDTO dto = new AvaliacaoDTO();
        dto.setId(entity.getId());
        dto.setNota(entity.getNota());
        dto.setComentario(entity.getComentario());

        if (entity.getSolicitacao() != null) {
            dto.setSolicitacaoId(entity.getSolicitacao().getId());
        }

        if (entity.getUsuario() != null) {
            dto.setUsuarioId(entity.getUsuario().getId());
        }

        if (entity.getPrestador() != null) {
            dto.setPrestadorId(entity.getPrestador().getId());
        }

        if (entity.getDataCriacao() != null) {
            dto.setDataCriacao(entity.getDataCriacao());
        }

        return dto;
    }

    /**
     * Converte DTO para entidade Avaliacao
     * Nota: Este método é apenas auxiliar. O verdadeiro mapeamento ocorre em criarAvaliacao
     * para garantir validações e relacionamentos corretos.
     */
    private Avaliacao toEntity(AvaliacaoDTO dto) {
        Avaliacao entity = new Avaliacao();
        entity.setNota(dto.getNota());
        entity.setComentario(dto.getComentario());
        return entity;
    }
}

