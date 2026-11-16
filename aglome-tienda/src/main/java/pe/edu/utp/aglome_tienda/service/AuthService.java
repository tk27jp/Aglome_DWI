package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.AuthResponse;
import pe.edu.utp.aglome_tienda.dto.CambioContrasenaRequest;
import pe.edu.utp.aglome_tienda.dto.LoginRequest;
import pe.edu.utp.aglome_tienda.dto.PerfilDTO;
import pe.edu.utp.aglome_tienda.dto.RegistroRequest;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.CredencialesInvalidasException;
import pe.edu.utp.aglome_tienda.exception.CuentaBloqueadaException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;
import pe.edu.utp.aglome_tienda.security.JwtService;

import java.time.Duration;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class AuthService {

    public static final int MAX_INTENTOS = 5;

    public static final int MINUTOS_BLOQUEO = 3;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword())); // nunca en texto plano
        usuario.setRol(Rol.CLIENTE);

        usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CredencialesInvalidasException("Email o contraseña incorrectos"));

        liberarSiYaPasoElBloqueo(usuario);
        if (estaBloqueada(usuario)) {
            throw cuentaBloqueada(usuario);
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            registrarIntentoFallido(usuario);
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
        }

        reiniciarIntentos(usuario);

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }

    public PerfilDTO obtenerPerfil(String email) {
        Usuario usuario = buscarPorEmail(email);
        return new PerfilDTO(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }

    public void cambiarContrasena(String email, CambioContrasenaRequest request) {
        Usuario usuario = buscarPorEmail(email);

        liberarSiYaPasoElBloqueo(usuario);
        if (estaBloqueada(usuario)) {
            throw cuentaBloqueada(usuario);
        }

        if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getPassword())) {
            registrarIntentoFallido(usuario);
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.getContrasenaNueva(), usuario.getPassword())) {
            throw new ReglaNegocioException("La contraseña nueva debe ser distinta de la actual");
        }

        usuario.setPassword(passwordEncoder.encode(request.getContrasenaNueva()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
    }


    private Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private boolean estaBloqueada(Usuario usuario) {
        return usuario.getBloqueadoHasta() != null && LocalDateTime.now().isBefore(usuario.getBloqueadoHasta());
    }

    private void liberarSiYaPasoElBloqueo(Usuario usuario) {
        if (usuario.getBloqueadoHasta() != null && !estaBloqueada(usuario)) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            usuarioRepository.save(usuario);
            throw cuentaBloqueada(usuario);
        }
        usuarioRepository.save(usuario);
    }

    private void reiniciarIntentos(Usuario usuario) {
        boolean habiaIntentos = usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0;
        if (habiaIntentos || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }
    }

    private CuentaBloqueadaException cuentaBloqueada(Usuario usuario) {
        long segundos = Math.max(1, Duration.between(LocalDateTime.now(), usuario.getBloqueadoHasta()).getSeconds());
        long minutos = (segundos + 59) / 60;
        return new CuentaBloqueadaException(
                "Cuenta bloqueada temporalmente por demasiados intentos fallidos. Intenta de nuevo en "
                        + minutos + (minutos == 1 ? " minuto" : " minutos"));
    }
}
