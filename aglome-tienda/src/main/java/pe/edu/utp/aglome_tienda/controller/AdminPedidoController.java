package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.service.PedidoService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
public class AdminPedidoController {

    private final PedidoService pedidoService;

    // GET /api/admin/pedidos              → todos, los más recientes primero
    // GET /api/admin/pedidos?estado=ENVIADO → solo los de ese estado
    @GetMapping
    public ResponseEntity<?> listarTodos(@RequestParam(required = false) EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.listarTodosAdmin(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.obtenerPorIdAdmin(id));
    }

    // PATCH /api/admin/pedidos/{id}/estado?estado=EN_PREPARACION (solo transiciones válidas, si no → 409)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id,
                                           @RequestParam EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, estado));
    }
}