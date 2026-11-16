package pe.edu.utp.aglome_tienda.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    public static class ControladorDePrueba {

        @GetMapping("/prueba/no-existe")
        public String noExiste() {
            throw new RecursoNoEncontradoException("Producto no encontrado");
        }

        @GetMapping("/prueba/conflicto")
        public String conflicto() {
            throw new ConflictoException("Ya existe una variante con ese SKU");
        }

        @GetMapping("/prueba/regla")
        public String regla() {
            throw new ReglaNegocioException("El pedido ya está cancelado");
        }

        @PostMapping("/prueba/validar")
        public String validar(@Valid @RequestBody CuerpoDePrueba cuerpo) {
            return "ok";
        }

        @GetMapping("/prueba/bloqueada")
        public String bloqueada() {
            throw new CuentaBloqueadaException("Cuenta bloqueada temporalmente");
        }

        @GetMapping("/prueba/credenciales")
        public String credenciales() {
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
        }
    }

    public record CuerpoDePrueba(@NotBlank(message = "El nombre es obligatorio") String nombre) { }

    @Test
    void recursoNoEncontrado_debeRetornar404ConElMensaje() throws Exception {
        mockMvc.perform(get("/prueba/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado"))
                .andExpect(jsonPath("$.path").value("/prueba/no-existe"));
    }

    @Test
    void conflicto_debeRetornar409() throws Exception {
        mockMvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una variante con ese SKU"));
    }

    @Test
    void reglaDeNegocio_debeRetornar400() throws Exception {
        mockMvc.perform(get("/prueba/regla"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El pedido ya está cancelado"));
    }

    @Test
    void validacion_debeRetornar400IndicandoQueCampoFallo() throws Exception {
        mockMvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("El nombre es obligatorio"));
    }

    @Test
    void cuerpoQueNoEsJson_debeRetornar400() throws Exception {
        mockMvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la petición no es un JSON válido"));
    }

    @Test
    void credencialesInvalidas_debeRetornar401() throws Exception {
        mockMvc.perform(get("/prueba/credenciales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void cuentaBloqueada_debeRetornar423() throws Exception {
        mockMvc.perform(get("/prueba/bloqueada"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.mensaje").value("Cuenta bloqueada temporalmente"));
    }
}
