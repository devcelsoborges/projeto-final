package ads.uninassau.brjobs.exception;

/**
 * Lançada quando o login é bem-sucedido (credenciais corretas) mas a conta ainda não
 * teve o e-mail confirmado. Só bloqueia contas explicitamente não confirmadas — contas
 * antigas (sem o campo) e sociais não são afetadas.
 */
public class EmailNotConfirmedException extends RuntimeException {

    public EmailNotConfirmedException(String message) {
        super(message);
    }
}
