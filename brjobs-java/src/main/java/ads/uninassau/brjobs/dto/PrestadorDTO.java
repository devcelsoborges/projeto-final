package ads.uninassau.brjobs.dto;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class PrestadorDTO {

    private Long id;

    private String nome;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private String descricao;
    private String experienciaProfissional;
    private String especialidades;
    private MultipartFile fotoPerfil; // Upload Angular
    private MultipartFile curriculo;  // Upload Angular
}

