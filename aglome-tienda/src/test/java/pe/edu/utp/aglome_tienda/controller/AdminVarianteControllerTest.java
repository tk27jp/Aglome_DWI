package pe.edu.utp.aglome_tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.dto.VarianteDTO;
import pe.edu.utp.aglome_tienda.dto.VarianteRequest;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.VarianteService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVarianteController.class)
@Import(SecurityConfig.class)
class AdminVarianteControllerTest {

    private static final UUID ID = UUID.fromString("e39a7f53-2050-4897-a938-dea4651c8bdc");

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private VarianteService varianteService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private VarianteRequest requestValido() {
        VarianteRequest request = new VarianteRequest();
        request.setProductoBaseId(UUID.randomUUID());
        request.setSku("BISAG-ACE-3PUL");
        request.setPrecioVenta(new BigDecimal("4.50"));
        request.setStock(100);
        request.setAtributos(Map.of("Medida", "3 pulgadas"));
        return request;
    }

    private VarianteDTO varianteEjemplo() {
        VarianteDTO dto = new VarianteDTO();
        dto.setId(ID);
        dto.setSku("BISAG-ACE-3PUL");
        dto.setStock(100);
        return dto;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conDatosValidos_debeRetornar201() throws Exception {
        when(varianteService.crear(any(VarianteRequest.class))).thenReturn(varianteEjemplo());

        mockMvc.perform(post("/api/admin/variantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("BISAG-ACE-3PUL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_sinSku_debeRetornar400PorValidacion() throws Exception {
        VarianteRequest request = requestValido();
        request.setSku(null);

        mockMvc.perform(post("/api/admin/variantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conStockNegativo_debeRetornar400PorValidacion() throws Exception {
        VarianteRequest request = requestValido();
        request.setStock(-1);

        mockMvc.perform(post("/api/admin/variantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void crear_comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(post("/api/admin/variantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(post("/api/admin/variantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizar_conDatosValidos_debeRetornar200() throws Exception {
        when(varianteService.actualizar(eq(ID), any(VarianteRequest.class)))
                .thenReturn(varianteEjemplo());

        mockMvc.perform(put("/api/admin/variantes/" + ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminar_conIdExistente_debeRetornar204() throws Exception {
        mockMvc.perform(delete("/api/admin/variantes/" + ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminar_conIdInexistente_debeRetornar404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Variante no encontrada"))
                .when(varianteService).eliminar(ID);

        mockMvc.perform(delete("/api/admin/variantes/" + ID))
                .andExpect(status().isNotFound());
    }
}