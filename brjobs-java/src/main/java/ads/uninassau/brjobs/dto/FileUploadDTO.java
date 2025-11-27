package ads.uninassau.brjobs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para upload de arquivos do usuário (foto de perfil e currículo)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDTO {

    private MultipartFile fotoPerfil;
    private MultipartFile curriculo;
}

