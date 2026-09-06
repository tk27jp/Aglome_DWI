package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}