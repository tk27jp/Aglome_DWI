package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductoBaseTest {

    @Test
    void productoBase_seCreaCorrectamente_conDatosValidos() {
        // Simular datos SIN error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase();
        producto.setNombre("Melamina Azul Marino");
        producto.setDescripcion("Aporta elegancia ejecutiva");
        producto.setMarca("Aglome");
        producto.setCategoria(categoria);
        producto.setPublicado(true);

        // Verificar el test
        assertEquals("Melamina Azul Marino", producto.getNombre());
        assertEquals("Aglome", producto.getMarca());
        assertTrue(producto.getPublicado());
        assertEquals("Melaminas", producto.getCategoria().getNombre());
    }

    @Test
    void productoBase_publicadoPorDefecto_esFalso() {
        // Simular datos SIN error (caso por defecto)
        ProductoBase producto = new ProductoBase();

        // Verificar el test
        assertFalse(producto.getPublicado()); // aún no se ha asignado ningún valor
    }
}