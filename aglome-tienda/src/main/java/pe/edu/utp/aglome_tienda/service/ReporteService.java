package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.VentaCategoriaDTO;
import pe.edu.utp.aglome_tienda.repository.DetallePedidoRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final DetallePedidoRepository detallePedidoRepository;

    public List<VentaCategoriaDTO> ventasPorCategoria() {
        return detallePedidoRepository.unidadesVendidasPorCategoria()
                .stream()
                .map(fila -> new VentaCategoriaDTO((String) fila[0], ((Number) fila[1]).longValue()))
                .collect(Collectors.toList());
    }
}