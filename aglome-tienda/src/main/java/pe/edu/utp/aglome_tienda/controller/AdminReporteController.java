package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.aglome_tienda.service.ReporteService;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final ReporteService reporteService;

    @GetMapping("/ventas-por-categoria")
    public ResponseEntity<?> ventasPorCategoria() {
        return ResponseEntity.ok(reporteService.ventasPorCategoria());
    }
}