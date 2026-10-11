package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.VentaCategoriaDTO;
import pe.edu.utp.aglome_tienda.repository.DetallePedidoRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private DetallePedidoRepository detallePedidoRepository;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    void ventasPorCategoria_conVentas_debeConvertirLasFilasADto() {
        // Simular datos SIN error (una fila: categoría y unidades vendidas)
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"Melaminas", 2L});
        when(detallePedidoRepository.unidadesVendidasPorCategoria()).thenReturn(filas);

        List<VentaCategoriaDTO> resultado = reporteService.ventasPorCategoria();

        // Verificar el test
        assertEquals(1, resultado.size());
        assertEquals("Melaminas", resultado.get(0).getCategoria());
        assertEquals(2L, resultado.get(0).getUnidades());
    }

    @Test
    void ventasPorCategoria_sinVentas_debeRetornarListaVacia() {
        // Simular caso sin datos (todavía no hay pedidos)
        when(detallePedidoRepository.unidadesVendidasPorCategoria()).thenReturn(new ArrayList<>());

        List<VentaCategoriaDTO> resultado = reporteService.ventasPorCategoria();

        // Verificar el test
        assertTrue(resultado.isEmpty());
    }
}