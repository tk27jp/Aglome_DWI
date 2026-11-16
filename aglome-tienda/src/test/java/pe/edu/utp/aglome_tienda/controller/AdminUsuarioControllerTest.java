package pe.edu.utp.aglome_tienda.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.CrearUsuarioRequest;
import pe.edu.utp.aglome_tienda.dto.PerfilDTO;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.UsuarioAdminService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUsuarioController.class)
@Import(SecurityConfig.class)
class AdminUsuarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UsuarioAdminService usuarioAdminService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private static final String VALIDO = """
            {"nombre":"Elton Vendedor","email":"elton@aglome.com","password":"secreto1","rol":"VENDEDOR"}""";

    // ---------- listar ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void listar_sinFiltro_debeRetornar200() throws Exception {
        when(usuarioAdminService.listar(isNull())).thenReturn(List.of(
                new PerfilDTO(1L, "jhonatan", "jhonatan@test.com", "CLIENTE"),
                new PerfilDTO(2L, "Elton", "elton@aglome.com", "VENDEDOR")));

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].rol").value("VENDEDOR"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listar_conRol_pasaElRolAlServicio() throws Exception {
        when(usuarioAdminService.listar(eq(Rol.VENDEDOR))).thenReturn(List.of(
                new PerfilDTO(2L, "Elton", "elton@aglome.com", "VENDEDOR")));

        mockMvc.perform(get("/api/admin/usuarios").param("rol", "VENDEDOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("elton@aglome.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listar_conRolQueNoExiste_debeRetornar400() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").param("rol", "SUPERUSUARIO"))
                .andExpect(status().isBadRequest());
    }

    // ---------- crear ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conDatosValidos_debeRetornar201SinLaContrasena() throws Exception {
        when(usuarioAdminService.crear(any(CrearUsuarioRequest.class)))
                .thenReturn(new PerfilDTO(7L, "Elton Vendedor", "elton@aglome.com", "VENDEDOR"));

        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conEmailMalFormado_debeRetornar400() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("elton@aglome.com", "no-es-un-email")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioAdminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conContrasenaCorta_debeRetornar400() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("secreto1", "123")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioAdminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_sinRol_debeRetornar400() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Elton\",\"email\":\"elton@aglome.com\",\"password\":\"secreto1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conRolQueNoExiste_debeRetornar400() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("VENDEDOR", "SUPERUSUARIO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_unCliente_debeRetornar400() throws Exception {
        when(usuarioAdminService.crear(any(CrearUsuarioRequest.class)))
                .thenThrow(new ReglaNegocioException("Desde el panel solo se crean cuentas de personal"));

        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("VENDEDOR", "CLIENTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_conEmailRepetido_debeRetornar409() throws Exception {
        when(usuarioAdminService.crear(any(CrearUsuarioRequest.class)))
                .thenThrow(new ConflictoException("Ya existe una cuenta con ese email"));

        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict());
    }

    // ---------- seguridad: solo ADMIN (el vendedor no gestiona cuentas) ----------

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void comoVendedor_debeRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioAdminService);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void comoCliente_debeRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/usuarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isUnauthorized());
    }
}
