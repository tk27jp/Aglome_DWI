package pe.edu.utp.aglome_tienda.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.AgregarAlCarritoRequest;
import pe.edu.utp.aglome_tienda.service.CarritoService;

import java.util.UUID;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    // GET /api/carrito → ver mi carrito actual
    @GetMapping
    public ResponseEntity<?> verCarrito(Authentication authentication) {
        try {
            return ResponseEntity.ok(carritoService.verCarrito(authentication.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/carrito/items → agregar un producto al carrito
    @PostMapping("/items")
    public ResponseEntity<?> agregarItem(Authentication authentication,
                                          @Valid @RequestBody AgregarAlCarritoRequest request) {
        try {
            return ResponseEntity.ok(carritoService.agregarItem(authentication.getName(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/carrito/items/{itemId} → cambiar la cantidad de un item (o eliminarlo si mandas 0)
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> actualizarCantidad(Authentication authentication,
                                                 @PathVariable UUID itemId,
                                                 @RequestParam Integer cantidad) {
        try {
            return ResponseEntity.ok(carritoService.actualizarCantidad(authentication.getName(), itemId, cantidad));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/carrito/items/{itemId} → quitar un producto del carrito
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> eliminarItem(Authentication authentication, @PathVariable UUID itemId) {
        try {
            return ResponseEntity.ok(carritoService.eliminarItem(authentication.getName(), itemId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}