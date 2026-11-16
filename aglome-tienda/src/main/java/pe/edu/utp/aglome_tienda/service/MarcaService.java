package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.MarcaDTO;
import pe.edu.utp.aglome_tienda.dto.MarcaRequest;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.repository.MarcaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository marcaRepository;
    private final ProductoBaseRepository productoBaseRepository;

    // ── lecturas: tienda pública ─────────────────────────────────────
    public List<MarcaDTO> listarTodas() {
        return marcaRepository.findAll(Sort.by("nombre"))
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public MarcaDTO obtenerPorId(Long id) {
        return mapearADto(buscarOFallar(id));
    }

    // ── escrituras: solo panel admin ─────────────────────────────────
    public MarcaDTO crear(MarcaRequest request) {
        String nombre = request.getNombre().trim();
        if (marcaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una marca con ese nombre");
        }
        Marca marca = new Marca();
        marca.setNombre(nombre);
        marca.setImagenUrl(request.getImagenUrl());
        return mapearADto(marcaRepository.save(marca));
    }

    public MarcaDTO actualizar(Long id, MarcaRequest request) {
        Marca marca = buscarOFallar(id);
        String nombre = request.getNombre().trim();
        if (marcaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException("Ya existe otra marca con ese nombre");
        }
        marca.setNombre(nombre);
        marca.setImagenUrl(request.getImagenUrl());
        return mapearADto(marcaRepository.save(marca));
    }

    public void eliminar(Long id) {
        Marca marca = buscarOFallar(id);
        if (productoBaseRepository.existsByMarcaIdAndEliminadoFalse(id)) {
            throw new ConflictoException("No se puede eliminar la marca porque tiene productos asociados");
        }
        marcaRepository.delete(marca);
    }

    // ── helpers privados ─────────────────────────────────────────────
    private Marca buscarOFallar(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca no encontrada con id: " + id));
    }

    private MarcaDTO mapearADto(Marca m) {
        return new MarcaDTO(m.getId(), m.getNombre(), m.getImagenUrl());
    }
}
