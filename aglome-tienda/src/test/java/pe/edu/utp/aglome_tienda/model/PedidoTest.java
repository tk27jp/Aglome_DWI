package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {

    @Test
    void pedido_seCreaCorrectamente_conEstadoConfirmado() {
        // Simular datos SIN error
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido.setTotal(new BigDecimal("91.80"));

        // Verificar el test
        assertEquals(EstadoPedido.CONFIRMADO, pedido.getEstado());
        assertEquals(new BigDecimal("91.80"), pedido.getTotal());
        assertEquals("Jonathan Navarro", pedido.getUsuario().getNombre());
    }
}