package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    @Query("SELECT p FROM Pedido p " +
           "WHERE p.usuario.id = :usuarioId AND p.estado = :estado " +
           "ORDER BY p.fecha DESC")
    List<Pedido> buscarPorUsuarioYEstado(@Param("usuarioId") Long usuarioId,
                                         @Param("estado") EstadoPedido estado);
}