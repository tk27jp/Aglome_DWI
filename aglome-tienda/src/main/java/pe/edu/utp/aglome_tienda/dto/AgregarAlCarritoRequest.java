package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AgregarAlCarritoRequest {

    @NotNull(message = "La variante es obligatoria")
    private UUID varianteId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
}