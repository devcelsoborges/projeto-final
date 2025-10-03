package ads.uninassau.brjobs.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor; // Usa RequiredArgsConstructor para campos 'final'

@Data
@RequiredArgsConstructor // Gera construtor com campos final/NonNull
public class LoginResponseDTO {

    private final String token;

    // Obs: Se 'token' não fosse final, usaríamos @AllArgsConstructor ou @NoArgsConstructor + Setter
}