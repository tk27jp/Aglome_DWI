package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoBaseService {

    private final ProductoBaseRepository productoBaseRepository;
    private final CategoriaRepository categoriaRepository;
    private final VarianteService varianteService;

    // ── lecturas: tienda pública ─────────────────────────────────────
    public List<ProductoBaseDTO> listarPublicados() {
        return productoBaseRepository.findByPublicadoTrue()
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public List<ProductoBaseDTO> listarPorCategoria(Long categoriaId) {
        return productoBaseRepository.findByCategoriaIdAndPublicadoTrue(categoriaId)
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public ProductoBaseDTO obtenerPorId(UUID id) {
        return mapearADto(buscarOFallar(id));
    }

    // ── lectura para el admin (ve todo, incluso lo no publicado) ─────
    public List<ProductoBaseDTO> listarTodosAdmin() {
        return productoBaseRepository.findAll()
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    // ── escrituras: solo panel admin ─────────────────────────────────
    public ProductoBaseDTO crear(ProductoBaseRequest request) {
        ProductoBase producto = new ProductoBase();
        aplicarDatos(producto, request);
        ProductoBase guardado = productoBaseRepository.save(producto);
        return mapearADto(guardado);
    }

    public ProductoBaseDTO actualizar(UUID id, ProductoBaseRequest request) {
        ProductoBase producto = buscarOFallar(id);
        aplicarDatos(producto, request);
        ProductoBase actualizado = productoBaseRepository.save(producto);
        return mapearADto(actualizado);
    }

    public void eliminar(UUID id) {
        ProductoBase producto = buscarOFallar(id);
        productoBaseRepository.delete(producto);
    }

    // ── helpers privados ──────────────────────────────────────────────
    private ProductoBase buscarOFallar(UUID id) {
        return productoBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
    }

    private void aplicarDatos(ProductoBase producto, ProductoBaseRequest request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + request.getCategoriaId()));

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setMarca(request.getMarca());
        producto.setCategoria(categoria);
        producto.setPublicado(request.getPublicado() != null ? request.getPublicado() : false);
    }

    private ProductoBaseDTO mapearADto(ProductoBase p) {
        return new ProductoBaseDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getMarca(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getPublicado(),
                varianteService.listarPorProducto(p.getId())
        );
    }
}