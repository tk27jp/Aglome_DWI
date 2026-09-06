package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class VarianteRequest {

    @NotNull(message = "El producto base es obligatorio")
    private UUID productoBaseId;

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal precioVenta;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    // ej: {"Color": "Rojo", "Medida": "1 Galón"} — el service busca o crea el TipoAtributo por nombre
    private Map<String, String> atributos;
}