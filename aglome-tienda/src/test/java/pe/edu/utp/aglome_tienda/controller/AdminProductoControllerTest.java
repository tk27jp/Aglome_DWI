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
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseRequest;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.ProductoBaseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProductoController.class)
@Import(SecurityConfig.class)
class AdminProductoControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private ProductoBaseService productoBaseService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conDatosValidos_debeRetornar201() throws Exception {
        ProductoBaseRequest request = new ProductoBaseRequest();
        request.setNombre("Melamina Azul Marino");
        request.setCategoriaId(1L);
        request.setPublicado(true);

        ProductoBaseDTO respuesta = new ProductoBaseDTO();
        respuesta.setNombre("Melamina Azul Marino");

        when(productoBaseService.crear(any(ProductoBaseRequest.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/admin/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Melamina Azul Marino"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_sinNombre_debeRetornar400PorValidacion() throws Exception {
        ProductoBaseRequest request = new ProductoBaseRequest();
        request.setCategoriaId(1L);

        mockMvc.perform(post("/api/admin/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_sinAutenticarse_debeRetornar401() throws Exception {
        ProductoBaseRequest request = new ProductoBaseRequest();
        request.setNombre("Melamina Azul Marino");
        request.setCategoriaId(1L);

        mockMvc.perform(post("/api/admin/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}