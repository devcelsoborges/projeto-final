package ads.uninassau.brjobs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordResetEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public PasswordResetEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean sendResetCode(String toEmail, String resetCode, int expiresMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender indisponível. Código de recuperação não enviado por e-mail para {}.", toEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("BRJobs - Recuperação de Senha");
            message.setText(
                    "Recebemos uma solicitação para redefinir sua senha.\n\n"
                            + "Seu código de recuperação é: " + resetCode + "\n"
                            + "Este código expira em " + expiresMinutes + " minutos.\n\n"
                            + "Se você não solicitou essa recuperação, ignore este e-mail."
            );

            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail de recuperação para {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }
}
