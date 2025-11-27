package ads.uninassau.brjobs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException; // Importação corrigida para o JJWT moderno

@Service
public class JwtTokenService {

    // Chave secreta lida do arquivo de configuração
    @Value("${brjobs.jwt.secret}")
    private String secret;

    // Tempo de expiração do token em milissegundos
    @Value("${brjobs.jwt.expiration}")
    private long expirationTime;

    /**
     * Gera um Token JWT para o usuário autenticado.
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                // Usando a chave secreta e o algoritmo HS512.
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * Valida o Token JWT verificando se ele não está expirado ou malformado.
     * @param authToken O token a ser validado.
     * @return true se o token for válido, false caso contrário.
     */
    public boolean validateToken(String authToken) {
        try {
            // Parser construído com a chave secreta para validação
            Jwts.parser()
                    .setSigningKey(secret)
                    .build() // ESSENCIAL para a API moderna do JJWT
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            // Assinatura JWT inválida (chave secreta incorreta)
            System.err.println("Assinatura JWT inválida: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            // Token JWT inválido (não está no formato esperado)
            System.err.println("Token JWT malformado: " + ex.getMessage());
        } catch (ExpiredJwtException ex) {
            // Token JWT expirado
            System.err.println("Token JWT expirado: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            // Token JWT não suportado
            System.err.println("Token JWT não suportado: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // Cadeia de claims JWT vazia
            System.err.println("Cadeia de claims JWT vazia: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Obtém o e-mail (subject) do usuário a partir do token.
     * @param token O token do qual extrair o e-mail.
     * @return O e-mail (string) do usuário.
     */
    public String getUsernameFromToken(String token) {
        // Parser construído com a chave secreta para extrair as claims
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .build() // ESSENCIAL para a API moderna do JJWT
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
}