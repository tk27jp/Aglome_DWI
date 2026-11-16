package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByEliminadoFalse();

    boolean existsByPadreIdAndEliminadoFalse(Long padreId);

    // solo las categorías que tienen al menos un producto visible en la tienda (para el panel de filtros)
    @Query("SELECT DISTINCT c FROM ProductoBase p JOIN p.categoria c " +
           "WHERE p.publicado = true AND p.eliminado = false AND c.eliminado = false ORDER BY c.nombre")
    List<Categoria> categoriasConProductosVisibles();
}
