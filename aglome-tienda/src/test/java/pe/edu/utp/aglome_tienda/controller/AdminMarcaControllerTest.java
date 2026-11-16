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
import pe.edu.utp.aglome_tienda.dto.MarcaDTO;
import pe.edu.utp.aglome_tienda.dto.MarcaRequest;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.MarcaService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMarcaController.class)
@Import(SecurityConfig.class)
class AdminMarcaControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private MarcaService marcaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private String json(String nombre) throws Exception {
        MarcaRequest request = new MarcaRequest();
        request.setNombre(nombre);
        return objectMapper.writeValueAsString(request);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conDatosValidos_debeRetornar201() throws Exception {
        when(marcaService.crear(any(MarcaRequest.class))).thenReturn(new MarcaDTO(1L, "Aglome", null));

        mockMvc.perform(post("/api/admin/marcas")
                        .contentType(MediaType.APPLICATION_JSON).content(json("Aglome")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Aglome"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_sinNombre_debeRetornar400PorValidacion() throws Exception {
        mockMvc.perform(post("/api/admin/marcas")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conNombreRepetido_debeRetornar409() throws Exception {
        when(marcaService.crear(any(MarcaRequest.class)))
                .thenThrow(new ConflictoException("Ya existe una marca con ese nombre"));

        mockMvc.perform(post("/api/admin/marcas")
                        .contentType(MediaType.APPLICATION_JSON).content(json("Aglome")))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void crear_comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(post("/api/admin/marcas")
                        .contentType(MediaType.APPLICATION_JSON).content(json("Aglome")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(post("/api/admin/marcas")
                        .contentType(MediaType.APPLICATION_JSON).content(json("Aglome")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminar_conProductosAsociados_debeRetornar409() throws Exception {
        doThrow(new ConflictoException("No se puede eliminar la marca porque tiene productos asociados"))
                .when(marcaService).eliminar(1L);

        mockMvc.perform(delete("/api/admin/marcas/1"))
                .andExpect(status().isConflict());
    }
}
