package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.GeocodeRequestDTO;
import ads.uninassau.brjobs.dto.GeocodeResponseDTO;
import ads.uninassau.brjobs.dto.PublicacaoServicoDTO;
import ads.uninassau.brjobs.model.PublicacaoServico;
import ads.uninassau.brjobs.model.TipoPublicacaoServico;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.PublicacaoServicoRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicacaoServicoService {

    private final PublicacaoServicoRepository publicacaoServicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PublicacaoCacheService publicacaoCacheService;
    private final GeocodeService geocodeService;

    public PublicacaoServicoDTO criarComTenant(Long tenantId, PublicacaoServicoDTO dto) {
        validarEntrada(dto);

        Usuario usuario = usuarioRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        TipoPublicacaoServico tipo = parseTipo(dto.getTipoPublicacao());
        GeocodeResponseDTO geocode = resolveGeocode(dto);

        // Neste módulo, qualquer usuário autenticado pode publicar PRESTACAO ou CONTRATACAO.
        // O tipo da publicação é validado por payload (parseTipo) e pelos campos obrigatórios.
        log.info("PublicacaoServicoService: tenant {} publicando tipo {}", tenantId, tipo);

        PublicacaoServico entity = PublicacaoServico.builder()
            .tipoPublicacao(tipo)
            .titulo(dto.getTitulo().trim())
            .descricao(dto.getDescricao().trim())
            .categoria(dto.getCategoria() == null ? null : dto.getCategoria().trim())
            .enderecoPublicacao(dto.getEnderecoPublicacao().trim())
            .cepPublicacao(blankToNull(dto.getCepPublicacao()))
            .cidadePublicacao(blankToNull(dto.getCidadePublicacao()))
            .estadoPublicacao(normalizeUf(dto.getEstadoPublicacao()))
            .latitude(geocode.getLat())
            .longitude(geocode.getLng())
            .geocodeProvider(geocode.getSource())
            .geocodePrecision(geocode.getPrecision())
            .preco(dto.getPreco())
            .orcamentoMin(dto.getOrcamentoMin())
            .orcamentoMax(dto.getOrcamentoMax())
            .status("ATIVA")
            .usuario(usuario)
            .ativo(true)
            .build();

        entity = publicacaoServicoRepository.save(entity);
        publicacaoCacheService.evictAll();
        log.info("PublicacaoServicoService: publicacao {} criada por tenant {}", entity.getId(), tenantId);

        return toDTO(entity);
    }

    public List<PublicacaoServicoDTO> listar(String tipo) {
        return buscarPaginado(tipo, null, 0, 20, null, null).getContent();
    }

    public Page<PublicacaoServicoDTO> buscarPaginado(String tipo, String termo, int page, int size) {
        return buscarPaginado(tipo, termo, page, size, null, null);
    }

    public Page<PublicacaoServicoDTO> buscarPaginado(String tipo, String termo, int page, int size, Double lat, Double lng) {
        int pageSeguro = Math.max(page, 0);
        int sizeSeguro = Math.min(Math.max(size, 1), 20);
        Pageable pageable = PageRequest.of(pageSeguro, sizeSeguro);

        TipoPublicacaoServico tipoFiltro = null;
        if (tipo != null && !tipo.isBlank()) {
            tipoFiltro = parseTipo(tipo);
        }

        String termoFiltro = null;
        if (termo != null && !termo.isBlank()) {
            termoFiltro = termo.trim();
        }

        if (termoFiltro == null) {
            return publicacaoServicoRepository
                .buscarAtivasPaginado(tipoFiltro, pageable)
                .map(entity -> toDTO(entity, lat, lng));
        }

        List<PublicacaoServico> base = (tipoFiltro == null)
            ? publicacaoServicoRepository.findAtivasOrdenadas()
            : publicacaoServicoRepository.findAtivasOrdenadasPorTipo(tipoFiltro);

        String termoLower = termoFiltro.toLowerCase(Locale.ROOT);
        List<PublicacaoServicoDTO> filtradas = new ArrayList<>();
        for (PublicacaoServico entity : base) {
            String titulo = safeLower(entity.getTitulo());
            String descricao = safeLower(entity.getDescricao());
            String categoria = safeLower(entity.getCategoria());
            String usuarioNome = safeLower(entity.getUsuario() != null ? entity.getUsuario().getNome() : null);
            String usuarioEndereco = safeLower(entity.getUsuario() != null ? entity.getUsuario().getEndereco() : null);
            String usuarioCidade = safeLower(entity.getUsuario() != null ? entity.getUsuario().getCidade() : null);
            String usuarioBairro = safeLower(entity.getUsuario() != null ? entity.getUsuario().getBairro() : null);

            if (titulo.contains(termoLower)
                || descricao.contains(termoLower)
                || categoria.contains(termoLower)
                || usuarioNome.contains(termoLower)
                || usuarioEndereco.contains(termoLower)
                || usuarioCidade.contains(termoLower)
                || usuarioBairro.contains(termoLower)) {
                filtradas.add(toDTO(entity, lat, lng));
            }
        }

        int start = (int) pageable.getOffset();
        if (start >= filtradas.size()) {
            return new PageImpl<>(List.of(), pageable, filtradas.size());
        }

        int end = Math.min(start + pageable.getPageSize(), filtradas.size());
        return new PageImpl<>(filtradas.subList(start, end), pageable, filtradas.size());
    }

    public PublicacaoServicoDTO buscarPorId(Long id) {
        PublicacaoServico entity = publicacaoServicoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Publicacao nao encontrada"));

        if (!Boolean.TRUE.equals(entity.getAtivo())) {
            throw new IllegalArgumentException("Publicacao nao encontrada");
        }

        return toDTO(entity);
    }

    public List<PublicacaoServicoDTO> listarMinhas(Long tenantId) {
        return publicacaoServicoRepository.findByUsuarioIdAndAtivoTrueOrderByDataCriacaoDesc(tenantId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    public void encerrarComTenant(Long tenantId, Long id) {
        PublicacaoServico entity = publicacaoServicoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Publicacao nao encontrada"));

        if (!entity.getUsuario().getId().equals(tenantId)) {
            throw new SecurityException("Acesso negado: somente o dono pode encerrar a publicacao");
        }

        entity.setStatus("ENCERRADA");
        entity.setAtivo(false);
        publicacaoServicoRepository.save(entity);
        publicacaoCacheService.evictAll();
    }

    private void validarEntrada(PublicacaoServicoDTO dto) {
        if (dto.getTipoPublicacao() == null || dto.getTipoPublicacao().isBlank()) {
            throw new IllegalArgumentException("tipoPublicacao e obrigatorio");
        }
        if (dto.getTitulo() == null || dto.getTitulo().isBlank()) {
            throw new IllegalArgumentException("titulo e obrigatorio");
        }
        if (dto.getDescricao() == null || dto.getDescricao().isBlank()) {
            throw new IllegalArgumentException("descricao e obrigatoria");
        }
        if (dto.getEnderecoPublicacao() == null || dto.getEnderecoPublicacao().isBlank()) {
            throw new IllegalArgumentException("enderecoPublicacao e obrigatorio");
        }
        validarCoordenadas(dto.getLatitude(), dto.getLongitude());

        TipoPublicacaoServico tipo = parseTipo(dto.getTipoPublicacao());
        if (tipo == TipoPublicacaoServico.PRESTACAO) {
            if (dto.getPreco() == null || dto.getPreco() <= 0) {
                throw new IllegalArgumentException("preco e obrigatorio para publicacao de prestacao");
            }
        } else {
            if (dto.getOrcamentoMin() == null || dto.getOrcamentoMax() == null) {
                throw new IllegalArgumentException("orcamentoMin e orcamentoMax sao obrigatorios para contratacao");
            }
            if (dto.getOrcamentoMin() < 0 || dto.getOrcamentoMax() <= 0 || dto.getOrcamentoMax() < dto.getOrcamentoMin()) {
                throw new IllegalArgumentException("faixa de orcamento invalida");
            }
        }
    }

    private TipoPublicacaoServico parseTipo(String valor) {
        try {
            return TipoPublicacaoServico.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("tipoPublicacao deve ser PRESTACAO ou CONTRATACAO");
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private PublicacaoServicoDTO toDTO(PublicacaoServico entity) {
        return toDTO(entity, null, null);
    }

    private PublicacaoServicoDTO toDTO(PublicacaoServico entity, Double userLat, Double userLng) {
        PublicacaoServicoDTO dto = new PublicacaoServicoDTO();
        String enderecoUsuario = entity.getUsuario() != null ? entity.getUsuario().getEndereco() : null;

        dto.setId(entity.getId());
        dto.setTipoPublicacao(entity.getTipoPublicacao().name());
        dto.setTitulo(entity.getTitulo());
        dto.setDescricao(entity.getDescricao());
        dto.setCategoria(entity.getCategoria());
        dto.setEnderecoPublicacao(entity.getEnderecoPublicacao());
        dto.setCepPublicacao(entity.getCepPublicacao());
        dto.setCidadePublicacao(entity.getCidadePublicacao());
        dto.setEstadoPublicacao(entity.getEstadoPublicacao());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setGeocodeProvider(entity.getGeocodeProvider());
        dto.setGeocodePrecision(entity.getGeocodePrecision());
        dto.setDistanceKm(calcularDistanciaKm(userLat, userLng, entity.getLatitude(), entity.getLongitude()));
        dto.setPreco(entity.getPreco());
        dto.setOrcamentoMin(entity.getOrcamentoMin());
        dto.setOrcamentoMax(entity.getOrcamentoMax());
        dto.setStatus(entity.getStatus());
        dto.setUsuarioId(entity.getUsuario().getId());
        dto.setUsuarioNome(entity.getUsuario().getNome());
        dto.setUsuarioEndereco(enderecoUsuario);
        dto.setUsuarioCidade(entity.getUsuario() != null ? entity.getUsuario().getCidade() : null);
        dto.setUsuarioBairro(entity.getUsuario() != null ? entity.getUsuario().getBairro() : null);
        dto.setIsHighlighted(Boolean.TRUE.equals(entity.getIsHighlighted()) && (entity.getHighlightExpiresAt() == null || entity.getHighlightExpiresAt().isAfter(java.time.LocalDateTime.now())));
        dto.setHighlightExpiresAt(entity.getHighlightExpiresAt());
        dto.setHighlightPlanId(entity.getHighlightPlan() != null ? entity.getHighlightPlan().getId() : null);
        dto.setHighlightPlanName(entity.getHighlightPlan() != null ? entity.getHighlightPlan().getName() : null);
        dto.setHighlightPriority(entity.getHighlightPlan() != null ? entity.getHighlightPlan().getPriority() : null);
        dto.setDataCriacao(entity.getDataCriacao());
        return dto;
    }

    private GeocodeResponseDTO resolveGeocode(PublicacaoServicoDTO dto) {
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            return GeocodeResponseDTO.builder()
                    .lat(dto.getLatitude())
                    .lng(dto.getLongitude())
                    .source(blankToNull(dto.getGeocodeProvider()) != null ? dto.getGeocodeProvider() : "browser")
                    .precision(blankToNull(dto.getGeocodePrecision()) != null ? dto.getGeocodePrecision() : "exact")
                    .build();
        }

        GeocodeRequestDTO request = new GeocodeRequestDTO();
        request.setEndereco(dto.getEnderecoPublicacao());
        request.setCep(dto.getCepPublicacao());
        request.setCidade(dto.getCidadePublicacao());
        request.setEstado(dto.getEstadoPublicacao());
        return geocodeService.geocode(request);
    }

    private void validarCoordenadas(Double lat, Double lng) {
        if ((lat == null && lng != null) || (lat != null && lng == null)) {
            throw new IllegalArgumentException("latitude e longitude devem ser enviadas juntas");
        }
        if (lat != null && (lat < -90 || lat > 90)) {
            throw new IllegalArgumentException("latitude invalida");
        }
        if (lng != null && (lng < -180 || lng > 180)) {
            throw new IllegalArgumentException("longitude invalida");
        }
    }

    private Double calcularDistanciaKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }

        double earthKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthKm * c * 10.0) / 10.0;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeUf(String value) {
        String uf = blankToNull(value);
        return uf == null ? null : uf.toUpperCase(Locale.ROOT);
    }
}
