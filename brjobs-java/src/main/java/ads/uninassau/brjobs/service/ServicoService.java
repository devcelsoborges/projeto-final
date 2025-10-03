package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.ServicoDTO;
import ads.uninassau.brjobs.model.Servico;
import ads.uninassau.brjobs.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServicoService {

    @Autowired
    private ServicoRepository servicoRepository;

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

    private Servico toEntity(ServicoDTO dto) {
        Servico entity = new Servico();
        entity.setDescricao(dto.getDescricao());
        entity.setPreco(dto.getPreco());
        return entity;
    }

    private ServicoDTO toDTO(Servico entity) {
        ServicoDTO dto = new ServicoDTO();
        dto.setId(entity.getId());
        dto.setDescricao(entity.getDescricao());
        dto.setPreco(entity.getPreco());
        return dto;
    }
}