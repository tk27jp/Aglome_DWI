package pe.edu.utp.aglome_tienda.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.aglome_tienda.model.ProductoBase;

import java.util.List;
import java.util.UUID;

/** Todas las consultas ignoran los productos con borrado lógico (eliminado = true). */
public interface ProductoBaseRepository extends JpaRepository<ProductoBase, UUID>, JpaSpecificationExecutor<ProductoBase> {

    List<ProductoBase> findByEliminadoFalse();

    boolean existsByMarcaIdAndEliminadoFalse(Long marcaId);

    boolean existsByCategoriaIdAndEliminadoFalse(Long categoriaId);

    // otros productos de la misma categoría (para "te puede interesar")
    List<ProductoBase> findByCategoriaIdAndPublicadoTrueAndEliminadoFalseAndIdNot(Long categoriaId, UUID id, Pageable pageable);

    // nombres para el autocompletado del buscador
    @Query("SELECT DISTINCT p.nombre FROM ProductoBase p " +
           "WHERE p.publicado = true AND p.eliminado = false " +
           "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "ORDER BY p.nombre")
    List<String> sugerenciasPorNombre(@Param("texto") String texto, Pageable pageable);

    Page<ProductoBase> findByPublicadoTrueAndEliminadoFalse(Pageable pageable);

    Page<ProductoBase> findByCategoriaIdAndPublicadoTrueAndEliminadoFalse(Long categoriaId, Pageable pageable);

    // JPQL: busca productos publicados cuyo nombre contenga el texto (sin importar mayúsculas)
    @Query("SELECT p FROM ProductoBase p " +
           "WHERE p.publicado = true AND p.eliminado = false " +
           "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<ProductoBase> buscarPublicadosPorNombre(@Param("texto") String texto);

    // misma búsqueda, paginada y ordenada (la usa la tienda).
    // LEFT JOIN con alias "marca": así ordenar por "marca.nombre" no descarta los productos sin marca
    @Query(value = "SELECT p FROM ProductoBase p LEFT JOIN p.marca marca " +
                   "WHERE p.publicado = true AND p.eliminado = false " +
                   "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))",
           countQuery = "SELECT COUNT(p) FROM ProductoBase p " +
                        "WHERE p.publicado = true AND p.eliminado = false " +
                        "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))")
    Page<ProductoBase> buscarPublicadosPorNombre(@Param("texto") String texto, Pageable pageable);
}
