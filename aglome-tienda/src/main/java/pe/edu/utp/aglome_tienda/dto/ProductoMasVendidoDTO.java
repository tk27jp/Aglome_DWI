package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** Producto con más unidades vendidas (sin contar pedidos cancelados). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMasVendidoDTO {
    private UUID productoId;
    private String nombre;
    private Long unidades;
    private BigDecimal ingresos;
}
