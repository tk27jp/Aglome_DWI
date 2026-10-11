package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.PedidoService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPedidoController.class)
@Import(SecurityConfig.class)
class AdminPedidoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private final String url = "/api/admin/pedidos/" + UUID.randomUUID() + "/estado";

    @Test
    void cambiarEstado_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(patch(url).param("estado", "CANCELADO"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cambiarEstado_comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(patch(url).param("estado", "CANCELADO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_comoAdmin_debeRetornar200() throws Exception {
        when(pedidoService.cambiarEstado(any(UUID.class), any(EstadoPedido.class)))
                .thenReturn(null);

        mockMvc.perform(patch(url).param("estado", "CANCELADO"))
                .andExpect(status().isOk());
    }
}