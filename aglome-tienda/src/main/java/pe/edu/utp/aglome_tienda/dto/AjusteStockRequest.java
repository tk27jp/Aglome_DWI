package pe.edu.utp.aglome_tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class AjusteStockRequest {

    @NotNull(message = "La variante es obligatoria")
    private UUID varianteId;

    // positivo = entra mercadería (reposición); negativo = sale (merma, rotura, conteo físico)
    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 255, message = "El motivo no puede pasar de 255 caracteres")
    private String motivo;
}
