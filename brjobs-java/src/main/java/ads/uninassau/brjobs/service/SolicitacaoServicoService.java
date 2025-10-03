package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.SolicitacaoServicoDTO;
import ads.uninassau.brjobs.model.SolicitacaoServico;
import ads.uninassau.brjobs.repository.SolicitacaoServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitacaoServicoService {

    @Autowired
    private SolicitacaoServicoRepository solicitacaoServicoRepository;

    public SolicitacaoServicoDTO criarSolicitacao(SolicitacaoServicoDTO dto) {
        SolicitacaoServico solicitacao = toEntity(dto);
        solicitacao = solicitacaoServicoRepository.save(solicitacao);
        return toDTO(solicitacao);
    }

    public List<SolicitacaoServicoDTO> listarSolicitacoes() {
        return solicitacaoServicoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private SolicitacaoServico toEntity(SolicitacaoServicoDTO dto) {
        SolicitacaoServico entity = new SolicitacaoServico();
        entity.setStatus(dto.getStatus());
        entity.setDataSolicitacao(dto.getDataSolicitacao());
        return entity;
    }

    private SolicitacaoServicoDTO toDTO(SolicitacaoServico entity) {
        SolicitacaoServicoDTO dto = new SolicitacaoServicoDTO();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setDataSolicitacao(entity.getDataSolicitacao());
        return dto;
    }
}