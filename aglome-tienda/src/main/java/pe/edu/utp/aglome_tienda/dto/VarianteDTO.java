package pe.edu.utp.aglome_tienda.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VarianteDTO {
    private UUID id;
    private String sku;
    private BigDecimal precioVenta;
    private Integer stock;
    private Boolean activa;
    private Map<String, String> atributos; // ej: {"Color": "Rojo", "Medida": "1 Galón"}
}