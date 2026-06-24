package ads.uninassau.brjobs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de e-mail transacional via Resend (https://resend.com).
 * Centraliza o envio HTTP para ser reusado por recuperação de senha e notificações
 * de chat. Quando a API key não está configurada, não envia e retorna false —
 * nunca lança, para não quebrar o fluxo que chamou.
 */
@Service
@Slf4j
public class MailService {

    private final RestTemplate restTemplate;

    @Value("${app.mail.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.mail.resend.from:BRJobs <onboarding@resend.dev>}")
    private String fromAddress;

    @Value("${app.mail.resend.base-url:https://api.resend.com/emails}")
    private String resendUrl;

    public MailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** True quando há API key configurada (Resend utilizável). */
    public boolean isConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    /**
     * Envia um e-mail. Retorna true se o Resend aceitou (2xx). Best-effort: nunca lança.
     */
    public boolean send(String toEmail, String subject, String html, String text) {
        return send(toEmail, subject, html, text, null);
    }

    /**
     * Envia um e-mail com um endereço opcional de "responder para" (reply-to) —
     * útil no formulário de contato, para a equipe responder direto ao visitante.
     */
    public boolean send(String toEmail, String subject, String html, String text, String replyTo) {
        if (!isConfigured()) {
            log.warn("Resend não configurado (RESEND_API_KEY ausente). E-mail '{}' não enviado para {}.",
                    subject, maskEmail(toEmail));
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("E-mail '{}' sem destinatário — envio ignorado.", subject);
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromAddress);
            body.put("to", List.of(toEmail));
            body.put("subject", subject);
            body.put("html", html);
            body.put("text", text);
            if (replyTo != null && !replyTo.isBlank()) {
                body.put("reply_to", replyTo);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    resendUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("email_enviado provider=resend to={} subject='{}'", maskEmail(toEmail), subject);
                return true;
            }
            log.warn("Resend retornou status {} ao enviar '{}' para {}",
                    response.getStatusCode(), subject, maskEmail(toEmail));
            return false;
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail '{}' via Resend para {}: {}",
                    subject, maskEmail(toEmail), ex.getMessage());
            return false;
        }
    }

    /**
     * Envia um e-mail usando um template publicado no Resend (referenciado por id/alias),
     * passando as variáveis do template. Não envia html/text (a API rejeita junto de template).
     * Best-effort: nunca lança.
     */
    public boolean sendTemplate(String toEmail, String templateId, String subject,
                                Map<String, Object> variables, String replyTo) {
        if (!isConfigured()) {
            log.warn("Resend não configurado (RESEND_API_KEY ausente). Template '{}' não enviado para {}.",
                    templateId, maskEmail(toEmail));
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Template '{}' sem destinatário — envio ignorado.", templateId);
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(resendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> template = new HashMap<>();
            template.put("id", templateId);
            template.put("variables", variables == null ? Map.of() : variables);

            Map<String, Object> body = new HashMap<>();
            // NÃO enviamos "from" aqui: o payload sobrescreveria o remetente configurado
            // no template do Resend. Deixamos cada template usar o seu próprio remetente.
            body.put("to", List.of(toEmail));
            body.put("template", template);
            if (subject != null && !subject.isBlank()) {
                body.put("subject", subject);
            }
            if (replyTo != null && !replyTo.isBlank()) {
                body.put("reply_to", replyTo);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    resendUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("email_enviado provider=resend template={} to={}", templateId, maskEmail(toEmail));
                return true;
            }
            log.warn("Resend retornou status {} ao enviar template '{}' para {}",
                    response.getStatusCode(), templateId, maskEmail(toEmail));
            return false;
        } catch (Exception ex) {
            log.warn("Falha ao enviar template '{}' via Resend para {}: {}",
                    templateId, maskEmail(toEmail), ex.getMessage());
            return false;
        }
    }

    /** Mascara o e-mail para não registrar PII completo nos logs (ex.: j***@dominio.com). */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "desconhecido";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
