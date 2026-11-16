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
import pe.edu.utp.aglome_tienda.dto.CambioContrasenaRequest;
import pe.edu.utp.aglome_tienda.exception.CuentaBloqueadaException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.dto.PerfilDTO;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerfilController.class)
@Import(SecurityConfig.class)
class PerfilControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void miPerfil_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void miPerfil_conToken_debeRetornar200ConSusDatos() throws Exception {
        when(authService.obtenerPerfil("jhonatan@test.com"))
                .thenReturn(new PerfilDTO(1L, "Jhonatan", "jhonatan@test.com", "CLIENTE"));

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jhonatan@test.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @WithMockUser(username = "fernando@test.com")
    void miPerfil_conUsuarioInexistente_debeRetornar404() throws Exception {
        when(authService.obtenerPerfil("fernando@test.com"))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isNotFound());
    }

    @Test
    void miPerfil_conTokenFalso_debeRetornar401() throws Exception {
        mockMvc.perform(get("/api/perfil").header("Authorization", "Bearer token-inventado"))
                .andExpect(status().isUnauthorized());
    }

    private static String cuerpo(String actual, String nueva) {
        return "{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}";
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cambiarContrasena_conDatosValidos_debeRetornar204() throws Exception {
        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Vieja123", "Nueva12345")))
                .andExpect(status().isNoContent());

        verify(authService).cambiarContrasena(eq("jhonatan@test.com"), any(CambioContrasenaRequest.class));
    }

    @Test
    void cambiarContrasena_sinToken_debeRetornar401() throws Exception {
        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Vieja123", "Nueva12345")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cambiarContrasena_conNuevaCorta_debeRetornar400() throws Exception {
        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Vieja123", "Ab1")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cambiarContrasena_conNuevaSinNumeros_debeRetornar400() throws Exception {
        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Vieja123", "SoloLetrasAqui")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("contrasenaNueva"));
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cambiarContrasena_sinLaActual_debeRetornar400() throws Exception {
        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaNueva\":\"Nueva12345\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cambiarContrasena_conLaActualIncorrecta_debeRetornar400() throws Exception {
        doThrow(new ReglaNegocioException("La contraseña actual no es correcta"))
                .when(authService).cambiarContrasena(eq("jhonatan@test.com"), any(CambioContrasenaRequest.class));

        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("equivocada", "Nueva12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta"));
    }

    @Test
    @WithMockUser(username = "jhonatan@test.com")
    void cambiarContrasena_conCuentaBloqueada_debeRetornar423() throws Exception {
        doThrow(new CuentaBloqueadaException("Cuenta bloqueada temporalmente"))
                .when(authService).cambiarContrasena(eq("jhonatan@test.com"), any(CambioContrasenaRequest.class));

        mockMvc.perform(put("/api/perfil/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("equivocada", "Nueva12345")))
                .andExpect(status().isLocked());
    }
}
