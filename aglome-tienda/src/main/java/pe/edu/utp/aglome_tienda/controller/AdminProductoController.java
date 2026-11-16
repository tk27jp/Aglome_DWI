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
        return ResponseEntity.status(HttpStatus.CREATED).body(productoBaseService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @Valid @RequestBody ProductoBaseRequest request) {
        return ResponseEntity.ok(productoBaseService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        productoBaseService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}