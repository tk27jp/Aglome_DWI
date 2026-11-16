package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.VarianteAtributo;
import pe.edu.utp.aglome_tienda.model.VarianteAtributoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface VarianteAtributoRepository extends JpaRepository<VarianteAtributo, VarianteAtributoId> {

    List<VarianteAtributo> findByVarianteId(UUID varianteId);

    // valores distintos de un atributo (ej. "Color") entre las variantes que hoy se pueden comprar
    @Query("SELECT DISTINCT va.valor FROM VarianteAtributo va " +
           "WHERE LOWER(va.tipoAtributo.nombre) = LOWER(:tipo) " +
           "AND va.variante.eliminado = false AND va.variante.activa = true " +
           "AND va.variante.productoBase.publicado = true AND va.variante.productoBase.eliminado = false " +
           "ORDER BY va.valor")
    List<String> valoresDisponibles(@Param("tipo") String tipo);
}
