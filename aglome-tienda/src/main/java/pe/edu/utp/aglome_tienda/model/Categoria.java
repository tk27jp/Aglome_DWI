package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categoria")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Categoria extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "padre_id")
    private Categoria padre; // permite subcategorías, opcional

    private Boolean activa = true;
    
    @Column(length = 500)
    private String imagenUrl;

    // borrado lógico: el registro no se borra, solo se marca y deja de mostrarse
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean eliminado = false;

    public Categoria(Long id, String nombre, Categoria padre, Boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.padre = padre;
        this.activa = activa;
    }
}