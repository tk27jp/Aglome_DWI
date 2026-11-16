package pe.edu.utp.aglome_tienda.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "ClaveSecretaSoloParaTestsDeAglomeTienda2026";
    private static final String OTRO_SECRETO = "OtraClaveDistintaParaProbarFirmasFalsas2026!!";

    private final JwtService jwtService = new JwtService(SECRETO, 3_600_000);

    @Test
    void generarToken_guardaElEmailYElRolDentroDelToken() {
        String token = jwtService.generarToken("jhonatan@test.com", "CLIENTE");

        assertEquals("jhonatan@test.com", jwtService.extraerEmail(token));
        assertEquals("CLIENTE", jwtService.extraerRol(token));
        assertTrue(jwtService.esTokenValido(token));
    }

    @Test
    void generarToken_conRolVendedor_loConservaTalCual() {
        String token = jwtService.generarToken("elton@aglome.com", "VENDEDOR");

        assertEquals("VENDEDOR", jwtService.extraerRol(token));
    }

    @Test
    void tokenVencido_noEsValidoYNoSePuedeLeer() {
        JwtService yaVencido = new JwtService(SECRETO, -1_000);
        String token = yaVencido.generarToken("jhonatan@test.com", "CLIENTE");

        assertFalse(jwtService.esTokenValido(token));
        assertThrows(Exception.class, () -> jwtService.extraerEmail(token));
    }

    @Test
    void tokenFirmadoConOtraClave_noEsValido() {
        String falso = new JwtService(OTRO_SECRETO, 3_600_000).generarToken("admin@aglome.com", "ADMIN");

        assertFalse(jwtService.esTokenValido(falso));
    }

    @Test
    void tokenAlteradoDespuesDeFirmado_noEsValido() {
        String token = jwtService.generarToken("jhonatan@test.com", "CLIENTE");
        String[] partes = token.split("\\.");
        String cuerpoAlterado = (partes[1].charAt(0) == 'A' ? "B" : "A") + partes[1].substring(1);
        String alterado = partes[0] + "." + cuerpoAlterado + "." + partes[2];

        assertFalse(jwtService.esTokenValido(alterado));
    }

    @Test
    void tokenSinFirma_noEsValido() {
        String sinFirma = Jwts.builder().subject("admin@aglome.com").claim("rol", "ADMIN").compact();

        assertFalse(jwtService.esTokenValido(sinFirma));
    }

    @Test
    void textoQueNoEsUnToken_noEsValido() {
        assertFalse(jwtService.esTokenValido("esto-no-es-un-token"));
        assertFalse(jwtService.esTokenValido(""));
        assertFalse(jwtService.esTokenValido(null));
    }

    @Test
    void claveSecretaMuyCorta_hacePorFallarElArranque() {
        assertThrows(RuntimeException.class, () -> new JwtService("corta", 1000));
    }
}
