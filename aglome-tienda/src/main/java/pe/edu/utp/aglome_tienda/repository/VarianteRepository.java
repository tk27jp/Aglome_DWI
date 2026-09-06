package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VarianteRepository extends JpaRepository<Variante, UUID> {

    List<Variante> findByProductoBaseId(UUID productoBaseId);
}