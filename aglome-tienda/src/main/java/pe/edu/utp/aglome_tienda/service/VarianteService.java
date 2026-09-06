package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.VarianteDTO;
import pe.edu.utp.aglome_tienda.dto.VarianteRequest;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VarianteService {

    private final VarianteRepository varianteRepository;
    private final ProductoBaseRepository productoBaseRepository;
    private final TipoAtributoRepository tipoAtributoRepository;
    private final VarianteAtributoRepository varianteAtributoRepository;

    // ── lecturas ─────────────────────────────────────────────────────
    public List<VarianteDTO> listarPorProducto(UUID productoBaseId) {
        return varianteRepository.findByProductoBaseId(productoBaseId)
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    // ── escrituras: solo panel admin ────────────────────────────────
    public VarianteDTO crear(VarianteRequest request) {
        ProductoBase productoBase = productoBaseRepository.findById(request.getProductoBaseId())
                .orElseThrow(() -> new RuntimeException("Producto base no encontrado"));

        Variante variante = new Variante();
        variante.setProductoBase(productoBase);
        variante.setSku(request.getSku());
        variante.setPrecioVenta(request.getPrecioVenta());
        variante.setStock(request.getStock());
        variante.setActiva(true);

        Variante guardada = varianteRepository.save(variante);

        guardarAtributos(guardada, request.getAtributos());

        return mapearADto(guardada);
    }

    public VarianteDTO actualizar(UUID id, VarianteRequest request) {
        Variante variante = varianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variante no encontrada"));

        variante.setSku(request.getSku());
        variante.setPrecioVenta(request.getPrecioVenta());
        variante.setStock(request.getStock());

        Variante actualizada = varianteRepository.save(variante);

        // reemplaza los atributos existentes por los nuevos que mandó el admin
        varianteAtributoRepository.findByVarianteId(id)
                .forEach(varianteAtributoRepository::delete);
        guardarAtributos(actualizada, request.getAtributos());

        return mapearADto(actualizada);
    }

    public void eliminar(UUID id) {
        if (!varianteRepository.existsById(id)) {
            throw new RuntimeException("Variante no encontrada");
        }
        varianteRepository.deleteById(id);
    }

    // ── helpers privados ─────────────────────────────────────────────
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
                v.getId(), v.getSku(), v.getPrecioVenta(), v.getStock(), v.getActiva(), atributos
        );
    }
}