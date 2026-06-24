package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.ContatoRequestDTO;
import ads.uninassau.brjobs.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint público do formulário "Entre em Contato".
 * Encaminha a mensagem por e-mail (via Resend) para a caixa de atendimento, com
 * reply-to apontando para o visitante — assim a equipe responde direto a ele.
 */
@RestController
@RequestMapping("/api/contato")
@RequiredArgsConstructor
@Slf4j
public class ContatoController {

    private final MailService mailService;

    @Value("${app.mail.contact-to:atendimento@brjobs.com.br}")
    private String contatoDestino;

    @PostMapping
    public ResponseEntity<?> enviar(@Valid @RequestBody ContatoRequestDTO req) {
        boolean enviado = mailService.send(
                contatoDestino,
                "BRJobs - Contato: " + req.getAssunto(),
                montarHtml(req),
                montarTexto(req),
                req.getEmail() // reply-to: responder vai direto para o visitante
        );

        if (!enviado) {
            log.warn("contato_email_falhou de={} assunto={}", mailService.maskEmail(req.getEmail()), req.getAssunto());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Não foi possível enviar sua mensagem agora. Tente novamente em instantes."));
        }

        log.info("contato_email_enviado de={} assunto={}", mailService.maskEmail(req.getEmail()), req.getAssunto());
        return ResponseEntity.ok(Map.of("message", "Mensagem enviada com sucesso."));
    }

    private String montarTexto(ContatoRequestDTO req) {
        return "Nova mensagem pelo formulario de contato do BRJobs.\n\n"
                + "Nome: " + req.getNome() + "\n"
                + "E-mail: " + req.getEmail() + "\n"
                + "Assunto: " + req.getAssunto() + "\n\n"
                + "Mensagem:\n" + req.getMensagem();
    }

    private String montarHtml(ContatoRequestDTO req) {
        // Escapa a entrada do visitante para evitar injeção de HTML no corpo do e-mail.
        String nome = escape(req.getNome());
        String email = escape(req.getEmail());
        String assunto = escape(req.getAssunto());
        String mensagem = escape(req.getMensagem()).replace("\n", "<br/>");

        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937\">"
                + "<h2 style=\"margin:0 0 12px\">Nova mensagem de contato</h2>"
                + "<p><strong>Nome:</strong> " + nome + "</p>"
                + "<p><strong>E-mail:</strong> " + email + "</p>"
                + "<p><strong>Assunto:</strong> " + assunto + "</p>"
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:16px 0\"/>"
                + "<p style=\"white-space:pre-wrap\">" + mensagem + "</p>"
                + "</div>";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
