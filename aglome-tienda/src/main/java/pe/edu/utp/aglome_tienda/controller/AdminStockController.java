package pe.edu.utp.aglome_tienda.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.AjusteStockRequest;
import pe.edu.utp.aglome_tienda.model.TipoMovimiento;
import pe.edu.utp.aglome_tienda.service.MovimientoStockService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final MovimientoStockService movimientoStockService;

    // GET /api/admin/stock/movimientos?varianteId=...&tipo=VENTA&pagina=0&tamano=20
    @GetMapping("/movimientos")
    public ResponseEntity<?> movimientos(@RequestParam(required = false) UUID varianteId,
                                         @RequestParam(required = false) TipoMovimiento tipo,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(movimientoStockService.listar(varianteId, tipo, pagina, tamano));
    }

    // POST /api/admin/stock/ajustes → reposición, merma o corrección manual (cantidad con signo)
    @PostMapping("/ajustes")
    public ResponseEntity<?> ajustar(@Valid @RequestBody AjusteStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoStockService.ajustar(request));
    }
}
