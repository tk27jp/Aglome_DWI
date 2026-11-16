package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.ProductoMasVendidoDTO;
import pe.edu.utp.aglome_tienda.dto.StockBajoDTO;
import pe.edu.utp.aglome_tienda.dto.VentaCategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.VentaMesDTO;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.ReporteService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReporteController.class)
@Import(SecurityConfig.class)
class AdminReporteControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ReporteService reporteService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void ventasPorCategoria_comoAdmin_debeRetornar200ConLasVentas() throws Exception {
        when(reporteService.ventasPorCategoria()).thenReturn(List.of(
                new VentaCategoriaDTO("Ferretería", 10L),
                new VentaCategoriaDTO("Melaminas", 4L)));

        mockMvc.perform(get("/api/admin/reportes/ventas-por-categoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("Ferretería"))
                .andExpect(jsonPath("$[0].unidades").value(10))
                .andExpect(jsonPath("$[1].categoria").value("Melaminas"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void ventasPorCategoria_comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/ventas-por-categoria"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ventasPorCategoria_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/ventas-por-categoria"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ventasPorMes_sinParametro_usaDoceMesesYRetorna200() throws Exception {
        when(reporteService.ventasPorMes(12)).thenReturn(List.of(
                new VentaMesDTO("2026-10", 3L, new BigDecimal("450.00"))));

        mockMvc.perform(get("/api/admin/reportes/ventas-por-mes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mes").value("2026-10"))
                .andExpect(jsonPath("$[0].pedidos").value(3))
                .andExpect(jsonPath("$[0].total").value(450.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ventasPorMes_conMesesInvalidos_debeRetornar400() throws Exception {
        when(reporteService.ventasPorMes(99)).thenThrow(new ReglaNegocioException("Los meses deben estar entre 1 y 36"));

        mockMvc.perform(get("/api/admin/reportes/ventas-por-mes").param("meses", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los meses deben estar entre 1 y 36"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void productosMasVendidos_comoAdmin_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(reporteService.productosMasVendidos(2)).thenReturn(List.of(
                new ProductoMasVendidoDTO(id, "Melamina Azul", 12L, new BigDecimal("480.00"))));

        mockMvc.perform(get("/api/admin/reportes/productos-mas-vendidos").param("limite", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Melamina Azul"))
                .andExpect(jsonPath("$[0].unidades").value(12))
                .andExpect(jsonPath("$[0].ingresos").value(480.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void stockBajo_conUmbral_debeRetornar200() throws Exception {
        when(reporteService.stockBajo(3)).thenReturn(List.of(
                new StockBajoDTO(UUID.randomUUID(), "PIN-1", "Pintura Blanca", 2)));

        mockMvc.perform(get("/api/admin/reportes/stock-bajo").param("umbral", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("PIN-1"))
                .andExpect(jsonPath("$[0].stock").value(2));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void reportesNuevos_comoCliente_debenRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/ventas-por-mes")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reportes/productos-mas-vendidos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reportes/stock-bajo")).andExpect(status().isForbidden());
    }

    @Test
    void reportesNuevos_sinToken_debenRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/ventas-por-mes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/reportes/productos-mas-vendidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/reportes/stock-bajo")).andExpect(status().isUnauthorized());
    }
}
