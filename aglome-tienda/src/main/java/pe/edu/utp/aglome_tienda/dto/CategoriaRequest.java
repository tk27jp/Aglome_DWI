package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private Long padreId;

    @Size(max = 500, message = "La URL de la imagen no puede pasar de 500 caracteres")
    private String imagenUrl;
}