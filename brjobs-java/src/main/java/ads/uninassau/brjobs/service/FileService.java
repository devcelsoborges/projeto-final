package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.exception.InvalidFileUploadException;
import com.luciad.imageio.webp.WebPWriteParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Serviço responsável por validar e processar uploads de arquivos.
 */
@Slf4j
@Service
public class FileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList("application/pdf");

    // Qualidade da compressão lossy ao converter para WebP (0.0 a 1.0)
    private static final float WEBP_QUALITY = 0.8f;
    // Limite de pixels para decodificação segura (evita estouro de memória com "image bombs")
    private static final long MAX_PIXELS = 25_000_000L; // ~25 MP

    /**
     * Valida o arquivo de imagem (foto de perfil) e converte o conteúdo para WebP,
     * reduzindo o espaço ocupado no banco. Se a conversão não for possível,
     * os bytes originais são armazenados.
     *
     * @param file arquivo a ser processado
     * @return bytes da imagem em WebP (ou originais, se a conversão falhar)
     * @throws InvalidFileUploadException se o arquivo for inválido
     */
    public byte[] processarFotoPerfil(MultipartFile file) {
        byte[] bytes = validarEConverterArquivo(file, ALLOWED_IMAGE_TYPES, "Foto de perfil");
        return converterParaWebp(bytes);
    }

    /**
     * Valida e converte arquivo de documento (currículo) em bytes
     *
     * @param file arquivo a ser processado
     * @return array de bytes do arquivo
     * @throws InvalidFileUploadException se o arquivo for inválido
     */
    public byte[] processarCurriculo(MultipartFile file) {
        return validarEConverterArquivo(file, ALLOWED_DOCUMENT_TYPES, "Currículo");
    }

    /**
     * Detecta o tipo MIME de uma imagem a partir dos magic bytes.
     * Suporta os tipos aceitos no upload (WebP, PNG e JPEG); assume JPEG como padrão.
     *
     * @param bytes conteúdo da imagem
     * @return tipo MIME detectado ("image/webp", "image/png" ou "image/jpeg")
     */
    public String detectarTipoImagem(byte[] bytes) {
        if (isWebp(bytes)) {
            return "image/webp";
        }
        if (bytes != null && bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        return "image/jpeg";
    }

    /**
     * Converte uma imagem (PNG/JPEG) para WebP lossy.
     * A conversão é oportunista: se a imagem já estiver em WebP, exceder o limite
     * seguro de pixels, não puder ser decodificada ou o resultado ficar maior que
     * o original, os bytes originais são retornados — o upload nunca é bloqueado
     * por falha de conversão.
     *
     * @param original bytes da imagem original
     * @return bytes em WebP, ou os originais quando a conversão não compensa/falha
     */
    byte[] converterParaWebp(byte[] original) {
        if (isWebp(original)) {
            return original;
        }
        if (!dimensoesSeguras(original)) {
            log.warn("Conversão para WebP ignorada: dimensões da imagem indisponíveis ou acima de {} pixels", MAX_PIXELS);
            return original;
        }

        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(original));
            if (imagem == null) {
                log.warn("Conversão para WebP ignorada: formato de imagem não suportado pelo decodificador");
                return original;
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
            if (!writers.hasNext()) {
                log.warn("Conversão para WebP indisponível: nenhum encoder WebP registrado neste ambiente");
                return original;
            }

            ImageWriter writer = writers.next();
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(saida)) {
                WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType("Lossy");
                writeParam.setCompressionQuality(WEBP_QUALITY);

                writer.setOutput(imageOutput);
                writer.write(null, new IIOImage(normalizarImagem(imagem), null, null), writeParam);
            } finally {
                writer.dispose();
            }

            byte[] webp = saida.toByteArray();
            if (webp.length == 0 || webp.length >= original.length) {
                return original;
            }
            return webp;
        } catch (IOException | RuntimeException | UnsatisfiedLinkError e) {
            log.warn("Falha ao converter imagem para WebP; armazenando original: {}", e.toString());
            return original;
        }
    }

    /**
     * Verifica se os bytes correspondem a um arquivo WebP (container RIFF com tag WEBP)
     */
    private boolean isWebp(byte[] bytes) {
        return bytes != null && bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    /**
     * Lê apenas o cabeçalho da imagem para validar as dimensões antes de
     * decodificar o conteúdo completo em memória
     */
    private boolean dimensoesSeguras(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                return pixels > 0 && pixels <= MAX_PIXELS;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Normaliza a imagem para RGB/ARGB, formatos esperados pelo encoder WebP
     * (cobre paletas indexadas, tons de cinza e outros tipos de BufferedImage)
     */
    private BufferedImage normalizarImagem(BufferedImage imagem) {
        boolean temAlpha = imagem.getColorModel().hasAlpha();
        int tipoAlvo = temAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (imagem.getType() == tipoAlvo) {
            return imagem;
        }
        BufferedImage convertida = new BufferedImage(imagem.getWidth(), imagem.getHeight(), tipoAlvo);
        Graphics2D graphics = convertida.createGraphics();
        try {
            graphics.drawImage(imagem, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return convertida;
    }

    /**
     * Valida o tamanho do arquivo
     */
    private void validarTamanhoPerfil(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileUploadException(
                "O arquivo excede o tamanho máximo permitido de 5 MB. Tamanho atual: " +
                formatarTamanho(file.getSize())
            );
        }
    }

    /**
     * Valida o tipo MIME do arquivo
     */
    private void validarTipoArquivo(MultipartFile file, List<String> tiposPermitidos) {
        String contentType = file.getContentType();
        if (contentType == null || !tiposPermitidos.contains(contentType)) {
            throw new InvalidFileUploadException(
                "Tipo de arquivo não permitido. Tipos aceitos: " + String.join(", ", tiposPermitidos)
            );
        }
    }

    /**
     * Método genérico para validar e converter arquivo em bytes
     */
    private byte[] validarEConverterArquivo(MultipartFile file, List<String> tiposPermitidos, String nomeArquivo) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException(nomeArquivo + " não pode estar vazio.");
        }

        validarTamanhoPerfil(file);
        validarTipoArquivo(file, tiposPermitidos);

        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileUploadException("Erro ao processar o arquivo: " + nomeArquivo, e);
        }
    }

    /**
     * Formata o tamanho do arquivo em formato legível
     */
    private String formatarTamanho(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] unidades = new String[]{"B", "KB", "MB", "GB"};
        int digitosGrupo = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitosGrupo), unidades[digitosGrupo]);
    }
}
