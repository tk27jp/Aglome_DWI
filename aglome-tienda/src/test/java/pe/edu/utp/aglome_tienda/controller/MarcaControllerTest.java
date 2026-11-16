package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.MarcaDTO;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.MarcaService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarcaController.class)
@Import(SecurityConfig.class)
class MarcaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MarcaService marcaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void listar_debeRetornar200SinNecesitarLogin() throws Exception {
        when(marcaService.listarTodas()).thenReturn(List.of(new MarcaDTO(1L, "Aglome", null)));

        mockMvc.perform(get("/api/marcas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Aglome"));
    }

    @Test
    void obtener_conIdInexistente_debeRetornar404() throws Exception {
        when(marcaService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Marca no encontrada con id: 99"));

        mockMvc.perform(get("/api/marcas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Marca no encontrada con id: 99"));
    }
}
