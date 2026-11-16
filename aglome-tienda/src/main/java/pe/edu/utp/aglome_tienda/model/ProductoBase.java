package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "producto_base")
@Data
@NoArgsConstructor

@EqualsAndHashCode(callSuper = false)
public class ProductoBase extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "marca_id")
    private Marca marca; // opcional

    @Column(length = 500)
    private String imagenUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    private Boolean publicado = false; // solo se muestra en la tienda si está en true

    // borrado lógico: el registro no se borra, solo se marca y deja de mostrarse
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean eliminado = false;

    public ProductoBase(UUID id, String nombre, String descripcion, Marca marca,
                        Categoria categoria, Boolean publicado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.marca = marca;
        this.categoria = categoria;
        this.publicado = publicado;
    }
}