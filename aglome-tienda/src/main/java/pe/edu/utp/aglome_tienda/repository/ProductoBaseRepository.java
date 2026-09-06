package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.ProductoBase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductoBaseRepository extends JpaRepository<ProductoBase, UUID> {

    List<ProductoBase> findByPublicadoTrue();

    List<ProductoBase> findByCategoriaIdAndPublicadoTrue(Long categoriaId);
}