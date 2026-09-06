package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.service.PedidoService;

import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    // POST /api/pedidos/confirmar → convierte el carrito actual en un pedido
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarCompra(Authentication authentication) {
        try {
            return ResponseEntity.ok(pedidoService.confirmarCompra(authentication.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/pedidos → ver mi historial de compras
    @GetMapping
    public ResponseEntity<?> misPedidos(Authentication authentication) {
        return ResponseEntity.ok(pedidoService.misPedidos(authentication.getName()));
    }

    // GET /api/pedidos/{id} → ver el detalle de un pedido específico mío
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(Authentication authentication, @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(pedidoService.obtenerPorId(authentication.getName(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}