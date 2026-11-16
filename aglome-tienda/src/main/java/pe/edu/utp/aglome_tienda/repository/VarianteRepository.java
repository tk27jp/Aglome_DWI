package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VarianteRepository extends JpaRepository<Variante, UUID> {

    List<Variante> findByProductoBaseIdAndEliminadoFalse(UUID productoBaseId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    // rango de precios de lo que hoy se puede comprar (para el slider de precio)
    @Query("SELECT MIN(v.precioVenta) FROM Variante v " +
           "WHERE v.eliminado = false AND v.activa = true " +
           "AND v.productoBase.publicado = true AND v.productoBase.eliminado = false")
    BigDecimal precioMinimoDisponible();

    @Query("SELECT MAX(v.precioVenta) FROM Variante v " +
           "WHERE v.eliminado = false AND v.activa = true " +
           "AND v.productoBase.publicado = true AND v.productoBase.eliminado = false")
    BigDecimal precioMaximoDisponible();

    // variantes activas con poco stock, las más urgentes primero
    @Query("""
            SELECT v FROM Variante v JOIN FETCH v.productoBase p
            WHERE v.eliminado = false AND v.activa = true AND p.eliminado = false
              AND v.stock <= :umbral
            ORDER BY v.stock ASC, v.sku ASC
            """)
    List<Variante> stockBajo(@Param("umbral") int umbral);

    @Query("""
            SELECT COUNT(v) FROM Variante v
            WHERE v.eliminado = false AND v.activa = true AND v.productoBase.eliminado = false
              AND v.stock <= :umbral
            """)
    long contarStockBajo(@Param("umbral") int umbral);
}
