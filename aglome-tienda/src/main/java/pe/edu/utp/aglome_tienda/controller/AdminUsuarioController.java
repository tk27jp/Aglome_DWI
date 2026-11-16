package pe.edu.utp.aglome_tienda.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.aglome_tienda.dto.CrearUsuarioRequest;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.service.UsuarioAdminService;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioAdminService usuarioAdminService;

    // GET /api/admin/usuarios            → todas las cuentas
    // GET /api/admin/usuarios?rol=VENDEDOR → solo las de ese rol
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Rol rol) {
        return ResponseEntity.ok(usuarioAdminService.listar(rol));
    }

    // POST /api/admin/usuarios → crea una cuenta de personal (ADMIN o VENDEDOR)
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioAdminService.crear(request));
    }
}
