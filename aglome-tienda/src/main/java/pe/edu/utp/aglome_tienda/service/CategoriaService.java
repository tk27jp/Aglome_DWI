package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.CategoriaRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // ── lecturas: usadas por la tienda pública ──────────────────────
    public List<CategoriaDTO> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    public CategoriaDTO obtenerPorId(Long id) {
        Categoria categoria = buscarOFallar(id);
        return mapearADto(categoria);
    }

    // ── escrituras: solo las va a llamar el panel admin ─────────────
    public CategoriaDTO crear(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        aplicarDatos(categoria, request);
        Categoria guardada = categoriaRepository.save(categoria);
        return mapearADto(guardada);
    }

    public CategoriaDTO actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarOFallar(id);
        aplicarDatos(categoria, request);
        Categoria actualizada = categoriaRepository.save(categoria);
        return mapearADto(actualizada);
    }

    public void eliminar(Long id) {
        Categoria categoria = buscarOFallar(id);
        categoriaRepository.delete(categoria);
    }

    // ── helpers privados ─────────────────────────────────────────────
    private Categoria buscarOFallar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
    }

    private void aplicarDatos(Categoria categoria, CategoriaRequest request) {
        categoria.setNombre(request.getNombre());
        if (request.getPadreId() != null) {
            Categoria padre = buscarOFallar(request.getPadreId());
            categoria.setPadre(padre);
        } else {
            categoria.setPadre(null);
        }
        if (categoria.getActiva() == null) {
            categoria.setActiva(true);
        }
    }

    private CategoriaDTO mapearADto(Categoria c) {
        return new CategoriaDTO(
                c.getId(),
                c.getNombre(),
                c.getPadre() != null ? c.getPadre().getId() : null,
                c.getActiva()
        );
    }
}