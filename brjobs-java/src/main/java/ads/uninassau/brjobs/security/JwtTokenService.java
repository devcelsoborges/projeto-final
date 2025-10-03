package ads.uninassau.brjobs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;

// Importações necessárias para o JJWT
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

@Service
public class JwtTokenService {

    // Chave secreta lida do arquivo de configuração (application.properties)
    @Value("${brjobs.jwt.secret}")
    private String secret;

    // Tempo de expiração do token em milissegundos (ex: 1 hora)
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
            // CORREÇÃO: Necessário chamar .build() no parser após setSigningKey
            Jwts.parser()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            // Log de Assinatura JWT inválida
        } catch (MalformedJwtException ex) {
            // Log de Token JWT inválido
        } catch (ExpiredJwtException ex) {
            // Log de Token JWT expirado
        } catch (UnsupportedJwtException ex) {
            // Log de Token JWT não suportado
        } catch (IllegalArgumentException ex) {
            // Log de Cadeia de claims JWT vazia
        }
        return false;
    }

    /**
     * Obtém o e-mail (subject) do usuário a partir do token.
     * @param token O token do qual extrair o e-mail.
     * @return O e-mail (string) do usuário.
     */
    public String getUsernameFromToken(String token) {
        // CORREÇÃO: Necessário chamar .build() no parser
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
}