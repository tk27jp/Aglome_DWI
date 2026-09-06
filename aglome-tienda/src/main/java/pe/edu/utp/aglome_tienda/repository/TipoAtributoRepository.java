package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.TipoAtributo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TipoAtributoRepository extends JpaRepository<TipoAtributo, UUID> {
}