package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.AuthResponse;
import pe.edu.utp.aglome_tienda.dto.LoginRequest;
import pe.edu.utp.aglome_tienda.dto.RegistroRequest;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;
import pe.edu.utp.aglome_tienda.security.JwtService;
import pe.edu.utp.aglome_tienda.dto.PerfilDTO;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword())); // nunca en texto plano
        usuario.setRol(Rol.CLIENTE); // todo registro nuevo entra como cliente, no admin

        usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Email o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }

    public PerfilDTO obtenerPerfil(String email) {
    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    return new PerfilDTO(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }
}