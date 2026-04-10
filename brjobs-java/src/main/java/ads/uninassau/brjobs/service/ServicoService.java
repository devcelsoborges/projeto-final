package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.ServicoDTO;
import ads.uninassau.brjobs.dto.ServicoListaDTO;
import ads.uninassau.brjobs.model.Servico;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.AvaliacaoRepository;
import ads.uninassau.brjobs.repository.ServicoRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicoService {

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final AvaliacaoRepository avaliacaoRepository;

    public ServicoDTO criarServico(ServicoDTO servicoDTO) {
        Servico servico = toEntity(servicoDTO);
        servico = servicoRepository.save(servico);
        return toDTO(servico);
    }

    public List<ServicoDTO> listarServicos() {
        return servicoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca serviços com filtros, paginação e ordenação
     *
     * @param categoria Filtro por categoria (opcional)
     * @param search Busca por titulo ou descricao (opcional)
     * @param page Página (1-indexed)
     * @param size Itens por página (max 100)
     * @param sort Ordenação: recente, avaliacoes, preco
     * @return PageImpl com lista de ServicoListaDTO
     */
    public PageImpl<ServicoListaDTO> buscar(String categoria, String search, int page, int size, String sort) {
        // Validar inputs
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        if (sort == null || sort.isEmpty()) sort = "recente";

        // Buscar todos os serviços ativos
        List<Servico> servicos = servicoRepository.findAll();

        // Filtrar por categoria
        if (categoria != null && !categoria.isEmpty()) {
            servicos = servicos.stream()
                .filter(s -> categoria.equalsIgnoreCase(s.getCategoria()))
                .collect(Collectors.toList());
        }

        // Filtrar por busca de texto
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            servicos = servicos.stream()
                .filter(s -> (s.getTitulo() != null && s.getTitulo().toLowerCase().contains(searchLower)) ||
                            (s.getDescricao() != null && s.getDescricao().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());
        }

        // Aplicar ordenação
        switch (sort.toLowerCase()) {
            case "avaliacoes":
                servicos.sort((s1, s2) -> {
                    Double media1 = avaliacaoRepository.getAvaliacaoMedia(s1.getUsuario().getId());
                    Double media2 = avaliacaoRepository.getAvaliacaoMedia(s2.getUsuario().getId());
                    media1 = media1 != null ? media1 : 0;
                    media2 = media2 != null ? media2 : 0;
                    return media2.compareTo(media1); // DESC
                });
                break;
            case "preco":
                servicos.sort(Comparator.comparingDouble(Servico::getPreco)); // ASC
                break;
            default: // recente
                servicos.sort((s1, s2) -> s2.getDataCriacao().compareTo(s1.getDataCriacao())); // DESC
        }

        // Paginação manual
        int totalElements = servicos.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<Servico> pageContent = servicos.subList(fromIndex, toIndex);

        // Mapear para DTO
        List<ServicoListaDTO> dtos = pageContent.stream()
            .map(this::mapToListaDTO)
            .collect(Collectors.toList());

        // Retornar PageImpl
        Pageable pageReq = PageRequest.of(page - 1, size);
        return new PageImpl<>(dtos, pageReq, totalElements);
    }

    /**
     * Mapeia entidade para DTO simples
     */
    private Servico toEntity(ServicoDTO dto) {
        Servico entity = new Servico();
        entity.setDescricao(dto.getDescricao());
        entity.setPreco(dto.getPreco());
        return entity;
    }

    /**
     * Mapeia entidade para DTO simples
     */
    private ServicoDTO toDTO(Servico entity) {
        ServicoDTO dto = new ServicoDTO();
        dto.setId(entity.getId());
        dto.setDescricao(entity.getDescricao());
        dto.setPreco(entity.getPreco());
        return dto;
    }

    /**
     * Mapeia entidade para DTO com dados de prestador (para lista/busca)
     */
    private ServicoListaDTO mapToListaDTO(Servico servico) {
        Double avaliacaoMedia = avaliacaoRepository.getAvaliacaoMedia(servico.getUsuario().getId());
        Long numAvaliacoes = avaliacaoRepository.countByPrestador(servico.getUsuario().getId());

        ServicoListaDTO.PrestaServicoDTO prestadorDTO = ServicoListaDTO.PrestaServicoDTO.builder()
            .id(servico.getUsuario().getId())
            .nome(servico.getUsuario().getNome())
            .avaliacaoMedia(avaliacaoMedia != null ? avaliacaoMedia : 0)
            .numAvaliacoes(numAvaliacoes)
            .fotoUrl("/api/v1/usuarios/" + servico.getUsuario().getId() + "/foto")
            .build();

        return ServicoListaDTO.builder()
            .id(servico.getId())
            .titulo(servico.getTitulo())
            .descricao(servico.getDescricao())
            .preco(servico.getPreco())
            .categoria(servico.getCategoria())
            .dataCriacao(servico.getDataCriacao())
            .prestador(prestadorDTO)
            .build();
    }

    /**
     * Cria um novo serviço com validação de tenant (prestador)
     * 
     * @param tenantId ID do usuário logado (do JWT)
     * @param titulo Título do serviço
     * @param descricao Descrição detalhada
     * @param categoria Categoria (ex: pintura, encanamento)
     * @param preco Preço em BigDecimal
     * @return ServicoDTO criado
     */
    public ServicoDTO criarComTenant(Long tenantId, String titulo, String descricao, String categoria, Double preco) {
        // 1. Validar inputs
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("Título é obrigatório");
        }
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição é obrigatória");
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new IllegalArgumentException("Categoria é obrigatória");
        }
        if (preco == null || preco <= 0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero");
        }

        // 2. Validar que usuário logado existe
        Usuario usuario = usuarioRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // 3. Criar entidade
        Servico servico = new Servico();
        servico.setUsuario(usuario);  // Isolamento: apenas quem criou pode editar
        servico.setTitulo(titulo.trim());
        servico.setDescricao(descricao.trim());
        servico.setCategoria(categoria.trim());
        servico.setPreco(preco);
        servico.setAtivo(true);  // Ativo por padrão
        servico.setStatus("PENDENTE");  // Status inicial

        // 4. Salvar
        servico = servicoRepository.save(servico);

        log.info("ServicoService: Serviço {} criado por tenant {}", servico.getId(), tenantId);

        return toDTO(servico);
    }

    /**
     * Atualiza serviço com validação de tenant (somente dono pode editar)
     */
    public ServicoDTO atualizarComTenant(Long tenantId, Long servicoId, String titulo, String descricao, String categoria, Double preco) {
        // 1. Validar que serviço existe
        Servico servico = servicoRepository.findById(servicoId)
            .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado"));

        // 2. Validar tenant = dono
        if (!servico.getUsuario().getId().equals(tenantId)) {
            throw new SecurityException("Acesso negado: apenas o dono pode atualizar");
        }

        // 3. Atualizar campos
        if (titulo != null && !titulo.trim().isEmpty()) {
            servico.setTitulo(titulo.trim());
        }
        if (descricao != null && !descricao.trim().isEmpty()) {
            servico.setDescricao(descricao.trim());
        }
        if (categoria != null && !categoria.trim().isEmpty()) {
            servico.setCategoria(categoria.trim());
        }
        if (preco != null && preco > 0) {
            servico.setPreco(preco);
        }

        // 4. Salvar
        servico = servicoRepository.save(servico);

        log.info("ServicoService: Serviço {} atualizado por tenant {}", servicoId, tenantId);

        return toDTO(servico);
    }

    /**
     * Deleta serviço com validação de tenant
     */
    public void deletarComTenant(Long tenantId, Long servicoId) {
        // 1. Validar que existe
        Servico servico = servicoRepository.findById(servicoId)
            .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado"));

        // 2. Validar tenant = dono
        if (!servico.getUsuario().getId().equals(tenantId)) {
            throw new SecurityException("Acesso negado: apenas o dono pode deletar");
        }

        // 3. Deletar (soft delete ou hard delete)
        servico.setAtivo(false);  // Soft delete
        servicoRepository.save(servico);

        log.info("ServicoService: Serviço {} deletado por tenant {}", servicoId, tenantId);
    }
}