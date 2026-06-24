package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import ads.uninassau.brjobs.security.JwtTokenService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração do endpoint público de foto de perfil.
 * Usa H2 em memória (modo PostgreSQL) e MockMvc com a cadeia de segurança real,
 * sem enviar credenciais — valida também que a rota GET é pública.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fototestdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@DisplayName("GET /api/usuarios/{id}/foto - Testes de Integração")
class UsuarioFotoPerfilIntegrationTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 4, 5, 6};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Usuario salvarUsuario(String email, byte[] foto) {
        Usuario usuario = Usuario.builder()
                .nome("Usuário Teste")
                .email(email)
                .senha("senha-criptografada")
                .tipoUsuario(TipoUsuario.CONTRATANTE)
                .ativo(true)
                .fotoPerfil(foto)
                .build();
        return usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Deve retornar a foto PNG com Content-Type image/png sem autenticação")
    void deveRetornarFotoPng() throws Exception {
        Usuario usuario = salvarUsuario("foto-png@test.com", PNG_BYTES);

        mockMvc.perform(get("/api/usuarios/{id}/foto", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    @DisplayName("Deve retornar a foto JPEG com Content-Type image/jpeg")
    void deveRetornarFotoJpeg() throws Exception {
        Usuario usuario = salvarUsuario("foto-jpeg@test.com", JPEG_BYTES);

        mockMvc.perform(get("/api/usuarios/{id}/foto", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG_BYTES));
    }

    @Test
    @DisplayName("Deve retornar 404 quando o usuário não possui foto")
    void deveRetornar404QuandoNaoHaFoto() throws Exception {
        Usuario usuario = salvarUsuario("sem-foto@test.com", null);

        mockMvc.perform(get("/api/usuarios/{id}/foto", usuario.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 404 quando o usuário não existe")
    void deveRetornar404QuandoUsuarioNaoExiste() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}/foto", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Foto armazenada em WebP deve ser servida com Content-Type image/webp")
    void deveServirFotoWebp() throws Exception {
        byte[] webpBytes = {'R', 'I', 'F', 'F', 20, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ', 1, 2, 3};
        Usuario usuario = salvarUsuario("foto-webp@test.com", webpBytes);

        mockMvc.perform(get("/api/usuarios/{id}/foto", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(webpBytes));
    }

    @Test
    @DisplayName("Upload de PNG deve ser convertido para WebP e servido como image/webp")
    void deveConverterUploadParaWebpFimAFim() throws Exception {
        Usuario usuario = salvarUsuario("upload-webp@test.com", null);
        String token = jwtTokenService.generateToken(usuario.getEmail());

        BufferedImage imagem = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = imagem.createGraphics();
        for (int x = 0; x < 120; x++) {
            graphics.setColor(new Color(x * 2, 100, 255 - x * 2));
            graphics.fillRect(x, 0, 1, 80);
        }
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", png);

        MockMultipartFile foto = new MockMultipartFile("foto", "perfil.png", "image/png", png.toByteArray());

        mockMvc.perform(multipart("/api/usuarios/{id}/foto", usuario.getId())
                        .file(foto)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        byte[] corpo = mockMvc.perform(get("/api/usuarios/{id}/foto", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(corpo.length >= 12
                        && corpo[0] == 'R' && corpo[1] == 'I' && corpo[2] == 'F' && corpo[3] == 'F'
                        && corpo[8] == 'W' && corpo[9] == 'E' && corpo[10] == 'B' && corpo[11] == 'P',
                "Bytes servidos deveriam estar no formato WebP");
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} deve expor fotoPerfilUrl apenas quando há foto")
    void deveExporFotoPerfilUrlNoDTO() throws Exception {
        Usuario comFoto = salvarUsuario("dto-com-foto@test.com", PNG_BYTES);
        Usuario semFoto = salvarUsuario("dto-sem-foto@test.com", null);

        mockMvc.perform(get("/api/usuarios/{id}", comFoto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoPerfilUrl").value("/api/usuarios/" + comFoto.getId() + "/foto"));

        mockMvc.perform(get("/api/usuarios/{id}", semFoto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoPerfilUrl").value(nullValue()));
    }
}
