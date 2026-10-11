package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categoria")
@Data
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "padre_id")
    private Categoria padre; // permite subcategorías, opcional

    private Boolean activa = true;

    public Categoria(Long id, String nombre, Categoria padre, Boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.padre = padre;
        this.activa = activa;
    }
}