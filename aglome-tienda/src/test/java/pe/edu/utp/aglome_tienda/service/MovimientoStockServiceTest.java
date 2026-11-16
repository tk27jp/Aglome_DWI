package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.utp.aglome_tienda.dto.AjusteStockRequest;
import pe.edu.utp.aglome_tienda.dto.MovimientoStockDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.MovimientoStock;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.model.TipoMovimiento;
import pe.edu.utp.aglome_tienda.model.Variante;
import pe.edu.utp.aglome_tienda.repository.MovimientoStockRepository;
import pe.edu.utp.aglome_tienda.repository.VarianteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceTest {

    @Mock private MovimientoStockRepository movimientoStockRepository;
    @Mock private VarianteRepository varianteRepository;

    @InjectMocks
    private MovimientoStockService movimientoStockService;

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private Variante variante(int stock) {
        ProductoBase p = new ProductoBase();
        p.setNombre("Melamina Azul");
        Variante v = new Variante();
        v.setId(UUID.randomUUID());
        v.setProductoBase(p);
        v.setSku("MEL-1");
        v.setStock(stock);
        return v;
    }

    private MovimientoStock movimientoDe(Variante v, TipoMovimiento tipo, int cantidad) {
        MovimientoStock m = new MovimientoStock();
        m.setId(UUID.randomUUID());
        m.setVariante(v);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockAnterior(10);
        m.setStockResultante(10 + cantidad);
        m.setMotivo("motivo");
        m.setFecha(LocalDateTime.now());
        return m;
    }

    private AjusteStockRequest ajuste(UUID varianteId, int cantidad, String motivo) {
        AjusteStockRequest r = new AjusteStockRequest();
        r.setVarianteId(varianteId);
        r.setCantidad(cantidad);
        r.setMotivo(motivo);
        return r;
    }

    // ---------- registrar ----------

    @Test
    void registrar_guardaLaCantidadConSignoYElUsuarioDeLaSesion() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("jhonatan@test.com", null, List.of()));
        Variante v = variante(8);
        UUID pedidoId = UUID.randomUUID();

        movimientoStockService.registrar(v, TipoMovimiento.VENTA, 10, 8, "Compra confirmada", pedidoId);

        ArgumentCaptor<MovimientoStock> captor = ArgumentCaptor.forClass(MovimientoStock.class);
        verify(movimientoStockRepository).save(captor.capture());
        MovimientoStock m = captor.getValue();
        assertEquals(TipoMovimiento.VENTA, m.getTipo());
        assertEquals(-2, m.getCantidad());
        assertEquals(10, m.getStockAnterior());
        assertEquals(8, m.getStockResultante());
        assertEquals(pedidoId, m.getPedidoId());
        assertEquals("jhonatan@test.com", m.getUsuarioEmail());
        assertNotNull(m.getFecha());
    }

    @Test
    void registrar_sinSesion_guardaUsuarioNull() {
        movimientoStockService.registrar(variante(5), TipoMovimiento.CANCELACION, 3, 5, "Pedido cancelado", null);

        ArgumentCaptor<MovimientoStock> captor = ArgumentCaptor.forClass(MovimientoStock.class);
        verify(movimientoStockRepository).save(captor.capture());
        assertNull(captor.getValue().getUsuarioEmail());
        assertEquals(2, captor.getValue().getCantidad());
    }

    // ---------- ajustar ----------

    @Test
    void ajustar_conCantidadPositiva_sumaStockYDejaElMovimiento() {
        Variante v = variante(10);
        when(varianteRepository.findById(v.getId())).thenReturn(Optional.of(v));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoStockDTO dto = movimientoStockService.ajustar(ajuste(v.getId(), 5, "  Reposición del proveedor  "));

        assertEquals(15, v.getStock());
        verify(varianteRepository).save(v);
        assertEquals("AJUSTE", dto.getTipo());
        assertEquals(5, dto.getCantidad());
        assertEquals(10, dto.getStockAnterior());
        assertEquals(15, dto.getStockResultante());
        assertEquals("Reposición del proveedor", dto.getMotivo());
        assertEquals("MEL-1", dto.getSku());
    }

    @Test
    void ajustar_conCantidadNegativa_restaStock() {
        Variante v = variante(10);
        when(varianteRepository.findById(v.getId())).thenReturn(Optional.of(v));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoStockDTO dto = movimientoStockService.ajustar(ajuste(v.getId(), -4, "Merma por humedad"));

        assertEquals(6, v.getStock());
        assertEquals(-4, dto.getCantidad());
    }

    @Test
    void ajustar_queDejariaElStockNegativo_lanzaReglaNegocioYNoGuarda() {
        Variante v = variante(3);
        when(varianteRepository.findById(v.getId())).thenReturn(Optional.of(v));

        assertThrows(ReglaNegocioException.class,
                () -> movimientoStockService.ajustar(ajuste(v.getId(), -4, "Conteo")));

        assertEquals(3, v.getStock());
        verify(varianteRepository, never()).save(any());
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    void ajustar_conCantidadCero_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> movimientoStockService.ajustar(ajuste(UUID.randomUUID(), 0, "Nada")));
        verifyNoInteractions(varianteRepository, movimientoStockRepository);
    }

    @Test
    void ajustar_conVarianteInexistente_lanzaRecursoNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(varianteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> movimientoStockService.ajustar(ajuste(id, 1, "Reposición")));
    }

    @Test
    void ajustar_conVarianteEliminada_lanzaRecursoNoEncontrado() {
        Variante v = variante(10);
        v.setEliminado(true);
        when(varianteRepository.findById(v.getId())).thenReturn(Optional.of(v));

        assertThrows(RecursoNoEncontradoException.class,
                () -> movimientoStockService.ajustar(ajuste(v.getId(), 1, "Reposición")));
        verify(movimientoStockRepository, never()).save(any());
    }

    // ---------- listar ----------

    @Test
    void listar_sinFiltros_pideTodoOrdenadoPorFechaDescendente() {
        Variante v = variante(8);
        when(movimientoStockRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimientoDe(v, TipoMovimiento.VENTA, -2))));

        PaginaDTO<MovimientoStockDTO> pagina = movimientoStockService.listar(null, null, 0, 20);

        assertEquals(1, pagina.getTotalElementos());
        assertEquals("VENTA", pagina.getContenido().get(0).getTipo());
        assertEquals("Melamina Azul", pagina.getContenido().get(0).getProducto());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(movimientoStockRepository).findAll(captor.capture());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("fecha").getDirection());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void listar_porVariante_usaElFiltroDeVariante() {
        Variante v = variante(8);
        when(varianteRepository.existsById(v.getId())).thenReturn(true);
        when(movimientoStockRepository.findByVarianteId(eq(v.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimientoDe(v, TipoMovimiento.AJUSTE, 5))));

        PaginaDTO<MovimientoStockDTO> pagina = movimientoStockService.listar(v.getId(), null, 0, 10);

        assertEquals(1, pagina.getContenido().size());
        verify(movimientoStockRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listar_porTipo_usaElFiltroDeTipo() {
        Variante v = variante(8);
        when(movimientoStockRepository.findByTipo(eq(TipoMovimiento.VENTA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimientoDe(v, TipoMovimiento.VENTA, -1))));

        assertEquals(1, movimientoStockService.listar(null, TipoMovimiento.VENTA, 0, 10).getContenido().size());
    }

    @Test
    void listar_porVarianteYTipo_usaAmbosFiltros() {
        Variante v = variante(8);
        when(varianteRepository.existsById(v.getId())).thenReturn(true);
        when(movimientoStockRepository.findByVarianteIdAndTipo(eq(v.getId()), eq(TipoMovimiento.AJUSTE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimientoDe(v, TipoMovimiento.AJUSTE, 2))));

        assertEquals(1, movimientoStockService.listar(v.getId(), TipoMovimiento.AJUSTE, 0, 10).getContenido().size());
    }

    @Test
    void listar_conVarianteInexistente_lanzaRecursoNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(varianteRepository.existsById(id)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> movimientoStockService.listar(id, null, 0, 10));
    }

    @Test
    void listar_conPaginacionInvalida_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> movimientoStockService.listar(null, null, -1, 10));
        assertThrows(ReglaNegocioException.class, () -> movimientoStockService.listar(null, null, 0, 0));
        assertThrows(ReglaNegocioException.class, () -> movimientoStockService.listar(null, null, 0, 51));
        verifyNoInteractions(movimientoStockRepository);
    }
}
