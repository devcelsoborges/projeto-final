package ads.uninassau.brjobs.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para marcar métodos que requerem validação de isolamento por tenant.
 *
 * Uso:
 * @ValidateTenant
 * public Servico getServico(Long tenantId, Long servicoId) { ... }
 *
 * O AspectJ irá interceptar e validar que:
 * 1. O tenantId do JWT (no request) == primeiro argumento tenantId (do método)
 *
 * Se não bater, lança AccessDeniedException (403 Forbidden).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateTenant {
    /**
     * Se true, vai validar o primeiro argumento como tenantId.
     * Se false, bypass da validação (para métodos públicos sem parametrização).
     */
    boolean enabled() default true;
}
