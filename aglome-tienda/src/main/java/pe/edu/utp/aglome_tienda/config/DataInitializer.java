package pe.edu.utp.aglome_tienda.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByEmail("admin@aglome.com")) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador Aglome");
            admin.setEmail("admin@aglome.com");
            admin.setPassword(passwordEncoder.encode("Admin123"));
            admin.setRol(Rol.ADMIN);
            usuarioRepository.save(admin);
        }
    }
}