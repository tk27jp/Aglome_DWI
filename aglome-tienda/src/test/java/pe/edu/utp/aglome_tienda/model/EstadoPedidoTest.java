package pe.edu.utp.aglome_tienda.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadoPedidoTest {

    @Test
    void flujoNormal_avanzaPasoAPaso() {
        assertTrue(EstadoPedido.CONFIRMADO.puedePasarA(EstadoPedido.EN_PREPARACION));
        assertTrue(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.ENVIADO));
        assertTrue(EstadoPedido.ENVIADO.puedePasarA(EstadoPedido.ENTREGADO));
    }

    @Test
    void noSePermitenSaltosNiRetrocesos() {
        assertFalse(EstadoPedido.CONFIRMADO.puedePasarA(EstadoPedido.ENVIADO));
        assertFalse(EstadoPedido.CONFIRMADO.puedePasarA(EstadoPedido.ENTREGADO));
        assertFalse(EstadoPedido.ENVIADO.puedePasarA(EstadoPedido.EN_PREPARACION));
        assertFalse(EstadoPedido.ENTREGADO.puedePasarA(EstadoPedido.ENVIADO));
    }

    @Test
    void cancelar_soloSePuedeAntesDeEnviar() {
        assertTrue(EstadoPedido.PENDIENTE.puedePasarA(EstadoPedido.CANCELADO));
        assertTrue(EstadoPedido.CONFIRMADO.puedePasarA(EstadoPedido.CANCELADO));
        assertTrue(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.CANCELADO));
        assertFalse(EstadoPedido.ENVIADO.puedePasarA(EstadoPedido.CANCELADO));
        assertFalse(EstadoPedido.ENTREGADO.puedePasarA(EstadoPedido.CANCELADO));
    }

    @Test
    void entregadoYCancelado_sonEstadosFinales() {
        assertTrue(EstadoPedido.ENTREGADO.esFinal());
        assertTrue(EstadoPedido.CANCELADO.esFinal());
        assertEquals(List.of(), EstadoPedido.CANCELADO.siguientes());
        assertFalse(EstadoPedido.CONFIRMADO.esFinal());
    }

    @Test
    void ningunEstadoPuedePasarASiMismo() {
        for (EstadoPedido e : EstadoPedido.values()) {
            assertFalse(e.puedePasarA(e), e + " no debería poder pasar a sí mismo");
        }
    }

    @Test
    void clienteSoloPuedeCancelarPendienteOConfirmado() {
        assertTrue(EstadoPedido.PENDIENTE.cancelablePorCliente());
        assertTrue(EstadoPedido.CONFIRMADO.cancelablePorCliente());
        assertFalse(EstadoPedido.EN_PREPARACION.cancelablePorCliente());
        assertFalse(EstadoPedido.ENVIADO.cancelablePorCliente());
        assertFalse(EstadoPedido.ENTREGADO.cancelablePorCliente());
        assertFalse(EstadoPedido.CANCELADO.cancelablePorCliente());
    }
}
