package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Todo lo que necesita la pantalla principal del administrador en una sola llamada.
 * Ingresos, ticket promedio, ventas por mes y más vendidos NO cuentan pedidos cancelados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenDashboardDTO {
    private BigDecimal ingresosTotales;
    private Long pedidosValidos;                 // todos menos cancelados
    private BigDecimal ticketPromedio;           // ingresos / pedidos válidos
    private Map<String, Long> pedidosPorEstado;  // incluye los estados con 0
    private Long variantesConStockBajo;
    private List<VentaMesDTO> ventasPorMes;
    private List<ProductoMasVendidoDTO> productosMasVendidos;
    private List<StockBajoDTO> stockBajo;
}
