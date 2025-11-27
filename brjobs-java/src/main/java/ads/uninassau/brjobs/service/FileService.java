package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.exception.InvalidFileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Serviço responsável por validar e processar uploads de arquivos.
 */
@Service
public class FileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList("application/pdf");

    /**
     * Valida e converte arquivo de imagem (foto de perfil) em bytes
     *
     * @param file arquivo a ser processado
     * @return array de bytes do arquivo
     * @throws InvalidFileUploadException se o arquivo for inválido
     */
    public byte[] processarFotoPerfil(MultipartFile file) {
        return validarEConverterArquivo(file, ALLOWED_IMAGE_TYPES, "Foto de perfil");
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

