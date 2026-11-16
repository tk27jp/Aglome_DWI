package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltroProductoRequest {
    private String texto;
    private Long categoriaId;
    private Long marcaId;
    private String color;
    private String medida;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean soloDisponibles;

    public FiltroProductoRequest(String texto, Long categoriaId, Long marcaId, String color, String medida,
                                 BigDecimal precioMin, BigDecimal precioMax) {
        this(texto, categoriaId, marcaId, color, medida, precioMin, precioMax, null);
    }
}
