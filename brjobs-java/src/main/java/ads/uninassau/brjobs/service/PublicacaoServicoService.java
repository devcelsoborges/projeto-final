package ads.uninassau.brjobs.service;

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

    public PublicacaoServicoDTO criarComTenant(Long tenantId, PublicacaoServicoDTO dto) {
        validarEntrada(dto);

        Usuario usuario = usuarioRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        TipoPublicacaoServico tipo = parseTipo(dto.getTipoPublicacao());

        // Neste módulo, qualquer usuário autenticado pode publicar PRESTACAO ou CONTRATACAO.
        // O tipo da publicação é validado por payload (parseTipo) e pelos campos obrigatórios.
        log.info("PublicacaoServicoService: tenant {} publicando tipo {}", tenantId, tipo);

        PublicacaoServico entity = PublicacaoServico.builder()
            .tipoPublicacao(tipo)
            .titulo(dto.getTitulo().trim())
            .descricao(dto.getDescricao().trim())
            .categoria(dto.getCategoria() == null ? null : dto.getCategoria().trim())
            .preco(dto.getPreco())
            .orcamentoMin(dto.getOrcamentoMin())
            .orcamentoMax(dto.getOrcamentoMax())
            .status("ATIVA")
            .usuario(usuario)
            .ativo(true)
            .build();

        entity = publicacaoServicoRepository.save(entity);
        log.info("PublicacaoServicoService: publicacao {} criada por tenant {}", entity.getId(), tenantId);

        return toDTO(entity);
    }

    public List<PublicacaoServicoDTO> listar(String tipo) {
        return buscarPaginado(tipo, null, 0, 20).getContent();
    }

    public Page<PublicacaoServicoDTO> buscarPaginado(String tipo, String termo, int page, int size) {
        int pageSeguro = Math.max(page, 0);
        int sizeSeguro = Math.min(Math.max(size, 1), 50);
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
                .map(this::toDTO);
        }

        List<PublicacaoServico> base = (tipoFiltro == null)
            ? publicacaoServicoRepository.findByAtivoTrueOrderByDataCriacaoDesc()
            : publicacaoServicoRepository.findByAtivoTrueAndTipoPublicacaoOrderByDataCriacaoDesc(tipoFiltro);

        String termoLower = termoFiltro.toLowerCase(Locale.ROOT);
        List<PublicacaoServicoDTO> filtradas = new ArrayList<>();
        for (PublicacaoServico entity : base) {
            String titulo = safeLower(entity.getTitulo());
            String descricao = safeLower(entity.getDescricao());
            String categoria = safeLower(entity.getCategoria());
            String usuarioNome = safeLower(entity.getUsuario() != null ? entity.getUsuario().getNome() : null);

            if (titulo.contains(termoLower)
                || descricao.contains(termoLower)
                || categoria.contains(termoLower)
                || usuarioNome.contains(termoLower)) {
                filtradas.add(toDTO(entity));
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
        PublicacaoServicoDTO dto = new PublicacaoServicoDTO();
        dto.setId(entity.getId());
        dto.setTipoPublicacao(entity.getTipoPublicacao().name());
        dto.setTitulo(entity.getTitulo());
        dto.setDescricao(entity.getDescricao());
        dto.setCategoria(entity.getCategoria());
        dto.setPreco(entity.getPreco());
        dto.setOrcamentoMin(entity.getOrcamentoMin());
        dto.setOrcamentoMax(entity.getOrcamentoMax());
        dto.setStatus(entity.getStatus());
        dto.setUsuarioId(entity.getUsuario().getId());
        dto.setUsuarioNome(entity.getUsuario().getNome());
        dto.setDataCriacao(entity.getDataCriacao());
        return dto;
    }
}
