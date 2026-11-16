package pe.edu.utp.aglome_tienda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.aglome_tienda.model.Marca;

import java.util.List;

public interface MarcaRepository extends JpaRepository<Marca, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    // solo las marcas que tienen al menos un producto visible en la tienda (para el panel de filtros)
    @Query("SELECT DISTINCT m FROM ProductoBase p JOIN p.marca m " +
           "WHERE p.publicado = true AND p.eliminado = false ORDER BY m.nombre")
    List<Marca> marcasConProductosVisibles();
}
