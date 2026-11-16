package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.aglome_tienda.dto.CrearUsuarioRequest;
import pe.edu.utp.aglome_tienda.dto.PerfilDTO;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioAdminService usuarioAdminService;

    private CrearUsuarioRequest request(String email, Rol rol) {
        CrearUsuarioRequest r = new CrearUsuarioRequest();
        r.setNombre("Elton Vendedor");
        r.setEmail(email);
        r.setPassword("secreto1");
        r.setRol(rol);
        return r;
    }

    @Test
    void crear_unVendedor_guardaLaContrasenaEncriptadaYNoLaDevuelve() {
        when(usuarioRepository.existsByEmail("elton@aglome.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto1")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });

        PerfilDTO dto = usuarioAdminService.crear(request("elton@aglome.com", Rol.VENDEDOR));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("HASH", captor.getValue().getPassword());   // nunca "secreto1"
        assertEquals(Rol.VENDEDOR, captor.getValue().getRol());
        assertEquals(7L, dto.getId());
        assertEquals("VENDEDOR", dto.getRol());
        assertEquals("elton@aglome.com", dto.getEmail());
    }

    @Test
    void crear_unAdministrador_estaPermitido() {
        when(usuarioRepository.existsByEmail("jefe@aglome.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto1")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilDTO dto = usuarioAdminService.crear(request("jefe@aglome.com", Rol.ADMIN));

        assertEquals("ADMIN", dto.getRol());
    }

    @Test
    void crear_unCliente_lanzaReglaNegocioPorqueLosClientesSeRegistranSolos() {
        assertThrows(ReglaNegocioException.class,
                () -> usuarioAdminService.crear(request("cli@test.com", Rol.CLIENTE)));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void crear_conEmailYaExistente_lanzaConflictoYNoGuarda() {
        when(usuarioRepository.existsByEmail("elton@aglome.com")).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> usuarioAdminService.crear(request("elton@aglome.com", Rol.VENDEDOR)));

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void listar_sinRol_devuelveTodasLasCuentasOrdenadasPorNombre() {
        when(usuarioRepository.findAll(any(Sort.class))).thenReturn(List.of(
                new Usuario(1L, "Jhonatan", "jhonatan@test.com", "h", Rol.CLIENTE),
                new Usuario(2L, "Elton", "elton@aglome.com", "h", Rol.VENDEDOR)));

        List<PerfilDTO> resultado = usuarioAdminService.listar(null);

        assertEquals(2, resultado.size());
        assertEquals("VENDEDOR", resultado.get(1).getRol());
        verify(usuarioRepository, never()).findByRol(any(), any());
    }

    @Test
    void listar_conRol_pideSoloEseRolOrdenadoPorNombre() {
        when(usuarioRepository.findByRol(eq(Rol.VENDEDOR), any(Sort.class))).thenReturn(List.of(
                new Usuario(2L, "Elton", "elton@aglome.com", "h", Rol.VENDEDOR)));

        List<PerfilDTO> resultado = usuarioAdminService.listar(Rol.VENDEDOR);

        assertEquals(1, resultado.size());
        assertEquals("Elton", resultado.get(0).getNombre());
        ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
        verify(usuarioRepository).findByRol(eq(Rol.VENDEDOR), captor.capture());
        assertNotNull(captor.getValue().getOrderFor("nombre"));
    }
}
