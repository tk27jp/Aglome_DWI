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
import pe.edu.utp.aglome_tienda.model.EstadoPedido;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.PedidoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value(url));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cambiarEstado_comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(patch(url).param("estado", "CANCELADO"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value(url));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_comoAdmin_debeRetornar200() throws Exception {
        when(pedidoService.cambiarEstado(any(UUID.class), any(EstadoPedido.class)))
                .thenReturn(null);

        mockMvc.perform(patch(url).param("estado", "CANCELADO"))
                .andExpect(status().isOk());
    }

    private PedidoDTO pedidoDeEjemplo(String estado, List<String> siguientes) {
        return new PedidoDTO(UUID.randomUUID(), LocalDateTime.now(), estado,
                new BigDecimal("91.80"), List.of(), siguientes);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_conTransicionInvalida_debeRetornar409() throws Exception {
        when(pedidoService.cambiarEstado(any(UUID.class), any(EstadoPedido.class)))
                .thenThrow(new ConflictoException("No se puede pasar un pedido de CONFIRMADO a ENTREGADO. Estados permitidos: EN_PREPARACION, CANCELADO"));

        mockMvc.perform(patch(url).param("estado", "ENTREGADO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Estados permitidos")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_conEstadoQueNoExiste_debeRetornar400() throws Exception {
        mockMvc.perform(patch(url).param("estado", "VOLANDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cambiarEstado_aEnPreparacion_devuelveElPedidoConSusSiguientesEstados() throws Exception {
        when(pedidoService.cambiarEstado(any(UUID.class), eq(EstadoPedido.EN_PREPARACION)))
                .thenReturn(pedidoDeEjemplo("EN_PREPARACION", List.of("ENVIADO", "CANCELADO")));

        mockMvc.perform(patch(url).param("estado", "EN_PREPARACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"))
                .andExpect(jsonPath("$.siguientesEstados[0]").value("ENVIADO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarTodos_sinFiltro_debeRetornar200() throws Exception {
        when(pedidoService.listarTodosAdmin(null))
                .thenReturn(List.of(pedidoDeEjemplo("CONFIRMADO", List.of("EN_PREPARACION", "CANCELADO"))));

        mockMvc.perform(get("/api/admin/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarTodos_conFiltroDeEstado_pasaElEstadoAlServicio() throws Exception {
        when(pedidoService.listarTodosAdmin(EstadoPedido.ENVIADO))
                .thenReturn(List.of(pedidoDeEjemplo("ENVIADO", List.of("ENTREGADO"))));

        mockMvc.perform(get("/api/admin/pedidos").param("estado", "ENVIADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENVIADO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtener_existente_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(pedidoService.obtenerPorIdAdmin(id)).thenReturn(pedidoDeEjemplo("CONFIRMADO", List.of()));

        mockMvc.perform(get("/api/admin/pedidos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtener_inexistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(pedidoService.obtenerPorIdAdmin(id)).thenThrow(new RecursoNoEncontradoException("Pedido no encontrado"));

        mockMvc.perform(get("/api/admin/pedidos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void listarYDetalle_comoCliente_debenRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/pedidos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/pedidos/" + UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    void listarYDetalle_sinToken_debenRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/pedidos/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }
}
