package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CarritoRepository extends JpaRepository<Carrito, UUID> {

    Optional<Carrito> findByUsuarioId(Long usuarioId);
}