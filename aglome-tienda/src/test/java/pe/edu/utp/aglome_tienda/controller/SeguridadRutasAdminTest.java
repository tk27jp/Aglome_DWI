package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.*;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre TODAS las rutas /api/admin y comprueba quién puede entrar:
 *   sin sesión → 401 · CLIENTE → 403 · VENDEDOR → solo pedidos y stock · ADMIN → todo.
 * Si algún día agregas una ruta o un controlador admin nuevo y no lo anotas aquí, el último test falla.
 */
@WebMvcTest({
        AdminCategoriaController.class, AdminDashboardController.class, AdminMarcaController.class,
        AdminPedidoController.class, AdminProductoController.class, AdminReporteController.class,
        AdminStockController.class, AdminUsuarioController.class, AdminVarianteController.class})
@Import(SecurityConfig.class)
class SeguridadRutasAdminTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CategoriaService categoriaService;
    @MockitoBean private MarcaService marcaService;
    @MockitoBean private ProductoBaseService productoBaseService;
    @MockitoBean private VarianteService varianteService;
    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private ReporteService reporteService;
    @MockitoBean private MovimientoStockService movimientoStockService;
    @MockitoBean private UsuarioAdminService usuarioAdminService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private static final String UUID_EJEMPLO = "00000000-0000-0000-0000-000000000001";

    /** vendedorPuede = true: la ruta es de la operación diaria (ADMIN y VENDEDOR); false: solo ADMIN. */
    record Ruta(HttpMethod metodo, String path, boolean vendedorPuede) {
        @Override
        public String toString() {
            return metodo + " " + path + (vendedorPuede ? "  [ADMIN + VENDEDOR]" : "  [solo ADMIN]");
        }
    }

    static Stream<Ruta> rutas() {
        return Stream.of(
                // catálogo: solo administrador
                new Ruta(HttpMethod.POST, "/api/admin/categorias", false),
                new Ruta(HttpMethod.PUT, "/api/admin/categorias/1", false),
                new Ruta(HttpMethod.DELETE, "/api/admin/categorias/1", false),
                new Ruta(HttpMethod.POST, "/api/admin/marcas", false),
                new Ruta(HttpMethod.PUT, "/api/admin/marcas/1", false),
                new Ruta(HttpMethod.DELETE, "/api/admin/marcas/1", false),
                new Ruta(HttpMethod.GET, "/api/admin/productos", false),
                new Ruta(HttpMethod.POST, "/api/admin/productos", false),
                new Ruta(HttpMethod.PUT, "/api/admin/productos/" + UUID_EJEMPLO, false),
                new Ruta(HttpMethod.DELETE, "/api/admin/productos/" + UUID_EJEMPLO, false),
                new Ruta(HttpMethod.POST, "/api/admin/variantes", false),
                new Ruta(HttpMethod.PUT, "/api/admin/variantes/" + UUID_EJEMPLO, false),
                new Ruta(HttpMethod.DELETE, "/api/admin/variantes/" + UUID_EJEMPLO, false),
                // dinero y gestión: solo administrador
                new Ruta(HttpMethod.GET, "/api/admin/dashboard", false),
                new Ruta(HttpMethod.GET, "/api/admin/reportes/ventas-por-categoria", false),
                new Ruta(HttpMethod.GET, "/api/admin/reportes/ventas-por-mes", false),
                new Ruta(HttpMethod.GET, "/api/admin/reportes/productos-mas-vendidos", false),
                new Ruta(HttpMethod.GET, "/api/admin/reportes/stock-bajo", false),
                new Ruta(HttpMethod.GET, "/api/admin/usuarios", false),
                new Ruta(HttpMethod.POST, "/api/admin/usuarios", false),
                // operación diaria: administrador y vendedor
                new Ruta(HttpMethod.GET, "/api/admin/pedidos", true),
                new Ruta(HttpMethod.GET, "/api/admin/pedidos/" + UUID_EJEMPLO, true),
                new Ruta(HttpMethod.PATCH, "/api/admin/pedidos/" + UUID_EJEMPLO + "/estado", true),
                new Ruta(HttpMethod.GET, "/api/admin/stock/movimientos", true),
                new Ruta(HttpMethod.POST, "/api/admin/stock/ajustes", true));
    }

    private MockHttpServletRequestBuilder pedir(Ruta ruta) {
        MockHttpServletRequestBuilder builder = request(ruta.metodo(), ruta.path());
        if (ruta.path().endsWith("/estado")) {
            builder.param("estado", "ENVIADO");
        }
        if (ruta.metodo() == HttpMethod.POST || ruta.metodo() == HttpMethod.PUT || ruta.metodo() == HttpMethod.PATCH) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return builder;
    }

    private void assertAccesoPermitido(MockHttpServletRequestBuilder peticion, String quien) throws Exception {
        int estado = mockMvc.perform(peticion).andReturn().getResponse().getStatus();
        // pasó la seguridad: puede dar 200, 400 (cuerpo vacío)... pero nunca 401 ni 403
        assertNotEquals(401, estado, quien + " no debería recibir 401");
        assertNotEquals(403, estado, quien + " no debería recibir 403");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rutas")
    void sinSesion_siempreRetorna401(Ruta ruta) throws Exception {
        mockMvc.perform(pedir(ruta)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rutas")
    void cliente_siempreRetorna403(Ruta ruta) throws Exception {
        mockMvc.perform(pedir(ruta).with(user("cliente@test.com").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rutas")
    void vendedor_soloEntraALaOperacionDiaria(Ruta ruta) throws Exception {
        MockHttpServletRequestBuilder peticion = pedir(ruta).with(user("vendedor@test.com").roles("VENDEDOR"));
        if (ruta.vendedorPuede()) {
            assertAccesoPermitido(peticion, "El vendedor");
        } else {
            mockMvc.perform(peticion).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rutas")
    void administrador_entraATodo(Ruta ruta) throws Exception {
        assertAccesoPermitido(pedir(ruta).with(user("admin@test.com").roles("ADMIN")), "El administrador");
    }

    @Test
    void todasLasRutasAdminDelProyectoEstanEnLaTabla() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<String> controladoresAdmin = new HashSet<>();
        int endpoints = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("pe.edu.utp.aglome_tienda.controller")) {
            Class<?> clase = Class.forName(bd.getBeanClassName());
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(clase, RequestMapping.class);
            if (base != null && base.path().length > 0 && base.path()[0].startsWith("/api/admin")) {
                controladoresAdmin.add(clase.getSimpleName());
                for (Method m : clase.getDeclaredMethods()) {
                    if (AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)) {
                        endpoints++;
                    }
                }
            }
        }

        assertEquals(Set.of("AdminCategoriaController", "AdminDashboardController", "AdminMarcaController",
                        "AdminPedidoController", "AdminProductoController", "AdminReporteController",
                        "AdminStockController", "AdminUsuarioController", "AdminVarianteController"),
                controladoresAdmin,
                "Hay un controlador admin nuevo: agrégalo a @WebMvcTest y a la tabla de rutas");
        assertEquals(rutas().count(), endpoints,
                "Hay una ruta admin nueva (o quitada): actualiza la tabla de rutas");
    }
}
