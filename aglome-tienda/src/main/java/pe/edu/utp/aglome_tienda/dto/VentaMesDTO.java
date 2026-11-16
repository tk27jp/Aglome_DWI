package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Ventas de un mes (sin contar pedidos cancelados). El mes va como "2026-11". */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaMesDTO {
    private String mes;
    private Long pedidos;
    private BigDecimal total;
}
