package ads.uninassau.brjobs.exception;

import ads.uninassau.brjobs.exception.EmailAlreadyInUseException;
import ads.uninassau.brjobs.exception.CPFAlreadyInUseException;
import ads.uninassau.brjobs.exception.ChatRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Tratamento de exceções de Validação de DTOs (@Valid falhou).
     * Retorna HTTP 400 Bad Request com detalhes dos campos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // Coleta todos os erros de campo
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        // Retorna um JSON com o mapa de erros e status 400
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento de exceções de Unicidade (Email já existe).
     * Retorna HTTP 409 Conflict.
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<String> handleEmailAlreadyInUseException(EmailAlreadyInUseException ex) {
        // Retorna a mensagem de erro da exceção e status 409
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CPFAlreadyInUseException.class)
    public ResponseEntity<String> handleCPFAlreadyInUseException(CPFAlreadyInUseException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<String> handleInvalidPasswordException(InvalidPasswordException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Tratamento de usuário não encontrado.
     * Retorna HTTP 404 Not Found.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("GlobalExceptionHandler 404 UserNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Login com senha em conta criada via login social que ainda não definiu senha.
     * Retorna HTTP 401 com a orientação de entrar pelo provedor ou definir senha.
     */
    @ExceptionHandler(SocialOnlyAccountException.class)
    public ResponseEntity<String> handleSocialOnlyAccountException(SocialOnlyAccountException ex) {
        log.warn("GlobalExceptionHandler 401 SocialOnlyAccountException: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Login com credenciais corretas mas e-mail não confirmado. HTTP 403.
     */
    @ExceptionHandler(EmailNotConfirmedException.class)
    public ResponseEntity<Map<String, String>> handleEmailNotConfirmedException(EmailNotConfirmedException ex) {
        log.warn("GlobalExceptionHandler 403 EmailNotConfirmedException: {}", ex.getMessage());
        // Corpo estruturado com code para o front diferenciar de um 403 de permissão e
        // oferecer o reenvio do e-mail de confirmação.
        return new ResponseEntity<>(
                Map.of("message", ex.getMessage(), "code", "EMAIL_NOT_CONFIRMED"),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("GlobalExceptionHandler 400 IllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException ex) {
        log.warn("GlobalExceptionHandler 403 SecurityException: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ChatRateLimitException.class)
    public ResponseEntity<String> handleChatRateLimitException(ChatRateLimitException ex) {
        log.warn("GlobalExceptionHandler 429 ChatRateLimitException: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Violação de constraint do banco (ex.: índice único de e-mail/CPF). Retorna 409 em vez
     * de 500, cobrindo corridas em que a validação prévia não pegou o duplicado.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("GlobalExceptionHandler 409 DataIntegrityViolation: {}", causa);
        return new ResponseEntity<>("Este e-mail ou CPF já está em uso.", HttpStatus.CONFLICT);
    }

    /**
     * Corpo da requisição malformado/ilegível (ex.: JSON inválido). Mantém 400 em vez de
     * ser engolido pelo handler genérico de Exception (que retornaria 500).
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        String causa = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("GlobalExceptionHandler 400 corpo invalido: {}", causa);
        return new ResponseEntity<>("Requisição inválida.", HttpStatus.BAD_REQUEST);
    }

    /**
     * Rede de segurança: qualquer exceção não mapeada vira 500 com mensagem genérica
     * (sem vazar stacktrace ao cliente), em vez de um erro cru. Evita o front cair no
     * "erro ao processar a requisição" sem diagnóstico no servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        log.error("GlobalExceptionHandler 500 (exceção não mapeada)", ex);
        return new ResponseEntity<>("Erro ao processar a requisição. Tente novamente.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}