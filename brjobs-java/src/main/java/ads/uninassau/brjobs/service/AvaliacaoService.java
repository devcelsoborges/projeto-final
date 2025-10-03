package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.AvaliacaoDTO;
import ads.uninassau.brjobs.model.Avaliacao;
import ads.uninassau.brjobs.repository.AvaliacaoRepository;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
@Service
public class AvaliacaoService {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoDTO criarAvaliacao(AvaliacaoDTO avaliacaoDTO) {
        Avaliacao avaliacao = toEntity(avaliacaoDTO);
        avaliacao = avaliacaoRepository.save(avaliacao);
        return toDTO(avaliacao);
    }

    public List<AvaliacaoDTO> listarAvaliacoes() {
        return avaliacaoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Avaliacao toEntity(AvaliacaoDTO dto) {
        Avaliacao entity = new Avaliacao();
        entity.setNota(dto.getNota());
        entity.setComentario(dto.getComentario());
        return entity;
    }

    private AvaliacaoDTO toDTO(Avaliacao entity) {
        AvaliacaoDTO dto = new AvaliacaoDTO();
        dto.setId(entity.getId());
        dto.setNota(entity.getNota());
        dto.setComentario(entity.getComentario());

        if (entity.getSolicitacao() != null) {
            dto.setSolicitacaoId(entity.getSolicitacao().getId());

            if (entity.getSolicitacao().getUsuario() != null) {
                dto.setUsuarioId(entity.getSolicitacao().getUsuario().getId());
            }
            if (entity.getSolicitacao().getServico() != null
                    && entity.getSolicitacao().getServico().getPrestador() != null) {
                dto.setPrestadorId(entity.getSolicitacao().getServico().getPrestador().getId());
            }
        }

        return dto;
    }
}
