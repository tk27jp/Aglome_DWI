package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import pe.edu.utp.aglome_tienda.dto.ProductoMasVendidoDTO;
import pe.edu.utp.aglome_tienda.dto.ResumenDashboardDTO;
import pe.edu.utp.aglome_tienda.dto.StockBajoDTO;
import pe.edu.utp.aglome_tienda.dto.VentaCategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.VentaMesDTO;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.model.Variante;
import pe.edu.utp.aglome_tienda.repository.DetallePedidoRepository;
import pe.edu.utp.aglome_tienda.repository.PedidoRepository;
import pe.edu.utp.aglome_tienda.repository.VarianteRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private DetallePedidoRepository detallePedidoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private VarianteRepository varianteRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Variante varianteConStock(String producto, String sku, int stock) {
        ProductoBase p = new ProductoBase();
        p.setNombre(producto);
        Variante v = new Variante();
        v.setId(UUID.randomUUID());
        v.setProductoBase(p);
        v.setSku(sku);
        v.setStock(stock);
        return v;
    }

    // ---------- ventas por categoría ----------

    @Test
    void ventasPorCategoria_conVentas_debeConvertirLasFilasADto() {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"Melaminas", 2L});
        when(detallePedidoRepository.unidadesVendidasPorCategoria()).thenReturn(filas);

        List<VentaCategoriaDTO> resultado = reporteService.ventasPorCategoria();

        assertEquals(1, resultado.size());
        assertEquals("Melaminas", resultado.get(0).getCategoria());
        assertEquals(2L, resultado.get(0).getUnidades());
    }

    @Test
    void ventasPorCategoria_sinVentas_debeRetornarListaVacia() {
        when(detallePedidoRepository.unidadesVendidasPorCategoria()).thenReturn(new ArrayList<>());

        assertTrue(reporteService.ventasPorCategoria().isEmpty());
    }

    // ---------- ventas por mes ----------

    @Test
    void ventasPorMes_rellenaLosMesesSinVentasConCero() {
        YearMonth actual = YearMonth.now();
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{actual.getYear(), actual.getMonthValue(), 2L, new BigDecimal("150.5")});
        when(pedidoRepository.ventasPorMes(any(LocalDateTime.class))).thenReturn(filas);

        List<VentaMesDTO> resultado = reporteService.ventasPorMes(3);

        assertEquals(3, resultado.size());
        assertEquals(actual.minusMonths(2).toString(), resultado.get(0).getMes());
        assertEquals(0L, resultado.get(0).getPedidos());
        assertEquals(new BigDecimal("0.00"), resultado.get(0).getTotal());
        assertEquals(actual.toString(), resultado.get(2).getMes());
        assertEquals(2L, resultado.get(2).getPedidos());
        assertEquals(new BigDecimal("150.50"), resultado.get(2).getTotal());
    }

    @Test
    void ventasPorMes_ignoraFilasFueraDelRango() {
        YearMonth viejo = YearMonth.now().minusMonths(10);
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{viejo.getYear(), viejo.getMonthValue(), 9L, new BigDecimal("999")});
        when(pedidoRepository.ventasPorMes(any(LocalDateTime.class))).thenReturn(filas);

        List<VentaMesDTO> resultado = reporteService.ventasPorMes(2);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(m -> m.getPedidos() == 0L));
    }

    @Test
    void ventasPorMes_conMesesInvalidos_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> reporteService.ventasPorMes(0));
        assertThrows(ReglaNegocioException.class, () -> reporteService.ventasPorMes(37));
        verifyNoInteractions(pedidoRepository);
    }

    // ---------- más vendidos ----------

    @Test
    void productosMasVendidos_convierteLasFilasYPideSoloElLimite() {
        UUID id = UUID.randomUUID();
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{id, "Melamina Azul", 12L, new BigDecimal("480")});
        when(detallePedidoRepository.productosMasVendidos(any(Pageable.class))).thenReturn(filas);

        List<ProductoMasVendidoDTO> resultado = reporteService.productosMasVendidos(3);

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.get(0).getProductoId());
        assertEquals("Melamina Azul", resultado.get(0).getNombre());
        assertEquals(12L, resultado.get(0).getUnidades());
        assertEquals(new BigDecimal("480.00"), resultado.get(0).getIngresos());
        verify(detallePedidoRepository).productosMasVendidos(org.springframework.data.domain.PageRequest.of(0, 3));
    }

    @Test
    void productosMasVendidos_conLimiteInvalido_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> reporteService.productosMasVendidos(0));
        assertThrows(ReglaNegocioException.class, () -> reporteService.productosMasVendidos(51));
        verifyNoInteractions(detallePedidoRepository);
    }

    // ---------- stock bajo ----------

    @Test
    void stockBajo_convierteVariantesADto() {
        Variante v = varianteConStock("Pintura Blanca", "PIN-1", 2);
        when(varianteRepository.stockBajo(5)).thenReturn(List.of(v));

        List<StockBajoDTO> resultado = reporteService.stockBajo(5);

        assertEquals(1, resultado.size());
        assertEquals("PIN-1", resultado.get(0).getSku());
        assertEquals("Pintura Blanca", resultado.get(0).getProducto());
        assertEquals(2, resultado.get(0).getStock());
    }

    @Test
    void stockBajo_conUmbralNegativo_lanzaReglaNegocio() {
        assertThrows(ReglaNegocioException.class, () -> reporteService.stockBajo(-1));
        verifyNoInteractions(varianteRepository);
    }

    // ---------- dashboard ----------

    @Test
    void resumenDashboard_juntaTodoYCalculaElTicketPromedio() {
        when(pedidoRepository.ingresosTotales()).thenReturn(new BigDecimal("300"));
        when(pedidoRepository.contarValidos()).thenReturn(3L);
        List<Object[]> porEstado = new ArrayList<>();
        porEstado.add(new Object[]{EstadoPedido.CONFIRMADO, 2L});
        porEstado.add(new Object[]{EstadoPedido.CANCELADO, 1L});
        when(pedidoRepository.contarPorEstado()).thenReturn(porEstado);
        when(pedidoRepository.ventasPorMes(any(LocalDateTime.class))).thenReturn(new ArrayList<>());
        when(varianteRepository.stockBajo(ReporteService.STOCK_BAJO_POR_DEFECTO))
                .thenReturn(List.of(varianteConStock("Pintura Blanca", "PIN-1", 1)));
        when(varianteRepository.contarStockBajo(ReporteService.STOCK_BAJO_POR_DEFECTO)).thenReturn(1L);
        when(detallePedidoRepository.productosMasVendidos(any(Pageable.class))).thenReturn(new ArrayList<>());

        ResumenDashboardDTO r = reporteService.resumenDashboard();

        assertEquals(new BigDecimal("300.00"), r.getIngresosTotales());
        assertEquals(3L, r.getPedidosValidos());
        assertEquals(new BigDecimal("100.00"), r.getTicketPromedio());
        assertEquals(2L, r.getPedidosPorEstado().get("CONFIRMADO"));
        assertEquals(1L, r.getPedidosPorEstado().get("CANCELADO"));
        assertEquals(0L, r.getPedidosPorEstado().get("PENDIENTE"));   // estados sin pedidos salen con 0
        assertEquals(1L, r.getVariantesConStockBajo());
        assertEquals(6, r.getVentasPorMes().size());
        assertEquals(1, r.getStockBajo().size());
        assertTrue(r.getProductosMasVendidos().isEmpty());
    }

    @Test
    void resumenDashboard_sinVentas_devuelveCerosSinFallar() {
        when(pedidoRepository.ingresosTotales()).thenReturn(null);   // SUM sin filas
        when(pedidoRepository.contarValidos()).thenReturn(0L);
        when(pedidoRepository.contarPorEstado()).thenReturn(new ArrayList<>());
        when(pedidoRepository.ventasPorMes(any(LocalDateTime.class))).thenReturn(new ArrayList<>());
        when(varianteRepository.stockBajo(anyInt())).thenReturn(new ArrayList<>());
        when(varianteRepository.contarStockBajo(anyInt())).thenReturn(0L);
        when(detallePedidoRepository.productosMasVendidos(any(Pageable.class))).thenReturn(new ArrayList<>());

        ResumenDashboardDTO r = reporteService.resumenDashboard();

        assertEquals(new BigDecimal("0.00"), r.getIngresosTotales());
        assertEquals(new BigDecimal("0.00"), r.getTicketPromedio());
        assertEquals(0L, r.getPedidosValidos());
        assertTrue(r.getPedidosPorEstado().values().stream().allMatch(n -> n == 0L));
    }
}
