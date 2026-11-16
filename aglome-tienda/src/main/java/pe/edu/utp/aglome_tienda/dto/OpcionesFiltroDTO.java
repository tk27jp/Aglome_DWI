package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Lo que el frontend necesita para dibujar el panel de filtros (solo opciones que existen de verdad). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcionesFiltroDTO {
    private List<CategoriaDTO> categorias;
    private List<MarcaDTO> marcas;
    private List<String> colores;
    private List<String> medidas;
    private BigDecimal precioMinimo;
    private BigDecimal precioMaximo;
}
