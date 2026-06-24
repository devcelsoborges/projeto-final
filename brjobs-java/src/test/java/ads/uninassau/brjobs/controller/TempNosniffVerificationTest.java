package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nosnifftestdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class TempNosniffVerificationTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void fotoEndpointSendsNosniffHeader() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Nosniff Teste")
                .email("nosniff@test.com")
                .senha("senha-criptografada")
                .tipoUsuario(TipoUsuario.CONTRATANTE)
                .ativo(true)
                .fotoPerfil(PNG_BYTES)
                .build());

        mockMvc.perform(get("/api/usuarios/{id}/foto", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
