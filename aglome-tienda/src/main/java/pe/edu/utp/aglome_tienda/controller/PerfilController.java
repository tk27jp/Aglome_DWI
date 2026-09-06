package pe.edu.utp.aglome_tienda.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.aglome_tienda.service.AuthService;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<?> miPerfil(Authentication authentication) {
        try {
            String email = authentication.getName(); // viene del token, gracias al JwtAuthFilter
            return ResponseEntity.ok(authService.obtenerPerfil(email));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}