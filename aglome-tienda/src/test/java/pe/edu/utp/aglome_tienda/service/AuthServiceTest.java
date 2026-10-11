package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.aglome_tienda.dto.AuthResponse;
import pe.edu.utp.aglome_tienda.dto.LoginRequest;
import pe.edu.utp.aglome_tienda.dto.RegistroRequest;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;
import pe.edu.utp.aglome_tienda.security.JwtService;

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
        request.setNombre("Jonathan Navarro");
        request.setEmail("jonathan@test.com");
        request.setPassword("123456");

        when(usuarioRepository.existsByEmail("jonathan@test.com")).thenReturn(false);
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
        request.setEmail("ana@test.com");
        request.setPassword("123456");

        when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(true);

        // Verificar el test
        assertThrows(RuntimeException.class, () -> authService.registrar(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_conCredencialesCorrectas_debeRetornarToken() {
        // Simular datos SIN error
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hashEncriptado", Rol.CLIENTE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jonathan@test.com");
        request.setPassword("123456");

        when(usuarioRepository.findByEmail("jonathan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashEncriptado")).thenReturn(true);
        when(jwtService.generarToken(anyString(), anyString())).thenReturn("token-falso");

        AuthResponse resultado = authService.login(request);

        // Verificar el test
        assertEquals("token-falso", resultado.getToken());
    }

    @Test
    void login_conPasswordIncorrecta_debeLanzarExcepcion() {
        // Simular datos CON error (contraseña incorrecta)
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hashEncriptado", Rol.CLIENTE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jonathan@test.com");
        request.setPassword("incorrecta");

        when(usuarioRepository.findByEmail("jonathan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hashEncriptado")).thenReturn(false);

        // Verificar el test
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Email o contraseña incorrectos", ex.getMessage());
    }
}