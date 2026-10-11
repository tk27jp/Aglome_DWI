package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaCategoriaDTO {
    private String categoria;
    private Long unidades;
}