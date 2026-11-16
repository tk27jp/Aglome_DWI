package pe.edu.utp.aglome_tienda.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.aglome_tienda.model.DetallePedido;

import java.util.List;
import java.util.UUID;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, UUID> {

    List<DetallePedido> findByPedidoId(UUID pedidoId);

    // JPQL con JOIN, SUM y GROUP BY: unidades vendidas por categoría (los pedidos cancelados no cuentan)
    @Query("""
            SELECT c.nombre, SUM(d.cantidad)
            FROM DetallePedido d
            JOIN d.pedido ped
            JOIN d.variante v
            JOIN v.productoBase p
            JOIN p.categoria c
            WHERE ped.estado <> pe.edu.utp.aglome_tienda.model.EstadoPedido.CANCELADO
            GROUP BY c.nombre
            ORDER BY SUM(d.cantidad) DESC
            """)
    List<Object[]> unidadesVendidasPorCategoria();

    // productos más vendidos: [id, nombre, unidades, ingresos]; el Pageable limita cuántos devolver
    @Query("""
            SELECT p.id, p.nombre, SUM(d.cantidad), SUM(d.subtotal)
            FROM DetallePedido d
            JOIN d.pedido ped
            JOIN d.variante v
            JOIN v.productoBase p
            WHERE ped.estado <> pe.edu.utp.aglome_tienda.model.EstadoPedido.CANCELADO
            GROUP BY p.id, p.nombre
            ORDER BY SUM(d.cantidad) DESC, p.nombre ASC
            """)
    List<Object[]> productosMasVendidos(Pageable pageable);
}
