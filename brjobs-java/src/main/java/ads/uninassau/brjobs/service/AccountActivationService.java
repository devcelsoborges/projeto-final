package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import ads.uninassau.brjobs.validator.UsuarioValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Confirmação de cadastro por e-mail (template "account-activation" do Resend).
 *
 * No cadastro local, gera um token (válido 24h) e envia um e-mail com {{confirm_url}}.
 * Ao clicar, a conta é marcada como confirmada. NÃO bloqueia o login — é apenas um
 * registro de confirmação (a base para, no futuro, exigir confirmação se desejado).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountActivationService {

    private static final int TOKEN_TTL_HORAS = 24;

    private final UsuarioRepository usuarioRepository;
    private final MailService mailService;

    @Value("${app.mail.resend.template.account-activation:account-activation}")
    private String templateId;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Gera o token de confirmação, persiste no usuário e envia o e-mail de confirmação.
     * Best-effort: falha de envio não interrompe o cadastro (apenas loga).
     */
    @Transactional
    public void enviarConfirmacao(Usuario usuario) {
        if (usuario == null || usuario.getEmail() == null) {
            return;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        usuario.setEmailConfirmado(false);
        usuario.setEmailConfirmationToken(token);
        usuario.setEmailConfirmationExpiresAt(LocalDateTime.now().plusHours(TOKEN_TTL_HORAS));
        usuarioRepository.save(usuario);

        Map<String, Object> variables = Map.of(
                "first_name", primeiroNome(usuario.getNome(), usuario.getEmail()),
                "confirm_url", montarConfirmUrl(token)
        );

        // Best-effort: o token já está persistido; uma falha de envio não pode derrubar o
        // cadastro (o usuário pode reenviar depois). Por isso nunca propagamos exceção daqui.
        boolean enviado = false;
        try {
            enviado = mailService.sendTemplate(
                    usuario.getEmail(),
                    templateId,
                    "Confirme seu cadastro — BRJobs",
                    variables,
                    null
            );
        } catch (Exception ex) {
            log.warn("account_activation_email_falhou userId={} motivo={}", usuario.getId(), ex.getMessage());
        }
        if (!enviado) {
            log.warn("account_activation_email_nao_enviado userId={} (verifique RESEND_API_KEY/RESEND_FROM)", usuario.getId());
        } else {
            log.info("account_activation_email userId={} enviado=true", usuario.getId());
        }
    }

    /**
     * Reenvia o e-mail de confirmação para um e-mail, se existir uma conta ainda não
     * confirmada. Não revela se o e-mail existe (anti-enumeração).
     */
    @Transactional
    public void reenviarPorEmail(String rawEmail) {
        String email = UsuarioValidator.normalizarEmail(rawEmail);
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> !Boolean.TRUE.equals(u.getEmailConfirmado()))
                .ifPresent(this::enviarConfirmacao);
    }

    /**
     * Confirma o cadastro a partir do token do link. Idempotente o suficiente: token
     * inválido/expirado retorna false; token válido marca confirmado e limpa o token.
     */
    @Transactional
    public boolean confirmar(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Usuario usuario = usuarioRepository.findByEmailConfirmationToken(token).orElse(null);
        if (usuario == null) {
            return false;
        }
        if (usuario.getEmailConfirmationExpiresAt() == null
                || LocalDateTime.now().isAfter(usuario.getEmailConfirmationExpiresAt())) {
            log.info("account_activation_token_expirado userId={}", usuario.getId());
            return false;
        }

        usuario.setEmailConfirmado(true);
        usuario.setEmailConfirmationToken(null);
        usuario.setEmailConfirmationExpiresAt(null);
        usuarioRepository.save(usuario);
        log.info("account_activation_confirmado userId={}", usuario.getId());
        return true;
    }

    private String montarConfirmUrl(String token) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        String tokenEnc = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return base + "/confirmar-email?token=" + tokenEnc;
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
