package ads.uninassau.brjobs.controller;

import ads.uninassau.brjobs.dto.LoginRequestDTO;
import ads.uninassau.brjobs.dto.LoginResponseDTO;
import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsável por operações de autenticação.
 * NÃO é responsável pelo registro de usuários (usar UsuarioController).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Endpoint para login e obtenção do token JWT.
     * POST /api/auth/login
     *
     * @param loginRequest contém email e senha
     * @return 200 OK com token JWT ou 401 UNAUTHORIZED
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            // Autenticar e gerar token
            String token = authService.authenticateAndGetToken(loginRequest);
            return ResponseEntity.ok(new LoginResponseDTO(token));

        } catch (AuthenticationException e) {
            // Credenciais inválidas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO(null, "Email ou senha inválidos."));
        }
    }

    /**
     * Endpoint para obter dados do usuário autenticado.
     * GET /api/auth/me
     * Requer token JWT no header Authorization
     *
     * @return 200 OK com dados do usuário
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            UsuarioDTO usuario = authService.obterUsuarioAutenticado(email);
            return ResponseEntity.ok(usuario);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Endpoint para validar se o token JWT é válido.
     * GET /api/auth/validate
     *
     * @return 200 OK se válido, 401 UNAUTHORIZED se inválido
     */
    @GetMapping("/validate")
    public ResponseEntity<Void> validarToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Endpoint para logout (opcional, principalmente para frontend)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}