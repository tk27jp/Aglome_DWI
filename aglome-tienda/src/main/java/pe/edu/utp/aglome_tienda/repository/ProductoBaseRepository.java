package pe.edu.utp.aglome_tienda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.aglome_tienda.model.ProductoBase;

import java.util.List;
import java.util.UUID;

public interface ProductoBaseRepository extends JpaRepository<ProductoBase, UUID> {

    List<ProductoBase> findByPublicadoTrue();

    List<ProductoBase> findByCategoriaIdAndPublicadoTrue(Long categoriaId);

    // JPQL: busca productos publicados cuyo nombre contenga el texto (sin importar mayúsculas)
    @Query("SELECT p FROM ProductoBase p " +
           "WHERE p.publicado = true " +
           "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<ProductoBase> buscarPublicadosPorNombre(@Param("texto") String texto);
}