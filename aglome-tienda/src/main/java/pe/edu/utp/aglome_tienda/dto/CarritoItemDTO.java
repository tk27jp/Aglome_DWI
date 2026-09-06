package pe.edu.utp.aglome_tienda.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarritoItemDTO {
    private UUID id;
    private UUID varianteId;
    private String productoNombre;
    private String sku;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
}