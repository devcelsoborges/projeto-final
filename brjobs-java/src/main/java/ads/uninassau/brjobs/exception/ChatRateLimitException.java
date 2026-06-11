package ads.uninassau.brjobs.exception;

public class ChatRateLimitException extends RuntimeException {

    public ChatRateLimitException(String message) {
        super(message);
    }
}
