package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, UUID> {

    List<DetallePedido> findByPedidoId(UUID pedidoId);
}