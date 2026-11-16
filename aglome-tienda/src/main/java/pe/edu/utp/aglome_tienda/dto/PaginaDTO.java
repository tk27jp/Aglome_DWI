package pe.edu.utp.aglome_tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/** Respuesta paginada con un formato estable para el frontend (no expone la clase Page de Spring). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaDTO<T> {
    private List<T> contenido;
    private int pagina;            // número de página actual, empieza en 0
    private int tamano;            // elementos por página
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;

    public static <T> PaginaDTO<T> desde(Page<T> page) {
        return new PaginaDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
