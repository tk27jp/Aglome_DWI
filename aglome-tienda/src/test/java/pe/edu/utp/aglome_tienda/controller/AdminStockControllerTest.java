package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.AjusteStockRequest;
import pe.edu.utp.aglome_tienda.dto.MovimientoStockDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.TipoMovimiento;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.MovimientoStockService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStockController.class)
@Import(SecurityConfig.class)
class AdminStockControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MovimientoStockService movimientoStockService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private MovimientoStockDTO movimiento(String tipo, int cantidad) {
        return new MovimientoStockDTO(UUID.randomUUID(), UUID.randomUUID(), "MEL-1", "Melamina Azul",
                tipo, cantidad, 10, 10 + cantidad, "motivo", null, "admin@aglome.com", LocalDateTime.now());
    }

    private PaginaDTO<MovimientoStockDTO> pagina(MovimientoStockDTO... movimientos) {
        return new PaginaDTO<>(List.of(movimientos), 0, 20, movimientos.length, 1, true, true);
    }

    // ---------- GET /movimientos ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void movimientos_sinFiltros_usaPaginaCeroYVeinte() throws Exception {
        when(movimientoStockService.listar(isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(pagina(movimiento("VENTA", -2)));

        mockMvc.perform(get("/api/admin/stock/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].tipo").value("VENTA"))
                .andExpect(jsonPath("$.contenido[0].cantidad").value(-2))
                .andExpect(jsonPath("$.contenido[0].sku").value("MEL-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movimientos_conFiltros_losPasaAlServicio() throws Exception {
        UUID varianteId = UUID.randomUUID();
        when(movimientoStockService.listar(eq(varianteId), eq(TipoMovimiento.AJUSTE), eq(1), eq(5)))
                .thenReturn(pagina(movimiento("AJUSTE", 5)));

        mockMvc.perform(get("/api/admin/stock/movimientos")
                        .param("varianteId", varianteId.toString())
                        .param("tipo", "AJUSTE").param("pagina", "1").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("AJUSTE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movimientos_conTipoQueNoExiste_debeRetornar400() throws Exception {
        mockMvc.perform(get("/api/admin/stock/movimientos").param("tipo", "ROBO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movimientos_conTamanoInvalido_debeRetornar400() throws Exception {
        when(movimientoStockService.listar(any(), any(), eq(0), eq(500)))
                .thenThrow(new ReglaNegocioException("El tamaño de página debe estar entre 1 y 50"));

        mockMvc.perform(get("/api/admin/stock/movimientos").param("tamano", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movimientos_conVarianteInexistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(movimientoStockService.listar(eq(id), isNull(), eq(0), eq(20)))
                .thenThrow(new RecursoNoEncontradoException("Variante no encontrada"));

        mockMvc.perform(get("/api/admin/stock/movimientos").param("varianteId", id.toString()))
                .andExpect(status().isNotFound());
    }

    // ---------- POST /ajustes ----------

    private String cuerpo(UUID varianteId, String cantidad, String motivo) {
        return "{\"varianteId\":" + (varianteId == null ? "null" : "\"" + varianteId + "\"")
                + ",\"cantidad\":" + cantidad
                + ",\"motivo\":" + (motivo == null ? "null" : "\"" + motivo + "\"") + "}";
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajustar_conDatosValidos_debeRetornar201() throws Exception {
        when(movimientoStockService.ajustar(any(AjusteStockRequest.class))).thenReturn(movimiento("AJUSTE", 5));

        mockMvc.perform(post("/api/admin/stock/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(UUID.randomUUID(), "5", "Reposición")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.stockResultante").value(15));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajustar_sinMotivo_debeRetornar400() throws Exception {
        mockMvc.perform(post("/api/admin/stock/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(UUID.randomUUID(), "5", " ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajustar_sinVarianteNiCantidad_debeRetornar400() throws Exception {
        mockMvc.perform(post("/api/admin/stock/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(null, "null", "Reposición")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajustar_queDejaStockNegativo_debeRetornar400() throws Exception {
        when(movimientoStockService.ajustar(any(AjusteStockRequest.class)))
                .thenThrow(new ReglaNegocioException("El ajuste dejaría el stock en negativo. Stock actual: 3"));

        mockMvc.perform(post("/api/admin/stock/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(UUID.randomUUID(), "-10", "Conteo")))
                .andExpect(status().isBadRequest());
    }

    // ---------- seguridad ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    void comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/stock/movimientos")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/stock/ajustes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(UUID.randomUUID(), "5", "x"))).andExpect(status().isForbidden());
    }

    @Test
    void sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/stock/movimientos")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/stock/ajustes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(UUID.randomUUID(), "5", "x"))).andExpect(status().isUnauthorized());
    }
}
