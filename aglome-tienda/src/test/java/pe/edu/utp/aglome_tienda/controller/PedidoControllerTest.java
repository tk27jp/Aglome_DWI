package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.PedidoService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "ana@test.com")
    void confirmarCompra_conCarritoValido_debeRetornar200() throws Exception {
        when(pedidoService.confirmarCompra(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/pedidos/confirmar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "ana@test.com")
    void confirmarCompra_conCarritoVacio_debeRetornar400() throws Exception {
        when(pedidoService.confirmarCompra(anyString()))
                .thenThrow(new RuntimeException("Tu carrito está vacío, no se puede confirmar la compra"));

        mockMvc.perform(post("/api/pedidos/confirmar"))
                .andExpect(status().isBadRequest());
    }
}