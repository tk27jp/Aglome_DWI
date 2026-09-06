package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.VarianteAtributo;
import pe.edu.utp.aglome_tienda.model.VarianteAtributoId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VarianteAtributoRepository extends JpaRepository<VarianteAtributo, VarianteAtributoId> {

    List<VarianteAtributo> findByVarianteId(UUID varianteId);
}