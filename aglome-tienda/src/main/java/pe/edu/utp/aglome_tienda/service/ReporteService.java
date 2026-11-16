package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.ProductoMasVendidoDTO;
import pe.edu.utp.aglome_tienda.dto.ResumenDashboardDTO;
import pe.edu.utp.aglome_tienda.dto.StockBajoDTO;
import pe.edu.utp.aglome_tienda.dto.VentaCategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.VentaMesDTO;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.model.Variante;
import pe.edu.utp.aglome_tienda.repository.DetallePedidoRepository;
import pe.edu.utp.aglome_tienda.repository.PedidoRepository;
import pe.edu.utp.aglome_tienda.repository.VarianteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reportes del administrador. Regla común: los pedidos CANCELADOS no cuentan como venta.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    public static final int STOCK_BAJO_POR_DEFECTO = 5;
    public static final int MESES_POR_DEFECTO = 12;
    public static final int LIMITE_POR_DEFECTO = 5;
    private static final int MAX_MESES = 36;
    private static final int MAX_LIMITE = 50;
    private static final int MESES_DEL_DASHBOARD = 6;
    private static final int STOCK_BAJO_EN_DASHBOARD = 10;

    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final VarianteRepository varianteRepository;

    public List<VentaCategoriaDTO> ventasPorCategoria() {
        return detallePedidoRepository.unidadesVendidasPorCategoria()
                .stream()
                .map(fila -> new VentaCategoriaDTO((String) fila[0], ((Number) fila[1]).longValue()))
                .collect(Collectors.toList());
    }

    /** Ventas de los últimos N meses; los meses sin ventas salen con 0 para que el gráfico no tenga huecos. */
    public List<VentaMesDTO> ventasPorMes(int meses) {
        if (meses < 1 || meses > MAX_MESES) {
            throw new ReglaNegocioException("Los meses deben estar entre 1 y " + MAX_MESES);
        }
        YearMonth actual = YearMonth.now();
        YearMonth primero = actual.minusMonths(meses - 1L);

        Map<YearMonth, VentaMesDTO> porMes = new LinkedHashMap<>();
        for (int i = 0; i < meses; i++) {
            YearMonth ym = primero.plusMonths(i);
            porMes.put(ym, new VentaMesDTO(ym.toString(), 0L, BigDecimal.ZERO.setScale(2)));
        }

        List<Object[]> filas = pedidoRepository.ventasPorMes(primero.atDay(1).atStartOfDay());
        for (Object[] fila : filas) {
            YearMonth ym = YearMonth.of(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue());
            if (porMes.containsKey(ym)) {
                porMes.put(ym, new VentaMesDTO(ym.toString(),
                        ((Number) fila[2]).longValue(), aDinero(fila[3])));
            }
        }
        return new ArrayList<>(porMes.values());
    }

    public List<ProductoMasVendidoDTO> productosMasVendidos(int limite) {
        if (limite < 1 || limite > MAX_LIMITE) {
            throw new ReglaNegocioException("El límite debe estar entre 1 y " + MAX_LIMITE);
        }
        return detallePedidoRepository.productosMasVendidos(PageRequest.of(0, limite))
                .stream()
                .map(f -> new ProductoMasVendidoDTO((java.util.UUID) f[0], (String) f[1],
                        ((Number) f[2]).longValue(), aDinero(f[3])))
                .collect(Collectors.toList());
    }

    public List<StockBajoDTO> stockBajo(int umbral) {
        validarUmbral(umbral);
        return varianteRepository.stockBajo(umbral).stream()
                .map(this::aStockBajoDto)
                .collect(Collectors.toList());
    }

    public ResumenDashboardDTO resumenDashboard() {
        BigDecimal ingresos = aDinero(pedidoRepository.ingresosTotales());
        long validos = pedidoRepository.contarValidos();
        BigDecimal ticket = validos == 0
                ? BigDecimal.ZERO.setScale(2)
                : ingresos.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP);

        List<StockBajoDTO> stockBajo = varianteRepository.stockBajo(STOCK_BAJO_POR_DEFECTO).stream()
                .limit(STOCK_BAJO_EN_DASHBOARD)
                .map(this::aStockBajoDto)
                .collect(Collectors.toList());

        return new ResumenDashboardDTO(
                ingresos,
                validos,
                ticket,
                pedidosPorEstado(),
                varianteRepository.contarStockBajo(STOCK_BAJO_POR_DEFECTO),
                ventasPorMes(MESES_DEL_DASHBOARD),
                productosMasVendidos(LIMITE_POR_DEFECTO),
                stockBajo);
    }

    // ---------- auxiliares ----------

    private Map<String, Long> pedidosPorEstado() {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (EstadoPedido estado : EstadoPedido.values()) {
            resultado.put(estado.name(), 0L);
        }
        for (Object[] fila : pedidoRepository.contarPorEstado()) {
            resultado.put(((EstadoPedido) fila[0]).name(), ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    private StockBajoDTO aStockBajoDto(Variante v) {
        return new StockBajoDTO(v.getId(), v.getSku(), v.getProductoBase().getNombre(), v.getStock());
    }

    private void validarUmbral(int umbral) {
        if (umbral < 0) {
            throw new ReglaNegocioException("El umbral de stock no puede ser negativo");
        }
    }

    private BigDecimal aDinero(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal b = valor instanceof BigDecimal bd ? bd : new BigDecimal(valor.toString());
        return b.setScale(2, RoundingMode.HALF_UP);
    }
}
