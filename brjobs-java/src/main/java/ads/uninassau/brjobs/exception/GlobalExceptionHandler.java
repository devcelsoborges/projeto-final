package ads.uninassau.brjobs.config;

import ads.uninassau.brjobs.exception.EmailAlreadyInUseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
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
}