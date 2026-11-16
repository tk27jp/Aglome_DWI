package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.FiltroProductoRequest;
import pe.edu.utp.aglome_tienda.dto.MarcaDTO;
import pe.edu.utp.aglome_tienda.dto.OpcionesFiltroDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseRequest;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.MarcaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoSpecification;
import pe.edu.utp.aglome_tienda.repository.VarianteAtributoRepository;
import pe.edu.utp.aglome_tienda.repository.VarianteRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoBaseService {

    private final ProductoBaseRepository productoBaseRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final VarianteService varianteService;
    private final VarianteAtributoRepository varianteAtributoRepository;
    private final VarianteRepository varianteRepository;

    // ── lecturas: tienda pública ─────────────────────────────────────
    public PaginaDTO<ProductoBaseDTO> listarPublicados(int pagina, int tamano, String orden, String direccion) {
        Pageable pageable = armarPaginacion(pagina, tamano, orden, direccion);
        return PaginaDTO.desde(productoBaseRepository.findByPublicadoTrueAndEliminadoFalse(pageable).map(this::mapearADto));
    }

    public PaginaDTO<ProductoBaseDTO> listarPorCategoria(Long categoriaId, int pagina, int tamano,
                                                         String orden, String direccion) {
        Pageable pageable = armarPaginacion(pagina, tamano, orden, direccion);
        return PaginaDTO.desde(productoBaseRepository
                .findByCategoriaIdAndPublicadoTrueAndEliminadoFalse(categoriaId, pageable).map(this::mapearADto));
    }

    // catálogo con filtros: categoría, marca, color, medida, rango de precio y texto
    public PaginaDTO<ProductoBaseDTO> filtrar(FiltroProductoRequest filtro, int pagina, int tamano,
                                              String orden, String direccion) {
        validarFiltro(filtro);
        Pageable pageable = armarPaginacion(pagina, tamano, orden, direccion);
        Page<ProductoBase> resultado = productoBaseRepository.findAll(ProductoSpecification.desde(filtro), pageable);
        return PaginaDTO.desde(resultado.map(this::mapearADto));
    }

    // las opciones que el frontend muestra en el panel de filtros: solo lo que existe en la tienda hoy
    public OpcionesFiltroDTO opcionesDeFiltro() {
        List<CategoriaDTO> categorias = categoriaRepository.categoriasConProductosVisibles().stream()
                .map(c -> new CategoriaDTO(c.getId(), c.getNombre(),
                        c.getPadre() != null ? c.getPadre().getId() : null, c.getActiva(), c.getImagenUrl()))
                .collect(Collectors.toList());
        List<MarcaDTO> marcas = marcaRepository.marcasConProductosVisibles().stream()
                .map(m -> new MarcaDTO(m.getId(), m.getNombre(), m.getImagenUrl()))
                .collect(Collectors.toList());

        return new OpcionesFiltroDTO(
                categorias,
                marcas,
                varianteAtributoRepository.valoresDisponibles("Color"),
                varianteAtributoRepository.valoresDisponibles("Medida"),
                varianteRepository.precioMinimoDisponible(),
                varianteRepository.precioMaximoDisponible());
    }

    // autocompletado del buscador: hasta 8 nombres; con menos de 2 letras no busca nada
    public List<String> sugerencias(String texto) {
        if (texto == null || texto.trim().length() < 2) {
            return List.of();
        }
        return productoBaseRepository.sugerenciasPorNombre(texto.trim(), PageRequest.of(0, MAX_SUGERENCIAS));
    }

    // "te puede interesar": hasta 4 productos de la misma categoría, sin incluir el mismo
    public List<ProductoBaseDTO> relacionados(UUID id) {
        ProductoBase producto = buscarOFallar(id);
        return productoBaseRepository
                .findByCategoriaIdAndPublicadoTrueAndEliminadoFalseAndIdNot(
                        producto.getCategoria().getId(), id,
                        PageRequest.of(0, MAX_RELACIONADOS, Sort.by("nombre")))
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public ProductoBaseDTO obtenerPorId(UUID id) {
        return mapearADto(buscarOFallar(id));
    }

    // ── lectura para el admin (ve todo, incluso lo no publicado) ─────
    public List<ProductoBaseDTO> listarTodosAdmin() {
        return productoBaseRepository.findByEliminadoFalse()
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
        // borrado lógico: se oculta el producto y sus variantes, pero los pedidos viejos conservan el historial
        producto.setEliminado(true);
        productoBaseRepository.save(producto);
        varianteService.eliminarPorProducto(id);
    }

    public PaginaDTO<ProductoBaseDTO> buscarPorNombre(String texto, int pagina, int tamano,
                                                      String orden, String direccion) {
        Pageable pageable = armarPaginacion(pagina, tamano, orden, direccion);
        Page<ProductoBase> resultado = productoBaseRepository.buscarPublicadosPorNombre(texto, pageable);
        return PaginaDTO.desde(resultado.map(this::mapearADto));
    }

    // ── helpers privados ──────────────────────────────────────────────
    private static final int TAMANO_MAXIMO = 50;
    private static final int MAX_SUGERENCIAS = 8;
    private static final int MAX_RELACIONADOS = 4;

    private void validarFiltro(FiltroProductoRequest filtro) {
        if (filtro.getPrecioMin() != null && filtro.getPrecioMin().signum() < 0) {
            throw new ReglaNegocioException("El precio mínimo no puede ser negativo");
        }
        if (filtro.getPrecioMax() != null && filtro.getPrecioMax().signum() < 0) {
            throw new ReglaNegocioException("El precio máximo no puede ser negativo");
        }
        if (filtro.getPrecioMin() != null && filtro.getPrecioMax() != null
                && filtro.getPrecioMin().compareTo(filtro.getPrecioMax()) > 0) {
            throw new ReglaNegocioException("El precio mínimo no puede ser mayor que el precio máximo");
        }
    }

    // campos por los que se permite ordenar (lo que pide el cliente -> propiedad de la entidad)
    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
            "nombre", "nombre",
            "marca", "marca.nombre");

    private Pageable armarPaginacion(int pagina, int tamano, String orden, String direccion) {
        if (pagina < 0) {
            throw new ReglaNegocioException("La página no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        String propiedad = CAMPOS_ORDEN.get(orden);
        if (propiedad == null) {
            throw new ReglaNegocioException("No se puede ordenar por '" + orden
                    + "'. Campos válidos: " + String.join(", ", CAMPOS_ORDEN.keySet().stream().sorted().toList()));
        }
        Sort.Direction dir;
        if ("asc".equalsIgnoreCase(direccion)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direccion)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new ReglaNegocioException("La dirección debe ser 'asc' o 'desc'");
        }
        return PageRequest.of(pagina, tamano, Sort.by(dir, propiedad));
    }

    private ProductoBase buscarOFallar(UUID id) {
        return productoBaseRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getEliminado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con id: " + id));
    }

    private void aplicarDatos(ProductoBase producto, ProductoBaseRequest request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .filter(c -> !Boolean.TRUE.equals(c.getEliminado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con id: " + request.getCategoriaId()));

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        Marca marca = null;
        if (request.getMarcaId() != null) {
            marca = marcaRepository.findById(request.getMarcaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Marca no encontrada con id: " + request.getMarcaId()));
        }
        producto.setMarca(marca);
        producto.setImagenUrl(request.getImagenUrl());
        producto.setCategoria(categoria);
        producto.setPublicado(request.getPublicado() != null ? request.getPublicado() : false);
    }

    private ProductoBaseDTO mapearADto(ProductoBase p) {
        return new ProductoBaseDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getMarca() != null ? p.getMarca().getNombre() : null,
                p.getMarca() != null ? p.getMarca().getId() : null,
                p.getImagenUrl(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getPublicado(),
                varianteService.listarPorProducto(p.getId()),
                p.getFechaCreacion()
        );
    }
}