package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.aglome_tienda.dto.ResumenDashboardDTO;
import pe.edu.utp.aglome_tienda.service.ReporteService;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ReporteService reporteService;

    @GetMapping
    public ResponseEntity<ResumenDashboardDTO> resumen() {
        return ResponseEntity.ok(reporteService.resumenDashboard());
    }
}
