package pe.edu.utp.aglome_tienda.service;

import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.CategoriaArbolDTO;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.CategoriaRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoBaseRepository productoBaseRepository;

    // ── lecturas: usadas por la tienda pública ──────────────────────
    public List<CategoriaDTO> listarTodas() {
        return categoriaRepository.findByEliminadoFalse()
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    // categorías con sus subcategorías anidadas, para el menú
    public List<CategoriaArbolDTO> arbol() {
        List<Categoria> todas = new ArrayList<>(categoriaRepository.findByEliminadoFalse());
        todas.sort(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER));

        Map<Long, CategoriaArbolDTO> nodos = new LinkedHashMap<>();
        for (Categoria c : todas) {
            nodos.put(c.getId(), new CategoriaArbolDTO(c.getId(), c.getNombre(), c.getImagenUrl(), new ArrayList<>()));
        }

        List<CategoriaArbolDTO> raices = new ArrayList<>();
        for (Categoria c : todas) {
            CategoriaArbolDTO nodo = nodos.get(c.getId());
            CategoriaArbolDTO padre = c.getPadre() != null ? nodos.get(c.getPadre().getId()) : null;
            if (padre != null) {
                padre.getHijas().add(nodo);
            } else {
                raices.add(nodo);
            }
        }
        return raices;
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
        if (productoBaseRepository.existsByCategoriaIdAndEliminadoFalse(id)) {
            throw new ConflictoException("No se puede eliminar la categoría porque tiene productos asociados");
        }
        if (categoriaRepository.existsByPadreIdAndEliminadoFalse(id)) {
            throw new ConflictoException("No se puede eliminar la categoría porque tiene subcategorías");
        }
        // borrado lógico
        categoria.setEliminado(true);
        categoriaRepository.save(categoria);
    }

    // ── helpers privados ─────────────────────────────────────────────
    private Categoria buscarOFallar(Long id) {
        return categoriaRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getEliminado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con id: " + id));
    }

    private void aplicarDatos(Categoria categoria, CategoriaRequest request) {
        categoria.setNombre(request.getNombre());
        categoria.setImagenUrl(request.getImagenUrl());
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
                c.getActiva(),
                c.getImagenUrl()
        );
    }
}