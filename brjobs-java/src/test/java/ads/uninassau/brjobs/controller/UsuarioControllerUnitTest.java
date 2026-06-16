package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController Unit Tests")
class UsuarioControllerUnitTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    @Test
    @DisplayName("Deve instanciar UsuarioController com sucesso")
    void testUsuarioControllerInstantiation() {
        assertNotNull(usuarioController);
    }
}
