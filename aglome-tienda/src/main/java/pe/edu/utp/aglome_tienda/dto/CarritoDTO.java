package pe.edu.utp.aglome_tienda.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarritoDTO {
    private UUID id;
    private List<CarritoItemDTO> items;
    private BigDecimal total;
}