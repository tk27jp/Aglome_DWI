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
import pe.edu.utp.aglome_tienda.dto.ResumenDashboardDTO;
import pe.edu.utp.aglome_tienda.dto.StockBajoDTO;
import pe.edu.utp.aglome_tienda.dto.VentaMesDTO;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.ReporteService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import(SecurityConfig.class)
class AdminDashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ReporteService reporteService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private ResumenDashboardDTO resumenDeEjemplo() {
        Map<String, Long> estados = new LinkedHashMap<>();
        estados.put("PENDIENTE", 1L);
        estados.put("CANCELADO", 2L);
        return new ResumenDashboardDTO(
                new BigDecimal("300.00"), 3L, new BigDecimal("100.00"), estados, 1L,
                List.of(new VentaMesDTO("2026-11", 3L, new BigDecimal("300.00"))),
                List.of(new ProductoMasVendidoDTO(UUID.randomUUID(), "Melamina Azul", 12L, new BigDecimal("480.00"))),
                List.of(new StockBajoDTO(UUID.randomUUID(), "PIN-1", "Pintura Blanca", 2)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resumen_comoAdmin_debeRetornar200ConTodasLasSecciones() throws Exception {
        when(reporteService.resumenDashboard()).thenReturn(resumenDeEjemplo());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosTotales").value(300.00))
                .andExpect(jsonPath("$.pedidosValidos").value(3))
                .andExpect(jsonPath("$.ticketPromedio").value(100.00))
                .andExpect(jsonPath("$.pedidosPorEstado.CANCELADO").value(2))
                .andExpect(jsonPath("$.variantesConStockBajo").value(1))
                .andExpect(jsonPath("$.ventasPorMes[0].mes").value("2026-11"))
                .andExpect(jsonPath("$.productosMasVendidos[0].nombre").value("Melamina Azul"))
                .andExpect(jsonPath("$.stockBajo[0].sku").value("PIN-1"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void resumen_comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void resumen_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }
}
