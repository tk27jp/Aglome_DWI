package pe.edu.utp.aglome_tienda.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.VarianteRequest;
import pe.edu.utp.aglome_tienda.service.VarianteService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/variantes")
@RequiredArgsConstructor
public class AdminVarianteController {

    private final VarianteService varianteService;

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody VarianteRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(varianteService.crear(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @Valid @RequestBody VarianteRequest request) {
        try {
            return ResponseEntity.ok(varianteService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            varianteService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}