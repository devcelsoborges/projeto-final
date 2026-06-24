package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.model.ChatMessage;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatNotificationService - job de notificação por template")
class ChatNotificationServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private ChatNotificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "graceMinutes", 2);
        ReflectionTestUtils.setField(service, "templateId", "new-message-notification");
        ReflectionTestUtils.setField(service, "chatUrl", "http://localhost:4200/chat");
        ReflectionTestUtils.setField(service, "listingFallback", "sua conversa no BRJobs");
    }

    private ChatMessage msg(Usuario de, Usuario para, String texto, LocalDateTime quando) {
        ChatMessage m = ChatMessage.builder().remetente(de).destinatario(para)
                .conteudo(texto).lido(false).notificado(false).build();
        m.setCreatedAt(quando);
        return m;
    }

    @Test
    @DisplayName("Sem Resend configurado: não consulta nem consome mensagens")
    void semResendConfiguradoNaoFazNada() {
        when(mailService.isConfigured()).thenReturn(false);

        service.notificarMensagensPendentes();

        verify(chatMessageRepository, never()).findByNotificadoFalseAndLidoFalseAndCreatedAtBefore(any());
        verify(mailService, never()).sendTemplate(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Desabilitado por config: nem toca no MailService")
    void desabilitadoNaoFazNada() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.notificarMensagensPendentes();

        verifyNoInteractions(mailService);
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    @DisplayName("Agrupa por conversa: 1 e-mail por par (destinatário, remetente) e marca todas notificadas")
    void agrupaPorConversaEnviaTemplateEMarcaNotificado() {
        when(mailService.isConfigured()).thenReturn(true);
        when(mailService.sendTemplate(anyString(), anyString(), anyString(), any(), isNull())).thenReturn(true);

        Usuario joao = Usuario.builder().id(1L).nome("Joao Silva").email("joao@example.com").build();
        Usuario maria = Usuario.builder().id(2L).nome("Maria").email("maria@example.com").build();
        Usuario ana = Usuario.builder().id(3L).nome("Ana").email("ana@example.com").build();

        // Ana recebeu de Joao (2 msgs) e de Maria (1) -> 2 conversas. Joao recebeu de Ana (1) -> 1 conversa.
        ChatMessage m1 = msg(joao, ana, "oi", LocalDateTime.of(2026, 6, 1, 10, 0));
        ChatMessage m2 = msg(joao, ana, "tudo bem?", LocalDateTime.of(2026, 6, 1, 10, 5));
        ChatMessage m3 = msg(maria, ana, "ola Ana", LocalDateTime.of(2026, 6, 1, 9, 0));
        ChatMessage m4 = msg(ana, joao, "responde ai", LocalDateTime.of(2026, 6, 1, 8, 0));
        when(chatMessageRepository.findByNotificadoFalseAndLidoFalseAndCreatedAtBefore(any()))
                .thenReturn(List.of(m1, m2, m3, m4));

        service.notificarMensagensPendentes();

        // 3 conversas distintas -> 3 e-mails
        verify(mailService, times(3)).sendTemplate(anyString(), anyString(), anyString(), any(), isNull());
        // todas marcadas como notificadas
        assertTrue(m1.getNotificado() && m2.getNotificado() && m3.getNotificado() && m4.getNotificado());
    }

    @Test
    @DisplayName("Usa a mensagem mais recente da conversa e trunca/escapa o preview")
    void usaMensagemMaisRecenteETruncaEscapaPreview() {
        when(mailService.isConfigured()).thenReturn(true);
        when(mailService.sendTemplate(anyString(), anyString(), anyString(), any(), isNull())).thenReturn(true);

        Usuario joao = Usuario.builder().id(1L).nome("Joao Silva").email("joao@example.com").build();
        Usuario ana = Usuario.builder().id(3L).nome("Ana Souza").email("ana@example.com").build();

        String longa = "<b>" + "a".repeat(300) + "</b>";
        ChatMessage antiga = msg(joao, ana, "mensagem antiga", LocalDateTime.of(2026, 6, 1, 9, 0));
        ChatMessage recente = msg(joao, ana, longa, LocalDateTime.of(2026, 6, 1, 10, 0));
        when(chatMessageRepository.findByNotificadoFalseAndLidoFalseAndCreatedAtBefore(any()))
                .thenReturn(List.of(antiga, recente));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        service.notificarMensagensPendentes();

        verify(mailService).sendTemplate(eq("ana@example.com"), eq("new-message-notification"),
                anyString(), varsCaptor.capture(), isNull());
        Map<String, Object> vars = varsCaptor.getValue();

        assertEquals("Ana", vars.get("first_name"), "primeiro nome do destinatário");
        assertEquals("Joao Silva", vars.get("sender_name"));
        assertEquals("sua conversa no BRJobs", vars.get("listing_title"));
        String preview = (String) vars.get("message_preview");
        assertFalse(preview.contains("<b>"), "preview deve escapar HTML");
        assertTrue(preview.contains("&lt;b&gt;"), "tag deve virar entidade escapada");
        assertTrue(preview.endsWith("…"), "preview longo deve ser truncado com reticências");
        assertTrue(preview.length() <= 180, "preview não pode estourar o limite");
    }
}
