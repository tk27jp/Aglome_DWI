package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.aglome_tienda.dto.AuthResponse;
import pe.edu.utp.aglome_tienda.dto.CambioContrasenaRequest;
import pe.edu.utp.aglome_tienda.exception.CredencialesInvalidasException;
import pe.edu.utp.aglome_tienda.exception.CuentaBloqueadaException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.dto.LoginRequest;
import pe.edu.utp.aglome_tienda.dto.RegistroRequest;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;
import pe.edu.utp.aglome_tienda.security.JwtService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registrar_conEmailNuevo_debeCrearUsuarioConRolCliente() {
        // Simular datos SIN error
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Jhonatan Navarro");
        request.setEmail("jhonatan@test.com");
        request.setPassword("123456");

        when(usuarioRepository.existsByEmail("jhonatan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashEncriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generarToken(anyString(), anyString())).thenReturn("token-falso");

        AuthResponse resultado = authService.registrar(request);

        // Verificar el test
        assertEquals("CLIENTE", resultado.getRol());
        assertEquals("token-falso", resultado.getToken());
        verify(usuarioRepository).save(argThat(u -> u.getRol() == Rol.CLIENTE));
    }

    @Test
    void registrar_conEmailYaExistente_debeLanzarExcepcion() {
        // Simular datos CON error (email duplicado)
        RegistroRequest request = new RegistroRequest();
        request.setEmail("jhonatan@test.com");
        request.setPassword("123456");

        when(usuarioRepository.existsByEmail("jhonatan@test.com")).thenReturn(true);

        // Verificar el test
        assertThrows(RuntimeException.class, () -> authService.registrar(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_conCredencialesCorrectas_debeRetornarToken() {
        // Simular datos SIN error
        Usuario usuario = new Usuario(1L, "Jhonatan Navarro", "jhonatan@test.com", "hashEncriptado", Rol.CLIENTE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jhonatan@test.com");
        request.setPassword("123456");

        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashEncriptado")).thenReturn(true);
        when(jwtService.generarToken(anyString(), anyString())).thenReturn("token-falso");

        AuthResponse resultado = authService.login(request);

        // Verificar el test
        assertEquals("token-falso", resultado.getToken());
    }

    @Test
    void login_conPasswordIncorrecta_debeLanzarExcepcion() {
        // Simular datos CON error (contraseña incorrecta)
        Usuario usuario = new Usuario(1L, "Jhonatan Navarro", "jhonatan@test.com", "hashEncriptado", Rol.CLIENTE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jhonatan@test.com");
        request.setPassword("incorrecta");

        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hashEncriptado")).thenReturn(false);

        // Verificar el test
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Email o contraseña incorrectos", ex.getMessage());
    }

    @Test
    void login_deUnVendedor_debeDevolverElRolVendedorEnLaRespuestaYElToken() {
        Usuario vendedor = new Usuario(5L, "Elton Vendedor", "elton@aglome.com", "hash", Rol.VENDEDOR);
        LoginRequest request = new LoginRequest();
        request.setEmail("elton@aglome.com");
        request.setPassword("secreto1");

        when(usuarioRepository.findByEmail("elton@aglome.com")).thenReturn(Optional.of(vendedor));
        when(passwordEncoder.matches("secreto1", "hash")).thenReturn(true);
        when(jwtService.generarToken("elton@aglome.com", "VENDEDOR")).thenReturn("token-vendedor");

        AuthResponse resultado = authService.login(request);

        assertEquals("VENDEDOR", resultado.getRol());
        assertEquals("token-vendedor", resultado.getToken());
    }

    private Usuario usuarioConIntentos(int intentos) {
        Usuario u = new Usuario(1L, "Jhonatan", "jhonatan@test.com", "hash", Rol.CLIENTE);
        u.setIntentosFallidos(intentos);
        return u;
    }

    private LoginRequest login(String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail("jhonatan@test.com");
        r.setPassword(password);
        return r;
    }

    @Test
    void login_conPasswordIncorrecta_cuentaElIntentoYGuarda() {
        Usuario u = usuarioConIntentos(0);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));

        assertEquals(1, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
        verify(usuarioRepository).save(u);
    }

    @Test
    void login_elQuintoIntentoFallido_bloqueaLaCuentaYLoAvisa() {
        Usuario u = usuarioConIntentos(AuthService.MAX_INTENTOS - 1);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        CuentaBloqueadaException ex = assertThrows(CuentaBloqueadaException.class,
                () -> authService.login(login("mala")));

        assertNotNull(u.getBloqueadoHasta());
        assertTrue(u.getBloqueadoHasta().isAfter(LocalDateTime.now().plusMinutes(AuthService.MINUTOS_BLOQUEO - 1)));
        assertTrue(ex.getMessage().contains("minutos"));
        verify(usuarioRepository).save(u);
    }

    @Test
    void login_conCuentaBloqueada_rechazaAunqueLaPasswordSeaCorrecta() {
        Usuario u = usuarioConIntentos(AuthService.MAX_INTENTOS);
        u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));

        assertThrows(CuentaBloqueadaException.class, () -> authService.login(login("correcta")));

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_conBloqueoVencido_dejaEntrarYBorraElBloqueo() {
        Usuario u = usuarioConIntentos(AuthService.MAX_INTENTOS);
        u.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);
        when(jwtService.generarToken("jhonatan@test.com", "CLIENTE")).thenReturn("token");

        AuthResponse resultado = authService.login(login("correcta"));

        assertEquals("token", resultado.getToken());
        assertEquals(0, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
    }

    @Test
    void login_conBloqueoVencidoYPasswordMala_empiezaDeCeroSinBloquearDeNuevo() {
        Usuario u = usuarioConIntentos(AuthService.MAX_INTENTOS);
        u.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(login("mala")));

        assertEquals(1, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
    }

    @Test
    void login_correctoDespuesDeAlgunosErrores_reiniciaElContador() {
        Usuario u = usuarioConIntentos(3);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);
        when(jwtService.generarToken("jhonatan@test.com", "CLIENTE")).thenReturn("token");

        authService.login(login("correcta"));

        assertEquals(0, u.getIntentosFallidos());
        verify(usuarioRepository).save(u);
    }

    @Test
    void login_correctoSinErroresPrevios_noEscribeEnLaBase() {
        Usuario u = usuarioConIntentos(0);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);
        when(jwtService.generarToken("jhonatan@test.com", "CLIENTE")).thenReturn("token");

        authService.login(login("correcta"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_conEmailInexistente_respondeIgualQueUnaClaveMalaYNoGuardaNada() {
        when(usuarioRepository.findByEmail("camila@test.com")).thenReturn(Optional.empty());
        LoginRequest request = login("x");
        request.setEmail("camila@test.com");

        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(request));

        assertEquals("Email o contraseña incorrectos", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    private CambioContrasenaRequest cambio(String actual, String nueva) {
        CambioContrasenaRequest r = new CambioContrasenaRequest();
        r.setContrasenaActual(actual);
        r.setContrasenaNueva(nueva);
        return r;
    }

    @Test
    void cambiarContrasena_conLaActualCorrecta_guardaLaNuevaEncriptada() {
        Usuario u = usuarioConIntentos(2);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Vieja123", "hash")).thenReturn(true);
        when(passwordEncoder.matches("Nueva12345", "hash")).thenReturn(false);
        when(passwordEncoder.encode("Nueva12345")).thenReturn("hashNuevo");

        authService.cambiarContrasena("jhonatan@test.com", cambio("Vieja123", "Nueva12345"));

        assertEquals("hashNuevo", u.getPassword());
        assertEquals(0, u.getIntentosFallidos());
        verify(usuarioRepository).save(u);
    }

    @Test
    void cambiarContrasena_conLaActualIncorrecta_lanzaReglaNegocioYCuentaElIntento() {
        Usuario u = usuarioConIntentos(0);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("equivocada", "hash")).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> authService.cambiarContrasena("jhonatan@test.com", cambio("equivocada", "Nueva12345")));

        assertEquals("hash", u.getPassword());   // no cambió
        assertEquals(1, u.getIntentosFallidos());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void cambiarContrasena_equivocarseLaVezNumeroCinco_bloqueaLaCuenta() {
        Usuario u = usuarioConIntentos(AuthService.MAX_INTENTOS - 1);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("equivocada", "hash")).thenReturn(false);

        assertThrows(CuentaBloqueadaException.class,
                () -> authService.cambiarContrasena("jhonatan@test.com", cambio("equivocada", "Nueva12345")));

        assertNotNull(u.getBloqueadoHasta());
    }

    @Test
    void cambiarContrasena_conCuentaBloqueada_lanzaCuentaBloqueada() {
        Usuario u = usuarioConIntentos(AuthService.MAX_INTENTOS);
        u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(5));
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));

        assertThrows(CuentaBloqueadaException.class,
                () -> authService.cambiarContrasena("jhonatan@test.com", cambio("Vieja123", "Nueva12345")));

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void cambiarContrasena_conUnaNuevaIgualALaActual_lanzaReglaNegocio() {
        Usuario u = usuarioConIntentos(0);
        when(usuarioRepository.findByEmail("jhonatan@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Vieja123", "hash")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> authService.cambiarContrasena("jhonatan@test.com", cambio("Vieja123", "Vieja123")));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarContrasena_conUsuarioInexistente_lanzaRecursoNoEncontrado() {
        when(usuarioRepository.findByEmail("fernando@test.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> authService.cambiarContrasena("fernando@test.com", cambio("a", "Nueva12345")));
    }
}
