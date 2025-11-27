package ads.uninassau.brjobs.dto;

import ads.uninassau.brjobs.model.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para resposta de dados do usuário.
 * Este DTO é utilizado para retornar informações do usuário após operações CRUD.
 * Para cadastro, utilize CadastroContratanteDTO ou CadastroPrestadorDTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    private Long id;
    private TipoUsuario tipoUsuario;
    private String nome;
    private String email;
    private String telefone;
    private String endereco;
    private String cpf;
    private String genero;
    private LocalDate dataNascimento;
    private LocalDate dataCadastro;
    private boolean ativo;
}