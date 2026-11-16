package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.aglome_tienda.dto.CambioContrasenaRequest;
import pe.edu.utp.aglome_tienda.dto.LoginRequest;
import pe.edu.utp.aglome_tienda.exception.CredencialesInvalidasException;
import pe.edu.utp.aglome_tienda.exception.CuentaBloqueadaException;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Con la base de datos de verdad (H2) y sin mocks. Comprueba lo que los tests con mocks no pueden:
 * que el contador de intentos fallidos QUEDA GUARDADO aunque el login termine en excepción
 * (si AuthService fuera @Transactional, la excepción deshacería el guardado y el bloqueo nunca funcionaría).
 */
@SpringBootTest
class AuthBloqueoIntegracionTest {

    private static final String EMAIL = "bloqueo.integracion@test.com";
    private static final String CLAVE = "Secreta123";

    @Autowired private AuthService authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void crearUsuario() {
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
        usuarioRepository.save(new Usuario(null, "Prueba Bloqueo", EMAIL, passwordEncoder.encode(CLAVE), Rol.CLIENTE));
    }

    @AfterEach
    void borrarUsuario() {
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
    }

    private LoginRequest login(String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(EMAIL);
        r.setPassword(password);
        return r;
    }

    private Usuario enLaBase() {
        return usuarioRepository.findByEmail(EMAIL).orElseThrow();
    }

    @Test
    void losIntentosFallidosQuedanGuardadosEnLaBase() {
        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));
        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));

        assertEquals(2, enLaBase().getIntentosFallidos());
    }

    @Test
    void despuesDeCincoErrores_laCuentaSeBloqueaYYaNoDejaEntrarNiConLaClaveCorrecta() {
        for (int i = 1; i < AuthService.MAX_INTENTOS; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));
        }
        assertThrows(CuentaBloqueadaException.class, () -> authService.login(login("mala")));   // el quinto

        Usuario bloqueado = enLaBase();
        assertNotNull(bloqueado.getBloqueadoHasta());
        assertTrue(bloqueado.getBloqueadoHasta().isAfter(LocalDateTime.now()));

        assertThrows(CuentaBloqueadaException.class, () -> authService.login(login(CLAVE)));
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));
        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));

        assertNotNull(authService.login(login(CLAVE)).getToken());

        assertEquals(0, enLaBase().getIntentosFallidos());
    }

    @Test
    void cuandoPasaElTiempoDeBloqueo_laCuentaVuelveAFuncionar() {
        Usuario u = enLaBase();
        u.setIntentosFallidos(AuthService.MAX_INTENTOS);
        u.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));   // el bloqueo venció hace un minuto
        usuarioRepository.save(u);

        assertNotNull(authService.login(login(CLAVE)).getToken());

        assertNull(enLaBase().getBloqueadoHasta());
        assertEquals(0, enLaBase().getIntentosFallidos());
    }

    @Test
    void despuesDeCambiarLaContrasena_laVieja_yaNoSirve_yLaNuevaSi() {
        CambioContrasenaRequest cambio = new CambioContrasenaRequest();
        cambio.setContrasenaActual(CLAVE);
        cambio.setContrasenaNueva("NuevaClave456");

        authService.cambiarContrasena(EMAIL, cambio);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login(CLAVE)));
        assertNotNull(authService.login(login("NuevaClave456")).getToken());
        assertNotEquals("NuevaClave456", enLaBase().getPassword());   // guardada encriptada
    }
}
