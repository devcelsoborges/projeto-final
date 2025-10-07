package ads.uninassau.brjobs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    private Long id;
    private String tipoUsuario; // trabalhador ou contratante
    private String nome;
    private String email;
    private String senha;
    private String telefone;
    private String endereco;
    private String cpf;
    private String funcao; // cargo
    private String dataNascimento;
    private String genero;
    private String experienciaProfissional;
    private String especialidades;
    private MultipartFile fotoPerfil; // Upload Angular
    private MultipartFile curriculo;  // Upload Angular
}
