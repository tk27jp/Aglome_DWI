package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarcaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;

    @Size(max = 500, message = "La URL de la imagen no puede pasar de 500 caracteres")
    private String imagenUrl;
}
