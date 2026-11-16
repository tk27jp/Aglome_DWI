package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void usuario_seCreaCorrectamente_conDatosValidos() {
        Usuario usuario = new Usuario(1L, "Jhonatan Navarro", "jhonatan@test.com", "hashEncriptado", Rol.CLIENTE);

        assertEquals("Jhonatan Navarro", usuario.getNombre());
        assertEquals(Rol.CLIENTE, usuario.getRol());
        assertNotEquals("123456", usuario.getPassword());
    }

    @Test
    void rol_soloAceptaValoresDefinidos() {
        Rol[] roles = Rol.values();

        // Verificar el test
        assertEquals(3, roles.length);
        assertTrue(java.util.Arrays.asList(roles).contains(Rol.ADMIN));
        assertTrue(java.util.Arrays.asList(roles).contains(Rol.VENDEDOR));
        assertTrue(java.util.Arrays.asList(roles).contains(Rol.CLIENTE));
    }
}