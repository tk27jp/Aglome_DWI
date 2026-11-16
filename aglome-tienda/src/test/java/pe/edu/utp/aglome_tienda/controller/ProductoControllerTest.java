package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.FiltroProductoRequest;
import pe.edu.utp.aglome_tienda.dto.MarcaDTO;
import pe.edu.utp.aglome_tienda.dto.OpcionesFiltroDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.ProductoBaseService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
class ProductoControllerTest {

    private static final UUID ID = UUID.fromString("0e7624a9-f473-41bc-965f-cdc15be620f1");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProductoBaseService productoBaseService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private ProductoBaseDTO productoEjemplo() {
        ProductoBaseDTO dto = new ProductoBaseDTO();
        dto.setId(ID);
        dto.setNombre("Melamina Azul Marino");
        dto.setImagenUrl("https://ejemplo.com/melamina.jpg");
        dto.setCategoriaNombre("Melaminas");
        dto.setPublicado(true);
        return dto;
    }

    private PaginaDTO<ProductoBaseDTO> paginaEjemplo() {
        return new PaginaDTO<>(List.of(productoEjemplo()), 0, 12, 1, 1, true, true);
    }

    @Test
    void listar_debeRetornar200SinNecesitarLogin() throws Exception {
        when(productoBaseService.listarPublicados(0, 12, "nombre", "asc")).thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Melamina Azul Marino"))
                .andExpect(jsonPath("$.contenido[0].imagenUrl").value("https://ejemplo.com/melamina.jpg"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primera").value(true));
    }

    @Test
    void listar_conParametros_debePasarlosAlServicio() throws Exception {
        when(productoBaseService.listarPublicados(2, 5, "marca", "desc")).thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos")
                        .param("pagina", "2").param("tamano", "5")
                        .param("orden", "marca").param("direccion", "desc"))
                .andExpect(status().isOk());

        verify(productoBaseService).listarPublicados(2, 5, "marca", "desc");
    }

    @Test
    void listar_conOrdenInvalido_debeRetornar400() throws Exception {
        when(productoBaseService.listarPublicados(0, 12, "precio", "asc"))
                .thenThrow(new ReglaNegocioException("No se puede ordenar por 'precio'"));

        mockMvc.perform(get("/api/productos").param("orden", "precio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede ordenar por 'precio'"));
    }

    @Test
    void listar_conTamanoNoNumerico_debeRetornar400() throws Exception {
        mockMvc.perform(get("/api/productos").param("tamano", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscar_debeRetornarLosProductosQueCoinciden() throws Exception {
        when(productoBaseService.buscarPorNombre("melamina", 0, 12, "nombre", "asc")).thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos/buscar").param("texto", "melamina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Melamina Azul Marino"));
    }

    @Test
    void obtener_conIdExistente_debeRetornar200() throws Exception {
        when(productoBaseService.obtenerPorId(ID)).thenReturn(productoEjemplo());

        mockMvc.perform(get("/api/productos/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void obtener_conIdInexistente_debeRetornar404() throws Exception {
        UUID otroId = UUID.randomUUID();
        when(productoBaseService.obtenerPorId(otroId))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado con id: " + otroId));

        mockMvc.perform(get("/api/productos/" + otroId))
                .andExpect(status().isNotFound());
    }

    @Test
    void porCategoria_debeRetornarLosProductosDeEsaCategoria() throws Exception {
        when(productoBaseService.listarPorCategoria(1L, 0, 12, "nombre", "asc")).thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos/categoria/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].categoriaNombre").value("Melaminas"));
    }

    @Test
    void filtrar_conDisponible_debePasarSoloDisponiblesAlServicio() throws Exception {
        FiltroProductoRequest esperado = new FiltroProductoRequest();
        esperado.setSoloDisponibles(true);
        when(productoBaseService.filtrar(esperado, 0, 12, "nombre", "asc")).thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos/filtrar").param("disponible", "true"))
                .andExpect(status().isOk());

        verify(productoBaseService).filtrar(esperado, 0, 12, "nombre", "asc");
    }

    @Test
    void filtrar_conTodosLosParametros_debeArmarElFiltroYPasarloAlServicio() throws Exception {
        FiltroProductoRequest esperado = new FiltroProductoRequest("mela", 1L, 2L, "Azul", "18mm",
                new BigDecimal("10"), new BigDecimal("60"));
        when(productoBaseService.filtrar(esperado, 0, 12, "nombre", "asc")).thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos/filtrar")
                        .param("texto", "mela").param("categoriaId", "1").param("marcaId", "2")
                        .param("color", "Azul").param("medida", "18mm")
                        .param("precioMin", "10").param("precioMax", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Melamina Azul Marino"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        verify(productoBaseService).filtrar(esperado, 0, 12, "nombre", "asc");
    }

    @Test
    void filtrar_sinParametros_debeUsarFiltroVacioYValoresPorDefecto() throws Exception {
        when(productoBaseService.filtrar(new FiltroProductoRequest(), 0, 12, "nombre", "asc"))
                .thenReturn(paginaEjemplo());

        mockMvc.perform(get("/api/productos/filtrar"))
                .andExpect(status().isOk());

        verify(productoBaseService).filtrar(new FiltroProductoRequest(), 0, 12, "nombre", "asc");
    }

    @Test
    void filtrar_conPrecioInvalido_debeRetornar400() throws Exception {
        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setPrecioMin(new BigDecimal("100"));
        filtro.setPrecioMax(new BigDecimal("10"));
        when(productoBaseService.filtrar(filtro, 0, 12, "nombre", "asc"))
                .thenThrow(new ReglaNegocioException("El precio mínimo no puede ser mayor que el precio máximo"));

        mockMvc.perform(get("/api/productos/filtrar").param("precioMin", "100").param("precioMax", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El precio mínimo no puede ser mayor que el precio máximo"));
    }

    @Test
    void opcionesDeFiltro_debeRetornar200SinNecesitarLogin() throws Exception {
        when(productoBaseService.opcionesDeFiltro()).thenReturn(new OpcionesFiltroDTO(
                List.of(new CategoriaDTO(1L, "Melaminas", null, true, null)),
                List.of(new MarcaDTO(1L, "Aglome", null)),
                List.of("Azul"), List.of("18mm"), new BigDecimal("4.50"), new BigDecimal("52.00")));

        mockMvc.perform(get("/api/productos/filtros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].nombre").value("Melaminas"))
                .andExpect(jsonPath("$.marcas[0].nombre").value("Aglome"))
                .andExpect(jsonPath("$.colores[0]").value("Azul"))
                .andExpect(jsonPath("$.precioMaximo").value(52.00));
    }

    @Test
    void sugerencias_debeRetornarLaListaDeNombres() throws Exception {
        when(productoBaseService.sugerencias("bis")).thenReturn(List.of("Bisagra de Acero"));

        mockMvc.perform(get("/api/productos/sugerencias").param("texto", "bis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Bisagra de Acero"));
    }

    @Test
    void relacionados_debeRetornarLosProductosDeLaMismaCategoria() throws Exception {
        UUID id = UUID.randomUUID();
        when(productoBaseService.relacionados(id)).thenReturn(List.of(productoEjemplo()));

        mockMvc.perform(get("/api/productos/" + id + "/relacionados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Melamina Azul Marino"));
    }

    @Test
    void relacionados_conProductoInexistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productoBaseService.relacionados(id))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado con id: " + id));

        mockMvc.perform(get("/api/productos/" + id + "/relacionados"))
                .andExpect(status().isNotFound());
    }
}
