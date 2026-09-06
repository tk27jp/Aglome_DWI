package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, UUID> {

    List<CarritoItem> findByCarritoId(UUID carritoId);

    Optional<CarritoItem> findByCarritoIdAndVarianteId(UUID carritoId, UUID varianteId);

    void deleteByCarritoId(UUID carritoId);
}