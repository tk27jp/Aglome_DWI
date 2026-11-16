package pe.edu.utp.aglome_tienda.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @InjectMocks private JwtAuthFilter filtro;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain cadena = new MockFilterChain();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinHeaderAuthorization_dejaPasarSinAutenticar() throws ServletException, IOException {
        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest(), "la petición debe seguir su camino");
        verifyNoInteractions(jwtService);
    }

    @Test
    void headerQueNoEsBearer_dejaPasarSinAutenticar() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
        verifyNoInteractions(jwtService);
    }

    @Test
    void tokenValido_autenticaConElEmailYElRolDelToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer token-bueno");
        when(jwtService.esTokenValido("token-bueno")).thenReturn(true);
        when(jwtService.extraerEmail("token-bueno")).thenReturn("elton@aglome.com");
        when(jwtService.extraerRol("token-bueno")).thenReturn("VENDEDOR");

        filtro.doFilter(request, response, cadena);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("elton@aglome.com", auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_VENDEDOR", auth.getAuthorities().iterator().next().getAuthority());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void tokenInvalido_noAutenticaPeroLaPeticionSigue() throws ServletException, IOException {
        // la que responde 401 después es la seguridad de Spring, no este filtro
        request.addHeader("Authorization", "Bearer token-falso");
        when(jwtService.esTokenValido("token-falso")).thenReturn(false);

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
        verify(jwtService, never()).extraerEmail(anyString());
        verify(jwtService, never()).extraerRol(anyString());
    }

    @Test
    void bearerSinNadaDespues_noAutentica() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer ");
        when(jwtService.esTokenValido("")).thenReturn(false);

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }
}
