package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "variante")
@Data
@NoArgsConstructor
public class Variante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_base_id", nullable = false)
    private ProductoBase productoBase;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private Integer stock = 0;

    private Boolean activa = true;

    public Variante(UUID id, ProductoBase productoBase, String sku, BigDecimal precioVenta,
                    Integer stock, Boolean activa) {
        this.id = id;
        this.productoBase = productoBase;
        this.sku = sku;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.activa = activa;
    }
}