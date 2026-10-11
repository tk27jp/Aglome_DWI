package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoriaTest {

    @Test
    void categoria_seCreaCorrectamente_conDatosValidos() {
        // Simular datos SIN error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);

        // Verificar el test
        assertEquals("Melaminas", categoria.getNombre());
        assertNull(categoria.getPadre());
        assertTrue(categoria.getActiva());
    }

    @Test
    void categoria_puedeTenerUnaCategoriaPadre_paraFormarSubcategorias() {
        // Simular datos SIN error
        Categoria padre = new Categoria(1L, "Melaminas", null, true);
        Categoria hija = new Categoria(2L, "Melaminas 18mm", padre, true);

        // Verificar el test
        assertEquals("Melaminas", hija.getPadre().getNombre());
    }
}