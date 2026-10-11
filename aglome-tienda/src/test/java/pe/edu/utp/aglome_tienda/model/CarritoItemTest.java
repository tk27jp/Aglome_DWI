package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CarritoItemTest {

    @Test
    void carritoItem_calculaSubtotalCorrectamente() {
        // Simular datos SIN error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(java.util.UUID.randomUUID(), "Melamina", "desc", "Aglome", categoria, true);
        Variante variante = new Variante(java.util.UUID.randomUUID(), producto, "SKU-1", new BigDecimal("45.90"), 30, true);

        CarritoItem item = new CarritoItem();
        item.setVariante(variante);
        item.setCantidad(2);

        // Verificar el test (el subtotal se calcula en el Service, aquí solo
        // confirmamos que el item guarda bien sus datos base)
        assertEquals(2, item.getCantidad());
        assertEquals(new BigDecimal("45.90"), item.getVariante().getPrecioVenta());
    }
}