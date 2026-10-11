package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.CategoriaService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@Import(SecurityConfig.class)
class CategoriaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CategoriaService categoriaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void listar_debeRetornar200SinNecesitarLogin() throws Exception {
        when(categoriaService.listarTodas())
                .thenReturn(List.of(new CategoriaDTO(1L, "Melaminas", null, true)));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Melaminas"));
    }

    @Test
    void obtenerPorId_conIdInexistente_debeRetornar404() throws Exception {
        when(categoriaService.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Categoría no encontrada con id: 99"));

        mockMvc.perform(get("/api/categorias/99"))
                .andExpect(status().isNotFound());
    }
}