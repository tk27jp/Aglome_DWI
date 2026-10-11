package pe.edu.utp.aglome_tienda.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class AdminInicial implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = "admin@aglome.com";

        if (!usuarioRepository.existsByEmail(email)) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador Aglome");
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode("Admin123"));
            admin.setRol(Rol.ADMIN);
            usuarioRepository.save(admin);
        }
    }
}