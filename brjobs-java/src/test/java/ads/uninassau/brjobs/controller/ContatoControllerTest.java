package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.ContatoRequestDTO;
import ads.uninassau.brjobs.service.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContatoController - envio do formulário de contato")
class ContatoControllerTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private ContatoController controller;

    private ContatoRequestDTO req() {
        return new ContatoRequestDTO("Joao da Silva", "joao@example.com",
                "Duvida sobre o serviço", "Gostaria de saber mais sobre como publicar.");
    }

    @Test
    @DisplayName("Envia para a caixa de atendimento com reply-to do visitante e retorna 200")
    void enviaComSucessoRetorna200() {
        ReflectionTestUtils.setField(controller, "contatoDestino", "atendimento@brjobs.com.br");
        when(mailService.send(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        ResponseEntity<?> resp = controller.enviar(req());

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        // destino = caixa de atendimento; reply-to = e-mail do visitante
        verify(mailService).send(eq("atendimento@brjobs.com.br"), anyString(), anyString(), anyString(), eq("joao@example.com"));
    }

    @Test
    @DisplayName("Falha de envio retorna 502")
    void falhaDeEnvioRetorna502() {
        ReflectionTestUtils.setField(controller, "contatoDestino", "atendimento@brjobs.com.br");
        when(mailService.send(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        ResponseEntity<?> resp = controller.enviar(req());

        assertEquals(HttpStatus.BAD_GATEWAY, resp.getStatusCode());
    }
}
