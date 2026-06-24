package ads.uninassau.brjobs.exception;

/**
 * Lançada quando um login com senha é tentado em uma conta criada via
 * login social (Google/Facebook/Apple) que ainda não definiu senha local.
 */
public class SocialOnlyAccountException extends RuntimeException {

    public SocialOnlyAccountException(String message) {
        super(message);
    }
}
