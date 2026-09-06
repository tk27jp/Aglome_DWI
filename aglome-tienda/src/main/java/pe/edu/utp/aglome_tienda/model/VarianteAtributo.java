package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "variante_atributo")
@IdClass(VarianteAtributoId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VarianteAtributo {

    @Id
    @ManyToOne
    @JoinColumn(name = "variante_id")
    private Variante variante;

    @Id
    @ManyToOne
    @JoinColumn(name = "tipo_atributo_id")
    private TipoAtributo tipoAtributo;

    @Column(nullable = false, length = 120)
    private String valor; // ej: "Rojo", "1 Galón"
}