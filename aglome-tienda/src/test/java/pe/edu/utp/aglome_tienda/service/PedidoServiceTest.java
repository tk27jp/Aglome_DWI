package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.PedidoDTO;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private DetallePedidoRepository detallePedidoRepository;
    @Mock private CarritoRepository carritoRepository;
    @Mock private CarritoItemRepository carritoItemRepository;
    @Mock private VarianteRepository varianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CarritoService carritoService;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    void confirmarCompra_conStockSuficiente_debeCrearPedidoYDescontarStock() {
        // Simular datos SIN error
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(UUID.randomUUID(), "Melamina", "desc", "Aglome", categoria, true);
        Variante variante = new Variante(UUID.randomUUID(), producto, "SKU-1", new BigDecimal("45.90"), 30, true);

        CarritoItem item = new CarritoItem();
        item.setCarrito(carrito);
        item.setVariante(variante);
        item.setCantidad(2);

        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detallePedidoRepository.findByPedidoId(any())).thenReturn(List.of());

        PedidoDTO resultado = pedidoService.confirmarCompra("ana@test.com");

        // Verificar el test
        assertNotNull(resultado);
        assertEquals(28, variante.getStock()); // 30 - 2 = 28
        verify(varianteRepository, times(1)).save(variante);
        verify(carritoService, times(1)).vaciarCarrito(carrito.getId());
    }

    @Test
    void confirmarCompra_conCarritoVacio_debeLanzarExcepcion() {
        // Simular datos CON error (carrito sin productos)
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);

        when(usuarioRepository.findByEmail("jonathan@test.com")).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of());

        // Verificar el test
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pedidoService.confirmarCompra("jonathan@test.com"));
        assertEquals("Tu carrito está vacío, no se puede confirmar la compra", ex.getMessage());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_conStockInsuficiente_debeLanzarExcepcionYNoDescontarNada() {
        // Simular datos CON error (no hay stock suficiente)
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(UUID.randomUUID(), "Melamina", "desc", "Aglome", categoria, true);
        Variante variante = new Variante(UUID.randomUUID(), producto, "SKU-1", new BigDecimal("45.90"), 1, true);

        CarritoItem item = new CarritoItem();
        item.setCarrito(carrito);
        item.setVariante(variante);
        item.setCantidad(5); // pide 5, hay 1

        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));

        // Verificar el test
        assertThrows(RuntimeException.class, () -> pedidoService.confirmarCompra("ana@test.com"));
        verify(pedidoRepository, never()).save(any());
        assertEquals(1, variante.getStock()); // el stock NO debe haberse tocado
    }
}