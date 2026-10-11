package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void usuario_seCreaCorrectamente_conDatosValidos() {
        // Simular datos SIN error
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "Jonathan@test.com", "hashEncriptado", Rol.CLIENTE);

        // Verificar el test
        assertEquals("Jonathan Navarro", usuario.getNombre());
        assertEquals(Rol.CLIENTE, usuario.getRol());
        assertNotEquals("123456", usuario.getPassword()); // nunca debe verse la clave en texto plano
    }

    @Test
    void rol_soloAceptaValoresDefinidos() {
        // Simular datos CON error (forzar un valor fuera del enum no compila,
        // así que la prueba real es confirmar que solo existen 2 valores válidos)
        Rol[] roles = Rol.values();

        // Verificar el test
        assertEquals(2, roles.length);
        assertTrue(java.util.Arrays.asList(roles).contains(Rol.ADMIN));
        assertTrue(java.util.Arrays.asList(roles).contains(Rol.CLIENTE));
    }
}