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

    @GetMapping
    public ResponseEntity<?> verCarrito(Authentication authentication) {
        return ResponseEntity.ok(carritoService.verCarrito(authentication.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<?> agregarItem(Authentication authentication,
                                          @Valid @RequestBody AgregarAlCarritoRequest request) {
        return ResponseEntity.ok(carritoService.agregarItem(authentication.getName(), request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> actualizarCantidad(Authentication authentication,
                                                 @PathVariable UUID itemId,
                                                 @RequestParam Integer cantidad) {
        return ResponseEntity.ok(carritoService.actualizarCantidad(authentication.getName(), itemId, cantidad));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> eliminarItem(Authentication authentication, @PathVariable UUID itemId) {
        return ResponseEntity.ok(carritoService.eliminarItem(authentication.getName(), itemId));
    }
}