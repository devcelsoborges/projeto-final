package ads.uninassau.brjobs.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para cadastro de prestador de serviço
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CadastroPrestadorDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
    private String nome;

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "O formato do email é inválido.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    private String senha; // Validação de complexidade será feita no service

    @NotBlank(message = "O telefone é obrigatório.")
    private String telefone; // Validação de formato será feita no service

    @NotNull(message = "A data de nascimento é obrigatória.")
    private LocalDate dataNascimento; // Validação de idade será feita no service

    @NotBlank(message = "O CPF é obrigatório.")
    private String cpf; // Validação de formato será feita no service

    @NotBlank(message = "O gênero é obrigatório.")
    private String genero;

    @NotBlank(message = "O endereço é obrigatório.")
    @Size(min = 5, max = 255, message = "O endereço deve ter entre 5 e 255 caracteres.")
    private String endereco;

    // Campos específicos de Prestador
    @NotBlank(message = "A função/profissão é obrigatória.")
    @Size(min = 3, max = 100, message = "A função deve ter entre 3 e 100 caracteres.")
    private String funcao;

    @Size(max = 1000, message = "A experiência profissional deve ter no máximo 1000 caracteres.")
    private String experienciaProfissional;

    @Size(max = 500, message = "As especialidades devem ter no máximo 500 caracteres.")
    private String especialidades;
}

