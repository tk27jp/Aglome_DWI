package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Variante a la que le queda poco stock. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBajoDTO {
    private UUID varianteId;
    private String sku;
    private String producto;
    private Integer stock;
}
