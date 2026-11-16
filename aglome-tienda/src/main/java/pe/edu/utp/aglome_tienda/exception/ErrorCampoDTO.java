package pe.edu.utp.aglome_tienda.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCampoDTO {
    private String campo;
    private String mensaje;
}