package pe.edu.utp.aglome_tienda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    static final String EMAIL_POR_DEFECTO = "admin@aglome.com";
    static final String PASSWORD_POR_DEFECTO = "Admin123";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${ADMIN_EMAIL:" + EMAIL_POR_DEFECTO + "}") String email,
                           @Value("${ADMIN_PASSWORD:" + PASSWORD_POR_DEFECTO + "}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        // una variable de entorno vacía cuenta como "no definida"
        this.email = StringUtils.hasText(email) ? email.trim() : EMAIL_POR_DEFECTO;
        this.password = StringUtils.hasText(password) ? password : PASSWORD_POR_DEFECTO;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(email)) {
            return;   // ya existe: nunca se pisa su contraseña
        }

        if (PASSWORD_POR_DEFECTO.equals(password)) {
            log.warn("Se creó el administrador '{}' con la contraseña de desarrollo. "
                    + "Define ADMIN_PASSWORD o cámbiala con PUT /api/perfil/contrasena antes de publicar el sistema.", email);
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador Aglome");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRol(Rol.ADMIN);
        usuarioRepository.save(admin);
    }
}
