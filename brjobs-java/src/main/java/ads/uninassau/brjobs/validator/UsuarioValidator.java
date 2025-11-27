package ads.uninassau.brjobs.validator;

import ads.uninassau.brjobs.exception.InvalidPasswordException;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Validador centralizado para dados do usuário.
 * Responsável por validações de negócio e formato.
 */
public class UsuarioValidator {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");
    private static final Pattern TELEFONE_PATTERN = Pattern.compile("\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}");
    private static final Pattern SENHA_PATTERN =
        Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    private static final int IDADE_MINIMA = 18;
    private static final int IDADE_MAXIMA = 120;

    /**
     * Valida o formato do CPF
     */
    public static boolean validarCPF(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }
        // Remove caracteres especiais
        String cpfLimpo = cpf.replaceAll("\\D", "");
        return cpfLimpo.length() == 11 && CPF_PATTERN.matcher(cpf).matches();
    }

    /**
     * Valida o formato do telefone
     */
    public static boolean validarTelefone(String telefone) {
        if (telefone == null || telefone.trim().isEmpty()) {
            return false;
        }
        return TELEFONE_PATTERN.matcher(telefone).matches();
    }

    /**
     * Valida a força da senha.
     * Requisitos: mínimo 8 caracteres, pelo menos 1 maiúscula, 1 minúscula, 1 número, 1 caractere especial
     *
     * @throws InvalidPasswordException se a senha não atender aos requisitos
     */
    public static void validarSenha(String senha) {
        if (senha == null || senha.isEmpty()) {
            throw new InvalidPasswordException("A senha não pode estar vazia.");
        }

        if (senha.length() < 8) {
            throw new InvalidPasswordException("A senha deve conter no mínimo 8 caracteres.");
        }

        if (!SENHA_PATTERN.matcher(senha).matches()) {
            throw new InvalidPasswordException(
                "A senha deve conter pelo menos: 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial (@$!%*?&)."
            );
        }
    }

    /**
     * Valida a data de nascimento (verifica idade mínima e máxima)
     */
    public static void validarDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            throw new IllegalArgumentException("A data de nascimento não pode ser nula.");
        }

        if (dataNascimento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data de nascimento não pode ser no futuro.");
        }

        int idade = Period.between(dataNascimento, LocalDate.now()).getYears();

        if (idade < IDADE_MINIMA) {
            throw new IllegalArgumentException("O usuário deve ter no mínimo " + IDADE_MINIMA + " anos.");
        }

        if (idade > IDADE_MAXIMA) {
            throw new IllegalArgumentException("A data de nascimento informada é inválida.");
        }
    }

    /**
     * Valida o email (formato básico, validação mais rigorosa pode ser feita com @Email)
     */
    public static boolean validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }
}

