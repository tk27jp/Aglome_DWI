package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.aglome_tienda.dto.AjusteStockRequest;
import pe.edu.utp.aglome_tienda.dto.MovimientoStockDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.MovimientoStock;
import pe.edu.utp.aglome_tienda.model.TipoMovimiento;
import pe.edu.utp.aglome_tienda.model.Variante;
import pe.edu.utp.aglome_tienda.repository.MovimientoStockRepository;
import pe.edu.utp.aglome_tienda.repository.VarianteRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historial de stock. Los demás servicios (compras, cancelaciones, edición de variantes)
 * llaman a {@link #registrar} dentro de su misma transacción: si la operación falla y se
 * deshace, el movimiento también se deshace.
 */
@Service
@RequiredArgsConstructor
public class MovimientoStockService {

    private static final int TAMANO_MAXIMO = 50;

    private final MovimientoStockRepository movimientoStockRepository;
    private final VarianteRepository varianteRepository;

    /** Anota un cambio de stock que el llamador ya aplicó (o va a aplicar) a la variante. */
    @Transactional
    public void registrar(Variante variante, TipoMovimiento tipo, int stockAnterior, int stockNuevo,
                          String motivo, UUID pedidoId) {
        MovimientoStock m = new MovimientoStock();
        m.setVariante(variante);
        m.setTipo(tipo);
        m.setCantidad(stockNuevo - stockAnterior);
        m.setStockAnterior(stockAnterior);
        m.setStockResultante(stockNuevo);
        m.setMotivo(motivo);
        m.setPedidoId(pedidoId);
        m.setUsuarioEmail(usuarioActual());
        m.setFecha(LocalDateTime.now());
        movimientoStockRepository.save(m);
    }

    /** Corrección manual del admin: suma o resta unidades y deja constancia del motivo. */
    @Transactional
    public MovimientoStockDTO ajustar(AjusteStockRequest request) {
        if (request.getCantidad() == 0) {
            throw new ReglaNegocioException("La cantidad del ajuste no puede ser 0");
        }
        Variante variante = varianteRepository.findById(request.getVarianteId())
                .filter(v -> !Boolean.TRUE.equals(v.getEliminado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante no encontrada"));

        int anterior = variante.getStock();
        int nuevo = anterior + request.getCantidad();
        if (nuevo < 0) {
            throw new ReglaNegocioException("El ajuste dejaría el stock en negativo. Stock actual: " + anterior);
        }

        variante.setStock(nuevo);
        varianteRepository.save(variante);

        MovimientoStock m = new MovimientoStock();
        m.setVariante(variante);
        m.setTipo(TipoMovimiento.AJUSTE);
        m.setCantidad(request.getCantidad());
        m.setStockAnterior(anterior);
        m.setStockResultante(nuevo);
        m.setMotivo(request.getMotivo().trim());
        m.setUsuarioEmail(usuarioActual());
        m.setFecha(LocalDateTime.now());
        return aDto(movimientoStockRepository.save(m));
    }

    /** Historial paginado, lo más reciente primero. Filtros opcionales: variante y tipo. */
    @Transactional(readOnly = true)
    public PaginaDTO<MovimientoStockDTO> listar(UUID varianteId, TipoMovimiento tipo, int pagina, int tamano) {
        if (pagina < 0) {
            throw new ReglaNegocioException("La página no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        if (varianteId != null && !varianteRepository.existsById(varianteId)) {
            throw new RecursoNoEncontradoException("Variante no encontrada");
        }

        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fecha"));
        Page<MovimientoStock> resultado;
        if (varianteId != null && tipo != null) {
            resultado = movimientoStockRepository.findByVarianteIdAndTipo(varianteId, tipo, pageable);
        } else if (varianteId != null) {
            resultado = movimientoStockRepository.findByVarianteId(varianteId, pageable);
        } else if (tipo != null) {
            resultado = movimientoStockRepository.findByTipo(tipo, pageable);
        } else {
            resultado = movimientoStockRepository.findAll(pageable);
        }
        return PaginaDTO.desde(resultado.map(this::aDto));
    }

    // ── helpers privados ─────────────────────────────────────────────
    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private MovimientoStockDTO aDto(MovimientoStock m) {
        Variante v = m.getVariante();
        return new MovimientoStockDTO(m.getId(), v.getId(), v.getSku(), v.getProductoBase().getNombre(),
                m.getTipo().name(), m.getCantidad(), m.getStockAnterior(), m.getStockResultante(),
                m.getMotivo(), m.getPedidoId(), m.getUsuarioEmail(), m.getFecha());
    }
}
