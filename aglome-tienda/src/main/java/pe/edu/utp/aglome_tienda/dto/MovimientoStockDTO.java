package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockDTO {
    private UUID id;
    private UUID varianteId;
    private String sku;
    private String producto;
    private String tipo;
    private Integer cantidad;         // con signo
    private Integer stockAnterior;
    private Integer stockResultante;
    private String motivo;
    private UUID pedidoId;
    private String usuario;
    private LocalDateTime fecha;
}
