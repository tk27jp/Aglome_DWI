package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.FiltroProductoRequest;
import pe.edu.utp.aglome_tienda.dto.OpcionesFiltroDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.service.ProductoBaseService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoBaseService productoBaseService;

    @GetMapping
    public ResponseEntity<PaginaDTO<ProductoBaseDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            @RequestParam(defaultValue = "nombre") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        return ResponseEntity.ok(productoBaseService.listarPublicados(pagina, tamano, orden, direccion));
    }

    @GetMapping("/filtrar")
    public ResponseEntity<PaginaDTO<ProductoBaseDTO>> filtrar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String medida,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            @RequestParam(defaultValue = "nombre") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        FiltroProductoRequest filtro = new FiltroProductoRequest(
                texto, categoriaId, marcaId, color, medida, precioMin, precioMax, disponible);
        return ResponseEntity.ok(productoBaseService.filtrar(filtro, pagina, tamano, orden, direccion));
    }

    // opciones disponibles para dibujar el panel de filtros
    @GetMapping("/filtros")
    public ResponseEntity<OpcionesFiltroDTO> opcionesDeFiltro() {
        return ResponseEntity.ok(productoBaseService.opcionesDeFiltro());
    }

    // autocompletado: GET /api/productos/sugerencias?texto=bis
    @GetMapping("/sugerencias")
    public ResponseEntity<List<String>> sugerencias(@RequestParam String texto) {
        return ResponseEntity.ok(productoBaseService.sugerencias(texto));
    }

    @GetMapping("/{id}/relacionados")
    public ResponseEntity<List<ProductoBaseDTO>> relacionados(@PathVariable UUID id) {
        return ResponseEntity.ok(productoBaseService.relacionados(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<PaginaDTO<ProductoBaseDTO>> buscar(
            @RequestParam String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            @RequestParam(defaultValue = "nombre") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        return ResponseEntity.ok(productoBaseService.buscarPorNombre(texto, pagina, tamano, orden, direccion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(productoBaseService.obtenerPorId(id));
    }

    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<PaginaDTO<ProductoBaseDTO>> porCategoria(
            @PathVariable Long categoriaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            @RequestParam(defaultValue = "nombre") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        return ResponseEntity.ok(productoBaseService.listarPorCategoria(categoriaId, pagina, tamano, orden, direccion));
    }
}
