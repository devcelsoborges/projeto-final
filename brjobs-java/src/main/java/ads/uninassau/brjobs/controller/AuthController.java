package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.LoginResponseDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
    String token = authService.authenticateAndGetToken(loginRequest.getEmail(), loginRequest.getSenha());
    return ResponseEntity.ok(new LoginResponseDTO(token));
    }


    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@RequestBody UsuarioDTO usuarioDTO) {
    UsuarioDTO novoUsuario = authService.registerUser(usuarioDTO);
    return new ResponseEntity<>(novoUsuario, HttpStatus.CREATED);
    }

}