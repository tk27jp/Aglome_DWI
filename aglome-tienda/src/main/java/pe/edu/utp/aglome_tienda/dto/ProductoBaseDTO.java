package pe.edu.utp.aglome_tienda.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoBaseDTO {
    private UUID id;
    private String nombre;
    private String descripcion;
    private String marca;
    private Long categoriaId;
    private String categoriaNombre;
    private Boolean publicado;
    private List<VarianteDTO> variantes;
}