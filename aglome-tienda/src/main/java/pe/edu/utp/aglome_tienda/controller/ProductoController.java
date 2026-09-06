package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.service.ProductoBaseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoBaseService productoBaseService;

    @GetMapping
    public ResponseEntity<List<ProductoBaseDTO>> listar() {
        return ResponseEntity.ok(productoBaseService.listarPublicados());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(productoBaseService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoBaseDTO>> porCategoria(@PathVariable Long categoriaId) {
        return ResponseEntity.ok(productoBaseService.listarPorCategoria(categoriaId));
    }
}