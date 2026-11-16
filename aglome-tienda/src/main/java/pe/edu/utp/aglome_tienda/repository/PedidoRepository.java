package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    @Query("SELECT p FROM Pedido p " +
           "WHERE p.usuario.id = :usuarioId AND p.estado = :estado " +
           "ORDER BY p.fecha DESC")
    List<Pedido> buscarPorUsuarioYEstado(@Param("usuarioId") Long usuarioId,
                                         @Param("estado") EstadoPedido estado);

    // panel admin: más recientes primero, con o sin filtro de estado
    List<Pedido> findAllByOrderByFechaDesc();

    List<Pedido> findByEstadoOrderByFechaDesc(EstadoPedido estado);

    // ---------- Reportes (los pedidos cancelados no cuentan como venta) ----------

    @Query("SELECT SUM(p.total) FROM Pedido p " +
           "WHERE p.estado <> pe.edu.utp.aglome_tienda.model.EstadoPedido.CANCELADO")
    BigDecimal ingresosTotales();   // null si todavía no hay ventas

    @Query("SELECT COUNT(p) FROM Pedido p " +
           "WHERE p.estado <> pe.edu.utp.aglome_tienda.model.EstadoPedido.CANCELADO")
    long contarValidos();

    // [estado, cantidad]
    @Query("SELECT p.estado, COUNT(p) FROM Pedido p GROUP BY p.estado")
    List<Object[]> contarPorEstado();

    // [año, mes, pedidos, total] desde una fecha, ordenado cronológicamente
    @Query("""
            SELECT YEAR(p.fecha), MONTH(p.fecha), COUNT(p), SUM(p.total)
            FROM Pedido p
            WHERE p.estado <> pe.edu.utp.aglome_tienda.model.EstadoPedido.CANCELADO
              AND p.fecha >= :desde
            GROUP BY YEAR(p.fecha), MONTH(p.fecha)
            ORDER BY YEAR(p.fecha), MONTH(p.fecha)
            """)
    List<Object[]> ventasPorMes(@Param("desde") LocalDateTime desde);
}
