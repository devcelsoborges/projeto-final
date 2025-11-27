package ads.uninassau.brjobs.exception;

public class CPFAlreadyInUseException extends RuntimeException {
    public CPFAlreadyInUseException(String cpf) {
        super("O CPF " + cpf + " já está cadastrado no sistema.");
    }
}