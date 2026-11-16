package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.edu.utp.aglome_tienda.dto.PedidoDTO;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final String EMAIL = "jhonatan@test.com";

    @Mock private PedidoRepository pedidoRepository;
    @Mock private DetallePedidoRepository detallePedidoRepository;
    @Mock private CarritoRepository carritoRepository;
    @Mock private CarritoItemRepository carritoItemRepository;
    @Mock private VarianteRepository varianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CarritoService carritoService;
    @Mock private MovimientoStockService movimientoStockService;

    @InjectMocks
    private PedidoService pedidoService;

    private Usuario crearUsuario(Long id) {
        return new Usuario(id, "Jhonatan Navarro", EMAIL, "hash", Rol.CLIENTE);
    }

    private Variante crearVariante(int stock) {
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(UUID.randomUUID(), "Melamina", "desc", new Marca(1L, "Aglome"), categoria, true);
        return new Variante(UUID.randomUUID(), producto, "SKU-1", new BigDecimal("45.90"), stock, true);
    }

    private Pedido crearPedido(Usuario usuario, EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(estado);
        pedido.setTotal(new BigDecimal("91.80"));
        return pedido;
    }

    @Test
    void confirmarCompra_conStockSuficiente_debeDescontarStockYVaciarCarrito() {
        // Simular datos SIN error
        Usuario usuario = crearUsuario(1L);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Variante variante = crearVariante(10);
        CarritoItem item = new CarritoItem(UUID.randomUUID(), carrito, variante, 2);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detallePedidoRepository.findByPedidoId(any())).thenReturn(List.of());

        PedidoDTO resultado = pedidoService.confirmarCompra(EMAIL);

        // Verificar el test
        assertNotNull(resultado);
        assertEquals("CONFIRMADO", resultado.getEstado());
        assertEquals(0, new BigDecimal("91.80").compareTo(resultado.getTotal()));
        assertEquals(8, variante.getStock());
        verify(detallePedidoRepository, times(1)).save(any(DetallePedido.class));
        verify(carritoService, times(1)).vaciarCarrito(carrito.getId());
    }

    @Test
    void confirmarCompra_conCarritoVacio_debeLanzarReglaNegocio() {
        // Simular datos CON error (carrito sin items)
        Usuario usuario = crearUsuario(1L);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of());

        // Verificar el test
        assertThrows(ReglaNegocioException.class, () -> pedidoService.confirmarCompra(EMAIL));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_conStockInsuficiente_debeLanzarConflictoYNoGuardar() {
        // Simular datos CON error (piden 5 y solo hay 1)
        Usuario usuario = crearUsuario(1L);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Variante variante = crearVariante(1);
        CarritoItem item = new CarritoItem(UUID.randomUUID(), carrito, variante, 5);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));

        // Verificar el test
        assertThrows(ConflictoException.class, () -> pedidoService.confirmarCompra(EMAIL));
        assertEquals(1, variante.getStock());
        verify(pedidoRepository, never()).save(any());
        verify(carritoService, never()).vaciarCarrito(any());
    }

    @Test
    void confirmarCompra_conUsuarioInexistente_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Verificar el test
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.confirmarCompra(EMAIL));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_deOtroUsuario_debeLanzarAccessDenied() {
        // Simular datos CON error (el pedido es de otro usuario)
        Usuario yo = crearUsuario(1L);
        Usuario otro = crearUsuario(2L);
        Pedido pedidoAjeno = crearPedido(otro, EstadoPedido.CONFIRMADO);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(yo));
        when(pedidoRepository.findById(pedidoAjeno.getId())).thenReturn(Optional.of(pedidoAjeno));

        // Verificar el test
        assertThrows(AccessDeniedException.class,
                () -> pedidoService.obtenerPorId(EMAIL, pedidoAjeno.getId()));
    }

    @Test
    void cambiarEstado_aCancelado_debeDevolverElStockAlInventario() {
        // Simular datos SIN error
        Usuario usuario = crearUsuario(1L);
        Pedido pedido = crearPedido(usuario, EstadoPedido.CONFIRMADO);
        Variante variante = crearVariante(5);
        DetallePedido detalle = new DetallePedido(UUID.randomUUID(), pedido, variante, 3,
                new BigDecimal("45.90"), new BigDecimal("137.70"));

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(detalle));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO resultado = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);

        // Verificar el test
        assertEquals("CANCELADO", resultado.getEstado());
        assertEquals(8, variante.getStock());
        verify(varianteRepository, times(1)).save(variante);
    }

    @Test
    void cambiarEstado_conPedidoYaCancelado_debeLanzarConflicto() {
        // Simular datos CON error
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.CANCELADO);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        // Verificar el test
        assertThrows(ConflictoException.class,
                () -> pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CONFIRMADO));
        verify(varianteRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void confirmarCompra_conProductoEliminadoDespuesDeAgregarlo_debeLanzarConflictoYNoGuardar() {
        // Simular datos CON error (el admin eliminó el producto mientras estaba en el carrito)
        Usuario usuario = crearUsuario(1L);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Variante variante = crearVariante(10);
        variante.getProductoBase().setEliminado(true);
        CarritoItem item = new CarritoItem(UUID.randomUUID(), carrito, variante, 1);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));

        // Verificar el test
        assertThrows(ConflictoException.class, () -> pedidoService.confirmarCompra(EMAIL));
        verify(pedidoRepository, never()).save(any());
        assertEquals(10, variante.getStock());
    }

    // ---------- Paso 10: flujo de estados ----------

    private DetallePedido detalleDe(Pedido pedido, Variante variante, int cantidad) {
        return new DetallePedido(UUID.randomUUID(), pedido, variante, cantidad,
                new BigDecimal("45.90"), new BigDecimal("45.90").multiply(BigDecimal.valueOf(cantidad)));
    }

    @Test
    void cambiarEstado_deConfirmadoAEnPreparacion_cambiaElEstadoSinTocarElStock() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO resultado = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.EN_PREPARACION);

        assertEquals("EN_PREPARACION", resultado.getEstado());
        assertEquals(List.of("ENVIADO", "CANCELADO"), resultado.getSiguientesEstados());
        verify(varianteRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_conSaltoInvalido_debeLanzarConflictoConLosEstadosPermitidos() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.ENTREGADO));

        assertTrue(ex.getMessage().contains("CONFIRMADO"));
        assertTrue(ex.getMessage().contains("EN_PREPARACION"));
        assertEquals(EstadoPedido.CONFIRMADO, pedido.getEstado());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_cancelarUnPedidoEnviado_debeLanzarConflictoYNoTocarElStock() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.ENVIADO);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThrows(ConflictoException.class,
                () -> pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO));

        verify(varianteRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_deUnPedidoEntregado_debeLanzarConflicto() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.ENTREGADO);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CONFIRMADO));

        assertTrue(ex.getMessage().contains("ENTREGADO"));
    }

    @Test
    void cambiarEstado_cancelarEnPreparacion_devuelveElStock() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.EN_PREPARACION);
        Variante variante = crearVariante(5);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(detalleDe(pedido, variante, 2)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO resultado = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);

        assertEquals("CANCELADO", resultado.getEstado());
        assertEquals(7, variante.getStock());
        assertTrue(resultado.getSiguientesEstados().isEmpty());
    }

    @Test
    void cambiarEstado_cancelarUnPendiente_noTocaElStockPorqueNuncaSeDescontó() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.PENDIENTE);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);

        verify(varianteRepository, never()).save(any());   // no se devuelve stock que nunca se descontó
    }

    @Test
    void cambiarEstado_conPedidoInexistente_debeLanzarRecursoNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(pedidoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pedidoService.cambiarEstado(id, EstadoPedido.EN_PREPARACION));
    }

    @Test
    void cancelarMiPedido_confirmado_cancelaYDevuelveElStock() {
        Usuario yo = crearUsuario(1L);
        Pedido pedido = crearPedido(yo, EstadoPedido.CONFIRMADO);
        Variante variante = crearVariante(3);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(yo));
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(detalleDe(pedido, variante, 4)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDTO resultado = pedidoService.cancelarMiPedido(EMAIL, pedido.getId());

        assertEquals("CANCELADO", resultado.getEstado());
        assertEquals(7, variante.getStock());
    }

    @Test
    void cancelarMiPedido_deOtroUsuario_debeLanzarAccessDenied() {
        Usuario yo = crearUsuario(1L);
        Pedido ajeno = crearPedido(crearUsuario(2L), EstadoPedido.CONFIRMADO);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(yo));
        when(pedidoRepository.findById(ajeno.getId())).thenReturn(Optional.of(ajeno));

        assertThrows(AccessDeniedException.class, () -> pedidoService.cancelarMiPedido(EMAIL, ajeno.getId()));
        assertEquals(EstadoPedido.CONFIRMADO, ajeno.getEstado());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelarMiPedido_yaEnPreparacion_debeLanzarConflicto() {
        Usuario yo = crearUsuario(1L);
        Pedido pedido = crearPedido(yo, EstadoPedido.EN_PREPARACION);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(yo));
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        ConflictoException ex = assertThrows(ConflictoException.class,
                () -> pedidoService.cancelarMiPedido(EMAIL, pedido.getId()));

        assertTrue(ex.getMessage().contains("EN_PREPARACION"));
        verify(varianteRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelarMiPedido_inexistente_debeLanzarRecursoNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(crearUsuario(1L)));
        when(pedidoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.cancelarMiPedido(EMAIL, id));
    }

    @Test
    void listarTodosAdmin_sinEstado_usaElListadoCompleto() {
        Pedido p = crearPedido(crearUsuario(1L), EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(p));

        List<PedidoDTO> resultado = pedidoService.listarTodosAdmin(null);

        assertEquals(1, resultado.size());
        verify(pedidoRepository, never()).findByEstadoOrderByFechaDesc(any());
    }

    @Test
    void listarTodosAdmin_conEstado_filtraPorEseEstado() {
        Pedido p = crearPedido(crearUsuario(1L), EstadoPedido.ENVIADO);
        when(pedidoRepository.findByEstadoOrderByFechaDesc(EstadoPedido.ENVIADO)).thenReturn(List.of(p));

        List<PedidoDTO> resultado = pedidoService.listarTodosAdmin(EstadoPedido.ENVIADO);

        assertEquals("ENVIADO", resultado.get(0).getEstado());
        assertEquals(List.of("ENTREGADO"), resultado.get(0).getSiguientesEstados());
        verify(pedidoRepository, never()).findAllByOrderByFechaDesc();
    }

    @Test
    void obtenerPorIdAdmin_inexistente_debeLanzarRecursoNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(pedidoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.obtenerPorIdAdmin(id));
    }

    @Test
    void obtenerPorIdAdmin_existente_devuelveElPedidoDeCualquierUsuario() {
        Pedido p = crearPedido(crearUsuario(9L), EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findById(p.getId())).thenReturn(Optional.of(p));

        PedidoDTO resultado = pedidoService.obtenerPorIdAdmin(p.getId());

        assertEquals(p.getId(), resultado.getId());
    }

    // ---------- Paso 11: historial de stock ----------

    @Test
    void confirmarCompra_registraUnMovimientoDeVentaPorCadaItem() {
        Usuario usuario = crearUsuario(1L);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Variante variante = crearVariante(10);
        CarritoItem item = new CarritoItem(UUID.randomUUID(), carrito, variante, 2);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.confirmarCompra(EMAIL);

        verify(movimientoStockService, times(1)).registrar(eq(variante), eq(TipoMovimiento.VENTA),
                eq(10), eq(8), anyString(), any());
    }

    @Test
    void confirmarCompra_conStockInsuficiente_noRegistraMovimientos() {
        Usuario usuario = crearUsuario(1L);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        CarritoItem item = new CarritoItem(UUID.randomUUID(), carrito, crearVariante(1), 5);

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByCarritoId(carrito.getId())).thenReturn(List.of(item));

        assertThrows(ConflictoException.class, () -> pedidoService.confirmarCompra(EMAIL));

        verifyNoInteractions(movimientoStockService);
    }

    @Test
    void cambiarEstado_cancelarUnPedido_registraUnMovimientoDeCancelacion() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.CONFIRMADO);
        Variante variante = crearVariante(5);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(detalleDe(pedido, variante, 3)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);

        verify(movimientoStockService).registrar(eq(variante), eq(TipoMovimiento.CANCELACION),
                eq(5), eq(8), anyString(), eq(pedido.getId()));
    }

    @Test
    void cambiarEstado_cancelarUnPendiente_noRegistraMovimientos() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.PENDIENTE);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);

        verifyNoInteractions(movimientoStockService);
    }

    @Test
    void cambiarEstado_sinDevolverStock_noRegistraMovimientos() {
        Pedido pedido = crearPedido(crearUsuario(1L), EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.EN_PREPARACION);

        verifyNoInteractions(movimientoStockService);
    }

    @Test
    void cancelarMiPedido_registraUnMovimientoDeCancelacion() {
        Usuario yo = crearUsuario(1L);
        Pedido pedido = crearPedido(yo, EstadoPedido.CONFIRMADO);
        Variante variante = crearVariante(3);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(yo));
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(detalleDe(pedido, variante, 4)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.cancelarMiPedido(EMAIL, pedido.getId());

        verify(movimientoStockService).registrar(eq(variante), eq(TipoMovimiento.CANCELACION),
                eq(3), eq(7), anyString(), eq(pedido.getId()));
    }
}
