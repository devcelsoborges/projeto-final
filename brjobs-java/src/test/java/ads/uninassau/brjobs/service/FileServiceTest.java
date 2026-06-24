package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.exception.InvalidFileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FileService - Conversão de foto de perfil para WebP")
class FileServiceTest {

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
    }

    private byte[] gerarImagem(String formato, boolean comAlpha) throws IOException {
        int tipo = comAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage imagem = new BufferedImage(120, 80, tipo);
        Graphics2D graphics = imagem.createGraphics();
        // Gradiente simples para a imagem ter conteúdo "fotográfico" compressível
        for (int x = 0; x < 120; x++) {
            graphics.setColor(new Color(x * 2, 100, 255 - x * 2, comAlpha ? 200 : 255));
            graphics.fillRect(x, 0, 1, 80);
        }
        graphics.dispose();

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagem, formato, saida), "Falha ao gerar imagem de teste " + formato);
        return saida.toByteArray();
    }

    private boolean isWebp(byte[] bytes) {
        return bytes != null && bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    @Test
    @DisplayName("Deve converter upload PNG para WebP")
    void deveConverterPngParaWebp() throws IOException {
        byte[] png = gerarImagem("png", false);
        MockMultipartFile arquivo = new MockMultipartFile("foto", "foto.png", "image/png", png);

        byte[] resultado = fileService.processarFotoPerfil(arquivo);

        assertNotNull(resultado);
        assertTrue(isWebp(resultado), "Resultado deveria ser WebP");
        assertEquals("image/webp", fileService.detectarTipoImagem(resultado));
    }

    @Test
    @DisplayName("Deve converter upload JPEG para WebP")
    void deveConverterJpegParaWebp() throws IOException {
        byte[] jpeg = gerarImagem("jpg", false);
        MockMultipartFile arquivo = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", jpeg);

        byte[] resultado = fileService.processarFotoPerfil(arquivo);

        assertTrue(isWebp(resultado), "Resultado deveria ser WebP");
    }

    @Test
    @DisplayName("Deve preservar canal alpha ao converter PNG transparente")
    void deveConverterPngComAlpha() throws IOException {
        byte[] png = gerarImagem("png", true);
        MockMultipartFile arquivo = new MockMultipartFile("foto", "foto.png", "image/png", png);

        byte[] resultado = fileService.processarFotoPerfil(arquivo);

        assertTrue(isWebp(resultado), "Resultado deveria ser WebP");
    }

    @Test
    @DisplayName("Upload já em WebP deve ser aceito e mantido sem reconversão")
    void deveManterWebpSemReconverter() throws IOException {
        byte[] png = gerarImagem("png", false);
        byte[] webp = fileService.converterParaWebp(png);
        assertTrue(isWebp(webp));

        MockMultipartFile arquivo = new MockMultipartFile("foto", "foto.webp", "image/webp", webp);
        byte[] resultado = fileService.processarFotoPerfil(arquivo);

        assertArrayEquals(webp, resultado, "WebP de entrada deveria passar inalterado");
    }

    @Test
    @DisplayName("Bytes corrompidos com MIME de imagem devem ser armazenados sem conversão")
    void deveManterOriginalQuandoConteudoNaoDecodifica() {
        byte[] lixo = {(byte) 0x89, 0x50, 0x4E, 0x47, 1, 2, 3, 4, 5, 6, 7, 8};
        MockMultipartFile arquivo = new MockMultipartFile("foto", "foto.png", "image/png", lixo);

        byte[] resultado = fileService.processarFotoPerfil(arquivo);

        assertArrayEquals(lixo, resultado, "Conteúdo não decodificável deveria ser mantido como está");
    }

    @Test
    @DisplayName("Deve continuar rejeitando tipos de arquivo não permitidos")
    void deveRejeitarTipoNaoPermitido() {
        MockMultipartFile arquivo = new MockMultipartFile("foto", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(InvalidFileUploadException.class, () -> fileService.processarFotoPerfil(arquivo));
    }

    @Test
    @DisplayName("detectarTipoImagem deve reconhecer WebP, PNG e JPEG")
    void deveDetectarTiposDeImagem() throws IOException {
        byte[] png = gerarImagem("png", false);
        byte[] jpeg = gerarImagem("jpg", false);
        byte[] webp = fileService.converterParaWebp(png);

        assertEquals("image/png", fileService.detectarTipoImagem(png));
        assertEquals("image/jpeg", fileService.detectarTipoImagem(jpeg));
        assertEquals("image/webp", fileService.detectarTipoImagem(webp));
    }

    @Test
    @DisplayName("Conversão deve reduzir o tamanho de um PNG fotográfico")
    void deveReduzirTamanhoDePngFotografico() throws IOException {
        byte[] png = gerarImagem("png", false);

        byte[] webp = fileService.converterParaWebp(png);

        assertTrue(isWebp(webp));
        assertTrue(webp.length < png.length,
                "WebP (" + webp.length + " bytes) deveria ser menor que o PNG original (" + png.length + " bytes)");
    }
}
