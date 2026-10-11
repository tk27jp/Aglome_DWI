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
import pe.edu.utp.aglome_tienda.dto.AgregarAlCarritoRequest;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.CarritoService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarritoController.class)
@Import(SecurityConfig.class)
class CarritoControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private CarritoService carritoService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "ana@test.com")
    void agregarItem_usuarioLogueado_debeRetornar200() throws Exception {
        AgregarAlCarritoRequest request = new AgregarAlCarritoRequest();
        request.setVarianteId(UUID.randomUUID());
        request.setCantidad(2);

        when(carritoService.agregarItem(anyString(), any(AgregarAlCarritoRequest.class))).thenReturn(null);

        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void agregarItem_sinLogin_debeRetornar401() throws Exception {
        AgregarAlCarritoRequest request = new AgregarAlCarritoRequest();
        request.setVarianteId(UUID.randomUUID());
        request.setCantidad(2);

        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}