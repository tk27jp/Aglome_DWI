package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.aglome_tienda.dto.ProductoMasVendidoDTO;
import pe.edu.utp.aglome_tienda.dto.StockBajoDTO;
import pe.edu.utp.aglome_tienda.dto.VentaCategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.VentaMesDTO;
import pe.edu.utp.aglome_tienda.service.ReporteService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final ReporteService reporteService;

    @GetMapping("/ventas-por-categoria")
    public ResponseEntity<List<VentaCategoriaDTO>> ventasPorCategoria() {
        return ResponseEntity.ok(reporteService.ventasPorCategoria());
    }

    @GetMapping("/ventas-por-mes")
    public ResponseEntity<List<VentaMesDTO>> ventasPorMes(
            @RequestParam(defaultValue = "" + ReporteService.MESES_POR_DEFECTO) int meses) {
        return ResponseEntity.ok(reporteService.ventasPorMes(meses));
    }

    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<List<ProductoMasVendidoDTO>> productosMasVendidos(
            @RequestParam(defaultValue = "" + ReporteService.LIMITE_POR_DEFECTO) int limite) {
        return ResponseEntity.ok(reporteService.productosMasVendidos(limite));
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<StockBajoDTO>> stockBajo(
            @RequestParam(defaultValue = "" + ReporteService.STOCK_BAJO_POR_DEFECTO) int umbral) {
        return ResponseEntity.ok(reporteService.stockBajo(umbral));
    }
}
