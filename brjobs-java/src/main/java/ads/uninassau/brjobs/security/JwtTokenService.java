package ads.uninassau.brjobs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public class JwtTokenService {

    // Chave secreta lida do arquivo de configuração
    @Value("${brjobs.jwt.secret}")
    private String secret;

    // Tempo de expiração do token em milissegundos
    @Value("${brjobs.jwt.expiration}")
    private long expirationTime;

    /**
     * Cria uma SecretKey a partir da string secreta.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalStateException("BRJOBS_JWT_SECRET deve ter pelo menos 64 bytes para assinar JWT com HS512.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera um Token JWT para o usuário autenticado.
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Valida o Token JWT verificando se ele não está expirado ou malformado.
     * @param authToken O token a ser validado.
     * @return true se o token for válido, false caso contrário.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            System.out.println("DEBUG JwtTokenService: Token validado com sucesso");
            return true;
        } catch (SignatureException ex) {
            System.err.println("DEBUG JwtTokenService: Assinatura JWT inválida: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            System.err.println("DEBUG JwtTokenService: Token JWT malformado: " + ex.getMessage());
        } catch (ExpiredJwtException ex) {
            System.err.println("DEBUG JwtTokenService: Token JWT expirado: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            System.err.println("DEBUG JwtTokenService: Token JWT não suportado: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            System.err.println("DEBUG JwtTokenService: Cadeia de claims JWT vazia: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Obtém o e-mail (subject) do usuário a partir do token.
     * @param token O token do qual extrair o e-mail.
     * @return O e-mail (string) do usuário.
     * @throws Exception se o token for inválido ou expirado
     */
    public String getUsernameFromToken(String token) throws Exception {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            if (subject == null || subject.isEmpty()) {
                throw new Exception("Token JWT não contém subject (email) válido");
            }
            return subject;
        } catch (SignatureException ex) {
            System.err.println("Assinatura JWT inválida ao extrair username: " + ex.getMessage());
            throw new Exception("Assinatura JWT inválida", ex);
        } catch (MalformedJwtException ex) {
            System.err.println("Token JWT malformado ao extrair username: " + ex.getMessage());
            throw new Exception("Token JWT malformado", ex);
        } catch (ExpiredJwtException ex) {
            System.err.println("Token JWT expirado ao extrair username: " + ex.getMessage());
            throw new Exception("Token JWT expirado", ex);
        } catch (UnsupportedJwtException ex) {
            System.err.println("Token JWT não suportado ao extrair username: " + ex.getMessage());
            throw new Exception("Token JWT não suportado", ex);
        } catch (IllegalArgumentException ex) {
            System.err.println("Cadeia de claims JWT vazia ao extrair username: " + ex.getMessage());
            throw new Exception("Cadeia de claims JWT vazia", ex);
        }
    }
}
