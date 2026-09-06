package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductoBaseRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    private String marca;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    private Boolean publicado = false;
}