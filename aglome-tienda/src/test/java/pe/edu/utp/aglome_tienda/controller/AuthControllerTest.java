package pe.edu.utp.aglome_tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.aglome_tienda.dto.AuthResponse;
import pe.edu.utp.aglome_tienda.dto.LoginRequest;
import pe.edu.utp.aglome_tienda.dto.RegistroRequest;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.security.SecurityConfig;
import pe.edu.utp.aglome_tienda.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void login_conCredencialesValidas_debeRetornar200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jonathan@test.com");
        request.setPassword("123456");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token-falso", "Jonathan Navarro", "jonathan@test.com", "CLIENTE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-falso"));
    }

    @Test
    void login_conCredencialesIncorrectas_debeRetornar401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jonathan@test.com");
        request.setPassword("incorrecta");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Email o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registro_sinEmail_debeRetornar400PorValidacion() throws Exception {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Jonathan Navarro");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}