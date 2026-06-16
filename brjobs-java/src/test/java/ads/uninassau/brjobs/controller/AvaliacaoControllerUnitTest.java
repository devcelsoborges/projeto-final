package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.service.AvaliacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvaliacaoController Unit Tests")
class AvaliacaoControllerUnitTest {

    @Mock
    private AvaliacaoService avaliacaoService;

    @InjectMocks
    private AvaliacaoController avaliacaoController;

    @Test
    @DisplayName("Deve instanciar AvaliacaoController com sucesso")
    void testAvaliacaoControllerInstantiation() {
        assertNotNull(avaliacaoController);
    }
}
