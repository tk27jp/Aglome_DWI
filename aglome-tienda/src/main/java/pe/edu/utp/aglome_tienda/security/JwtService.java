package pe.edu.utp.aglome_tienda.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Clave secreta para firmar el token. En un proyecto real iría en
    // application.properties o una variable de entorno, no fija en el código.
    private static final String SECRET = "AglomeTiendaClaveSecretaParaJWT2026CambiarEnProduccion";
    private static final long EXPIRACION_MS = 1000 * 60 * 60 * 10; // 10 horas

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Genera un token nuevo para un usuario, guardando su email y su rol adentro
    public String generarToken(String email, String rol) {
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRACION_MS))
                .signWith(key)
                .compact();
    }

    // Extrae el email (subject) del token
    public String extraerEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Extrae el rol guardado dentro del token
    public String extraerRol(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("rol", String.class);
    }

    // Valida que el token no esté vencido y esté bien firmado
    public boolean esTokenValido(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}