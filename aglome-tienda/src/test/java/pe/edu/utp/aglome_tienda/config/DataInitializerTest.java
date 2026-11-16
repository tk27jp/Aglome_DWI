package pe.edu.utp.aglome_tienda.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private Usuario ejecutarYCapturar(DataInitializer inicializador) {
        inicializador.run();
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void conCredencialesDeEntorno_creaElAdminConEsasCredenciales() {
        when(usuarioRepository.existsByEmail("jefe@aglome.com")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura99")).thenReturn("HASH");

        Usuario admin = ejecutarYCapturar(
                new DataInitializer(usuarioRepository, passwordEncoder, "jefe@aglome.com", "ClaveSegura99"));

        assertEquals("jefe@aglome.com", admin.getEmail());
        assertEquals("HASH", admin.getPassword());          // guardada encriptada
        assertNotEquals("ClaveSegura99", admin.getPassword());
        assertEquals(Rol.ADMIN, admin.getRol());
    }

    @Test
    void sinCredenciales_usaLasDeDesarrollo() {
        when(usuarioRepository.existsByEmail("admin@aglome.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin123")).thenReturn("HASH");

        Usuario admin = ejecutarYCapturar(new DataInitializer(usuarioRepository, passwordEncoder, null, null));

        assertEquals("admin@aglome.com", admin.getEmail());
        assertEquals(Rol.ADMIN, admin.getRol());
    }

    @Test
    void variablesDeEntornoVacias_cuentanComoNoDefinidas() {
        when(usuarioRepository.existsByEmail("admin@aglome.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin123")).thenReturn("HASH");

        Usuario admin = ejecutarYCapturar(new DataInitializer(usuarioRepository, passwordEncoder, "  ", ""));

        assertEquals("admin@aglome.com", admin.getEmail());
    }

    @Test
    void siElAdminYaExiste_noHaceNadaYNoPisaSuContrasena() {
        when(usuarioRepository.existsByEmail("admin@aglome.com")).thenReturn(true);

        new DataInitializer(usuarioRepository, passwordEncoder, "admin@aglome.com", "OtraClave1").run();

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
