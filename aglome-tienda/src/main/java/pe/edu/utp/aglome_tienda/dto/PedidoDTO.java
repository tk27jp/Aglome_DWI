package pe.edu.utp.aglome_tienda.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private UUID id;
    private LocalDateTime fecha;
    private String estado;
    private BigDecimal total;
    private List<DetallePedidoDTO> detalles;
}