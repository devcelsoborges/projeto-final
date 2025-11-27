package ads.uninassau.brjobs.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Usuário não encontrado com o ID: " + id);
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}

