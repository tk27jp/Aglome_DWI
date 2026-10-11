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
@PreAuthorize("hasRole('ADMIN')")
public class AdminPedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodosAdmin());
    }

    // PUT /api/admin/pedidos/{id}/estado?estado=CANCELADO
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id,
                                           @RequestParam EstadoPedido estado) {
        try {
            return ResponseEntity.ok(pedidoService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}