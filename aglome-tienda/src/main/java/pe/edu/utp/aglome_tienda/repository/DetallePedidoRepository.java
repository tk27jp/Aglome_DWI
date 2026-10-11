package pe.edu.utp.aglome_tienda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.aglome_tienda.model.DetallePedido;

import java.util.List;
import java.util.UUID;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, UUID> {

    List<DetallePedido> findByPedidoId(UUID pedidoId);

    // JPQL con JOIN, SUM y GROUP BY: unidades vendidas por categoría
    @Query("""
            SELECT c.nombre, SUM(d.cantidad)
            FROM DetallePedido d
            JOIN d.variante v
            JOIN v.productoBase p
            JOIN p.categoria c
            GROUP BY c.nombre
            ORDER BY SUM(d.cantidad) DESC
            """)
    List<Object[]> unidadesVendidasPorCategoria();
}