package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.aglome_tienda.dto.CrearUsuarioRequest;
import pe.edu.utp.aglome_tienda.dto.PerfilDTO;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Rol;
import pe.edu.utp.aglome_tienda.model.Usuario;
import pe.edu.utp.aglome_tienda.repository.UsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;

/** Gestión de cuentas del personal desde el panel de administración. */
@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PerfilDTO crear(CrearUsuarioRequest request) {
        if (request.getRol() == Rol.CLIENTE) {
            throw new ReglaNegocioException(
                    "Desde el panel solo se crean cuentas de personal (ADMIN o VENDEDOR). Los clientes se registran solos");
        }
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword())); // nunca en texto plano
        usuario.setRol(request.getRol());

        return aDto(usuarioRepository.save(usuario));
    }

    /** Todas las cuentas ordenadas por nombre; con rol, solo las de ese rol. */
    @Transactional(readOnly = true)
    public List<PerfilDTO> listar(Rol rol) {
        Sort porNombre = Sort.by("nombre");
        List<Usuario> usuarios = rol == null
                ? usuarioRepository.findAll(porNombre)
                : usuarioRepository.findByRol(rol, porNombre);
        return usuarios.stream().map(this::aDto).collect(Collectors.toList());
    }

    private PerfilDTO aDto(Usuario u) {
        return new PerfilDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol().name());
    }
}
