package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.aglome_tienda.dto.VarianteDTO;
import pe.edu.utp.aglome_tienda.dto.VarianteRequest;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VarianteService {

    private final VarianteRepository varianteRepository;
    private final ProductoBaseRepository productoBaseRepository;
    private final TipoAtributoRepository tipoAtributoRepository;
    private final VarianteAtributoRepository varianteAtributoRepository;
    private final MovimientoStockService movimientoStockService;

    // ── lecturas ─────────────────────────────────────────────────────
    public List<VarianteDTO> listarPorProducto(UUID productoBaseId) {
        return varianteRepository.findByProductoBaseIdAndEliminadoFalse(productoBaseId)
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    // ── escrituras: solo panel admin ────────────────────────────────
    public VarianteDTO crear(VarianteRequest request) {
        if (varianteRepository.existsBySku(request.getSku())) {
            throw new ConflictoException("Ya existe una variante con ese SKU");
        }

        ProductoBase productoBase = productoBaseRepository.findById(request.getProductoBaseId())
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto base no encontrado"));

        Variante variante = new Variante();
        variante.setProductoBase(productoBase);
        variante.setSku(request.getSku());
        variante.setPrecioVenta(request.getPrecioVenta());
        variante.setStock(request.getStock());
        variante.setImagenUrl(request.getImagenUrl());
        variante.setActiva(true);

        Variante guardada = varianteRepository.save(variante);

        if (guardada.getStock() > 0) {
            movimientoStockService.registrar(guardada, TipoMovimiento.STOCK_INICIAL,
                    0, guardada.getStock(), "Stock inicial al crear la variante", null);
        }

        guardarAtributos(guardada, request.getAtributos());

        return mapearADto(guardada);
    }

    public VarianteDTO actualizar(UUID id, VarianteRequest request) {
        Variante variante = buscarOFallar(id);
        if (varianteRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new ConflictoException("Ya existe otra variante con ese SKU");
        }
        int stockAnterior = variante.getStock();
        variante.setSku(request.getSku());
        variante.setPrecioVenta(request.getPrecioVenta());
        variante.setStock(request.getStock());
        variante.setImagenUrl(request.getImagenUrl());

        Variante actualizada = varianteRepository.save(variante);

        if (stockAnterior != request.getStock()) {
            movimientoStockService.registrar(actualizada, TipoMovimiento.AJUSTE,
                    stockAnterior, request.getStock(), "Stock editado al actualizar la variante", null);
        }

        // reemplaza los atributos existentes por los nuevos que mandó el admin
        varianteAtributoRepository.findByVarianteId(id)
                .forEach(varianteAtributoRepository::delete);
        guardarAtributos(actualizada, request.getAtributos());

        return mapearADto(actualizada);
    }

    // borrado lógico: la variante se oculta pero los pedidos que la compraron siguen mostrándola
    public void eliminar(UUID id) {
        Variante variante = buscarOFallar(id);
        variante.setEliminado(true);
        varianteRepository.save(variante);
    }

    // se usa al eliminar un producto: oculta todas sus variantes
    public void eliminarPorProducto(UUID productoBaseId) {
        List<Variante> variantes = varianteRepository.findByProductoBaseIdAndEliminadoFalse(productoBaseId);
        variantes.forEach(v -> v.setEliminado(true));
        varianteRepository.saveAll(variantes);
    }

    // ── helpers privados ─────────────────────────────────────────────
    private Variante buscarOFallar(UUID id) {
        return varianteRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getEliminado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante no encontrada"));
    }

    private void guardarAtributos(Variante variante, Map<String, String> atributos) {
        if (atributos == null) return;

        for (Map.Entry<String, String> entry : atributos.entrySet()) {
            TipoAtributo tipo = tipoAtributoRepository.findAll().stream()
                    .filter(t -> t.getNombre().equalsIgnoreCase(entry.getKey()))
                    .findFirst()
                    .orElseGet(() -> {
                        TipoAtributo nuevo = new TipoAtributo();
                        nuevo.setNombre(entry.getKey());
                        return tipoAtributoRepository.save(nuevo);
                    });

            VarianteAtributo va = new VarianteAtributo();
            va.setVariante(variante);
            va.setTipoAtributo(tipo);
            va.setValor(entry.getValue());
            varianteAtributoRepository.save(va);
        }
    }

    VarianteDTO mapearADto(Variante v) {
        Map<String, String> atributos = new LinkedHashMap<>();
        varianteAtributoRepository.findByVarianteId(v.getId())
                .forEach(va -> atributos.put(va.getTipoAtributo().getNombre(), va.getValor()));

          return new VarianteDTO(
          v.getId(), v.getSku(), v.getPrecioVenta(), v.getStock(), v.getActiva(), atributos, v.getImagenUrl()
        );
    }
}