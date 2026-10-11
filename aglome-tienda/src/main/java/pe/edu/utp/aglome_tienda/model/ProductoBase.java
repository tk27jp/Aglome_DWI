package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "producto_base")
@Data
@NoArgsConstructor

public class ProductoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    private String marca;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    private Boolean publicado = false; // solo se muestra en la tienda si está en true

    public ProductoBase(UUID id, String nombre, String descripcion, String marca,
                        Categoria categoria, Boolean publicado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.categoria = categoria;
        this.publicado = publicado;
    }
}