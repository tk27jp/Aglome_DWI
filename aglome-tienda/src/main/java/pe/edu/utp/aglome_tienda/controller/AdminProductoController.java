package pe.edu.utp.aglome_tienda.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseRequest;
import pe.edu.utp.aglome_tienda.service.ProductoBaseService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
public class AdminProductoController {

    private final ProductoBaseService productoBaseService;

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        return ResponseEntity.ok(productoBaseService.listarTodosAdmin());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoBaseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productoBaseService.crear(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @Valid @RequestBody ProductoBaseRequest request) {
        try {
            return ResponseEntity.ok(productoBaseService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            productoBaseService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}