package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.LoginResponseDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Endpoint para login e obtenção do token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {

        // 1. Tenta autenticar o usuário usando o AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getSenha()
                )
        );

        // 2. Define o usuário autenticado no contexto de segurança
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Gera o token JWT para retorno
        String token = authService.generateToken(loginRequest.getEmail());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    /**
     * Endpoint para registrar um novo usuário.
     */
    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@RequestBody UsuarioDTO usuarioDTO) {

        // Esta linha era a linha 16 (ou próxima) que causava o erro.
        // Agora ela usa o UsuarioDTO, que existe.
        UsuarioDTO novoUsuario = authService.registerUser(usuarioDTO);

        return new ResponseEntity<>(novoUsuario, HttpStatus.CREATED);
    }
}