package ads.uninassau.brjobs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Monta e envia o e-mail de "Esqueci minha senha" usando o template do Resend
 * ("password-reset"), passando {{first_name}} e {{reset_url}}.
 *
 * O reset_url leva APENAS um token forte e opaco (sem e-mail nem código na URL): o
 * frontend abre a tela de nova senha e envia o token ao backend, que o resolve para o
 * usuário. O transporte (Resend) fica em {@link MailService}.
 */
@Service
@Slf4j
public class PasswordResetEmailService {

    private final MailService mailService;

    @Value("${app.mail.resend.template.password-reset:password-reset}")
    private String templateId;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public PasswordResetEmailService(MailService mailService) {
        this.mailService = mailService;
    }

    public boolean sendResetLink(String toEmail, String nome, String rawToken) {
        Map<String, Object> variables = Map.of(
                "first_name", primeiroNome(nome, toEmail),
                "reset_url", montarResetUrl(rawToken)
        );
        return mailService.sendTemplate(
                toEmail,
                templateId,
                "BRJobs - Redefinição de senha",
                variables,
                null
        );
    }

    /**
     * Deep link para a tela de recuperação, contendo apenas o token opaco.
     * Ex.: https://app.brjobs.com.br/forgot-password?token=<aleatório>
     */
    private String montarResetUrl(String rawToken) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        String tokenEnc = URLEncoder.encode(rawToken == null ? "" : rawToken, StandardCharsets.UTF_8);
        return base + "/forgot-password?token=" + tokenEnc;
    }

    private String primeiroNome(String nome, String email) {
        if (nome != null && !nome.isBlank()) {
            return nome.trim().split("\\s+")[0];
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "usuário";
    }
}
