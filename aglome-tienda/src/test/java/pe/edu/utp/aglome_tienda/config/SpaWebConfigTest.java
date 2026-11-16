package pe.edu.utp.aglome_tienda.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El front de Angular se sirve desde el mismo servidor. Usa src/test/resources/static/index.html,
 * así la prueba no depende de que el front esté compilado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpaWebConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void raiz_devuelveLaPaginaPrincipal_sinToken() throws Exception {
        // Para "/" Spring reenvía internamente a index.html (MockMvc no ejecuta el reenvío,
        // un servidor real sí): vale que llegue la página o que el destino del reenvío sea index.html.
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String cuerpo = result.getResponse().getContentAsString();
                    String reenvio = result.getResponse().getForwardedUrl();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            cuerpo.contains("<app-root>") || "index.html".equals(reenvio),
                            "La raíz debe entregar el front (cuerpo=" + cuerpo + ", reenvío=" + reenvio + ")");
                });
    }

    @Test
    void rutaDeAngular_alRecargar_devuelveIndexHtml() throws Exception {
        mockMvc.perform(get("/productos/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<app-root>")));
    }

    @Test
    void archivoInexistente_noDevuelveLaPaginaPrincipal() throws Exception {
        mockMvc.perform(get("/no-existe.js"))
                .andExpect(status().isNotFound());
    }

    @Test
    void api_sigueProtegida_sinToken() throws Exception {
        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void api_rutaInexistenteNoCaeEnElFront() throws Exception {
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiPublica_siguePublica() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void postFueraDeLaApi_requiereAutenticacion() throws Exception {
        // Solo los GET de páginas son públicos: cualquier otro método exige login.
        mockMvc.perform(post("/productos"))
                .andExpect(status().isUnauthorized());
    }
}