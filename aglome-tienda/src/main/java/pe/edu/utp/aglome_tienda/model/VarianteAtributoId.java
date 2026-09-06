package pe.edu.utp.aglome_tienda.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VarianteAtributoId implements Serializable {
    private UUID variante;
    private UUID tipoAtributo;
}