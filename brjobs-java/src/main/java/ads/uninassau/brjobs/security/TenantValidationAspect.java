package ads.uninassau.brjobs.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Aspect que intercepta métodos anotados com @ValidateTenant e valida isolamento por tenant.
 *
 * Fluxo:
 * 1. Intercepta chamada a método @ValidateTenant
 * 2. Extrai tenant_id do request (setAttribute pelo TenantFilter)
 * 3. Extrai primeiro argumento do método (tenantId esperado)
 * 4. Compara: tenant_id (JWT) == tenantId (argumento)
 * 5. Se não bater → lança AccessDeniedException (403 Forbidden)
 *
 * Uso:
 * @ValidateTenant
 * public Servico getServico(Long prestadorId, Long servicoId) {
 *     // prestadorId é comparado com tenant_id do JWT
 * }
 */
@Aspect
@Component
@Slf4j
public class TenantValidationAspect {

    /**
     * Valida que o primeiro argumento do método (@ValidateTenant) corresponde ao tenant_id do JWT.
     */
    @Before("@annotation(ads.uninassau.brjobs.security.ValidateTenant)")
    public void validateTenantAccess(JoinPoint joinPoint) throws Throwable {
        // 1. Extrair tenant_id do request (setAttribute pelo TenantFilter)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("TenantValidationAspect: Nenhum request context encontrado");
            throw new AccessDeniedException("Acesso negado: sem contexto de request");
        }

        HttpServletRequest request = attributes.getRequest();
        Object tenantIdFromRequest = request.getAttribute("tenant_id");

        if (tenantIdFromRequest == null) {
            log.warn("TenantValidationAspect: Nenhum tenant_id no request");
            throw new AccessDeniedException("Acesso negado: usuário não autenticado ou tenant ausente");
        }

        // 2. Extrair primeiro argumento (tenantId esperado)
        Object[] methodArgs = joinPoint.getArgs();
        if (methodArgs == null || methodArgs.length == 0) {
            log.warn("TenantValidationAspect: Método sem argumentos: {}.{}", 
                joinPoint.getTarget().getClass().getSimpleName(), 
                joinPoint.getSignature().getName());
            // Métodos sem argumentos não podem validar tenant, permitir passa
            return;
        }

        Object tenantIdFromMethod = methodArgs[0];

        // 3. Comparar
        if (!tenantIdFromRequest.equals(tenantIdFromMethod)) {
            log.warn("TenantValidationAspect: Tentativa de acesso negado. " +
                    "tenant_id do request='{}' != tenantId do método='{}'",
                tenantIdFromRequest, tenantIdFromMethod);

            // Log estruturado para auditoria
            logAccessDenial(request, tenantIdFromRequest, tenantIdFromMethod);

            throw new AccessDeniedException("Acesso negado: tenant mismatch");
        }

        log.debug("TenantValidationAspect: Acesso validado para tenant='{}'", tenantIdFromRequest);
    }

    /**
     * Log estruturado de tentativas de acesso negado (audit trail)
     */
    private void logAccessDenial(HttpServletRequest request, Object expectedTenant, Object attemptedTenant) {
        String remoteIp = request.getRemoteAddr();
        String method = request.getMethod();
        String path = request.getRequestURI();
        long timestamp = System.currentTimeMillis();

        log.warn("SECURITY_AUDIT: Access Denied - " +
                "Expected Tenant={}, Attempted Tenant={}, IP={}, Method={}, Path={}, Timestamp={}",
                expectedTenant, attemptedTenant, remoteIp, method, path, timestamp);
    }
}
