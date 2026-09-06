package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private Long padreId; // opcional, para subcategorías
}