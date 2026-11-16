package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.PedidoDTO;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.PedidoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.access.AccessDeniedException;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void confirmarCompra_conCarritoValido_debeRetornar200() throws Exception {
        when(pedidoService.confirmarCompra(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/pedidos/confirmar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void confirmarCompra_conCarritoVacio_debeRetornar400() throws Exception {
        when(pedidoService.confirmarCompra(anyString()))
                .thenThrow(new ReglaNegocioException("Tu carrito está vacío, no se puede confirmar la compra"));

        mockMvc.perform(post("/api/pedidos/confirmar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cancelar_pedidoPropioCancelable_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(pedidoService.cancelarMiPedido(anyString(), any(UUID.class)))
                .thenReturn(new PedidoDTO(id, LocalDateTime.now(), "CANCELADO",
                        new BigDecimal("91.80"), List.of(), List.of()));

        mockMvc.perform(post("/api/pedidos/" + id + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cancelar_pedidoYaEnPreparacion_debeRetornar409() throws Exception {
        when(pedidoService.cancelarMiPedido(anyString(), any(UUID.class)))
                .thenThrow(new ConflictoException("Este pedido ya no se puede cancelar porque está EN_PREPARACION"));

        mockMvc.perform(post("/api/pedidos/" + UUID.randomUUID() + "/cancelar"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cancelar_pedidoDeOtroUsuario_debeRetornar403() throws Exception {
        when(pedidoService.cancelarMiPedido(anyString(), any(UUID.class)))
                .thenThrow(new AccessDeniedException("Este pedido no te pertenece"));

        mockMvc.perform(post("/api/pedidos/" + UUID.randomUUID() + "/cancelar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cancelar_pedidoInexistente_debeRetornar404() throws Exception {
        when(pedidoService.cancelarMiPedido(anyString(), any(UUID.class)))
                .thenThrow(new RecursoNoEncontradoException("Pedido no encontrado"));

        mockMvc.perform(post("/api/pedidos/" + UUID.randomUUID() + "/cancelar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelar_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(post("/api/pedidos/" + UUID.randomUUID() + "/cancelar"))
                .andExpect(status().isUnauthorized());
    }
}
