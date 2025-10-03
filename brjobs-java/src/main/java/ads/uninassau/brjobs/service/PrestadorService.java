package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.PrestadorDTO;
import ads.uninassau.brjobs.entity.Prestador;
import ads.uninassau.brjobs.repository.PrestadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PrestadorService {

    @Autowired
    private PrestadorRepository prestadorRepository;

    public PrestadorDTO criarPrestador(PrestadorDTO prestadorDTO) {
        Prestador prestador = toEntity(prestadorDTO);
        prestador = prestadorRepository.save(prestador);
        return toDTO(prestador);
    }

    public Optional<PrestadorDTO> buscarPorId(Long id) {
        return prestadorRepository.findById(id).map(this::toDTO);
    }

    public List<PrestadorDTO> listarTodos() {
        return prestadorRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Prestador toEntity(PrestadorDTO dto) {
        Prestador entity = new Prestador();
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        return entity;
    }

    private PrestadorDTO toDTO(Prestador entity) {
        PrestadorDTO dto = new PrestadorDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        return dto;
    }
}