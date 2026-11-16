package pe.edu.utp.aglome_tienda.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.CambioContrasenaRequest;
import pe.edu.utp.aglome_tienda.service.AuthService;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<?> miPerfil(Authentication authentication) {
        String email = authentication.getName(); // viene del token, gracias al JwtAuthFilter
        return ResponseEntity.ok(authService.obtenerPerfil(email));
    }

    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(Authentication authentication,
                                                  @Valid @RequestBody CambioContrasenaRequest request) {
        authService.cambiarContrasena(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
