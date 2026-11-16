package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.aglome_tienda.model.EstadoPedido;
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
        return ResponseEntity.ok(pedidoService.confirmarCompra(authentication.getName()));
    }

    // GET /api/pedidos → ver mi historial de compras
    // GET /api/pedidos            → todos mis pedidos
// GET /api/pedidos?estado=... → solo los de ese estado
    @GetMapping
    public ResponseEntity<?> misPedidos(Authentication authentication,
                                    @RequestParam(required = false) EstadoPedido estado) {
        if (estado == null) {
            return ResponseEntity.ok(pedidoService.misPedidos(authentication.getName()));
        }
            return ResponseEntity.ok(pedidoService.misPedidosPorEstado(authentication.getName(), estado));
    }

    // GET /api/pedidos/{id} → ver el detalle de un pedido específico mío
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(authentication.getName(), id));
    }

    // POST /api/pedidos/{id}/cancelar → cancelo mi pedido (solo si aún no está en preparación)
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.cancelarMiPedido(authentication.getName(), id));
    }
}
