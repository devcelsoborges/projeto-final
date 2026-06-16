package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.RelatorioGanhosDTO;
import ads.uninassau.brjobs.model.RelatorioGanhosCache;
import ads.uninassau.brjobs.model.RelatorioGanhosCategoriaBreakdown;
import ads.uninassau.brjobs.model.RelatorioGanhosClienteBreakdown;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.RelatorioGanhosCacheRepository;
import ads.uninassau.brjobs.repository.RelatorioGanhosCategoriaBreakdownRepository;
import ads.uninassau.brjobs.repository.RelatorioGanhosClienteBreakdownRepository;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GanhosService {

    private final RelatorioGanhosCacheRepository cacheRepository;
    private final RelatorioGanhosCategoriaBreakdownRepository categoriaRepository;
    private final RelatorioGanhosClienteBreakdownRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Gera relatório de ganhos para um prestador (tenant-aware)
     *
     * @param tenantId ID do usuário logado (do JWT)
     * @param ano Ano (ex: 2026)
     * @param mes Mês (1-12)
     * @return RelatorioGanhosDTO
     */
    public RelatorioGanhosDTO gerar(Long tenantId, int ano, int mes) {
        // 1. Validar que é PRESTADOR
        Usuario usuario = usuarioRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!usuario.isPrestador()) {
            throw new AccessDeniedException("Apenas prestadores podem acessar relatório de ganhos");
        }

        // 2. Validar mes/ano
        if (mes < 1 || mes > 12 || ano < 2000) {
            throw new IllegalArgumentException("Mês/ano inválidos");
        }

        // 3. Buscar no cache
        YearMonth mesAnoRequest = YearMonth.of(ano, mes);
        RelatorioGanhosCache cache = cacheRepository.findByPrestadorIdAndMesAno(tenantId, mesAnoRequest)
            .orElse(null);

        // 4. Montar resposta
        RelatorioGanhosDTO dto = new RelatorioGanhosDTO();
        dto.setMes(mesAnoRequest);

        if (cache == null) {
            // Nenhum dado para este mês
            dto.setTotalFaturado(BigDecimal.ZERO);
            dto.setNumServicos(0);
            dto.setPorCategoria(List.of());
            dto.setPorCliente(List.of());
            log.debug("GanhosService: Nenhum ganho para prestador {} em {}/{}", tenantId, mes, ano);
        } else {
            dto.setTotalFaturado(cache.getTotalFaturado());
            dto.setNumServicos(cache.getNumServicos());

            // Buscar breakdowns
            List<RelatorioGanhosCategoriaBreakdown> categorias = categoriaRepository.findByCacheId(cache.getId());
            dto.setPorCategoria(categorias.stream()
                .map(c -> RelatorioGanhosDTO.BreakdownCategoriaDTO.builder()
                    .categoria(c.getCategoria())
                    .total(c.getTotal())
                    .numServicos(c.getNumServicos())
                    .build())
                .collect(Collectors.toList()));

            List<RelatorioGanhosClienteBreakdown> clientes = clienteRepository.findByCacheId(cache.getId());
            dto.setPorCliente(clientes.stream()
                .map(c -> RelatorioGanhosDTO.BreakdownClienteDTO.builder()
                    .clienteId(c.getCliente().getId())
                    .clienteNome(c.getClienteNome())
                    .total(c.getTotal())
                    .numServicos(c.getNumServicos())
                    .build())
                .collect(Collectors.toList()));

            log.info("GanhosService: Relatório gerado para prestador {} em {}/{}", tenantId, mes, ano);
        }

        return dto;
    }
}
