package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaArbolDTO {
    private Long id;
    private String nombre;
    private String imagenUrl;
    private List<CategoriaArbolDTO> hijas;
}
