package pe.edu.utp.aglome_tienda.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.aglome_tienda.model.MovimientoStock;
import pe.edu.utp.aglome_tienda.model.TipoMovimiento;

import java.util.UUID;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, UUID> {

    Page<MovimientoStock> findByVarianteId(UUID varianteId, Pageable pageable);

    Page<MovimientoStock> findByTipo(TipoMovimiento tipo, Pageable pageable);

    Page<MovimientoStock> findByVarianteIdAndTipo(UUID varianteId, TipoMovimiento tipo, Pageable pageable);
}
