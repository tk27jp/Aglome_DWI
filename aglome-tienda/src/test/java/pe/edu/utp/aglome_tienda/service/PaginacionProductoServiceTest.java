package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaginacionProductoServiceTest {

    @Mock private ProductoBaseRepository productoBaseRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private VarianteService varianteService;

    @InjectMocks
    private ProductoBaseService productoBaseService;

    @Test
    void listarPublicados_debeArmarLaPaginacionYDevolverLosDatosDeLaPagina() {
        // Simular datos SIN error: 25 productos en total, se pide la página 1 de 10
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(UUID.randomUUID(), "Melamina Azul", "desc", new Marca(1L, "Aglome"), categoria, true);
        when(productoBaseRepository.findByPublicadoTrueAndEliminadoFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(producto), PageRequest.of(1, 10), 25));

        PaginaDTO<ProductoBaseDTO> resultado = productoBaseService.listarPublicados(1, 10, "marca", "desc");

        // Verificar el test
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productoBaseRepository).findByPublicadoTrueAndEliminadoFalse(captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("marca.nombre").getDirection());

        assertEquals(25, resultado.getTotalElementos());
        assertEquals(3, resultado.getTotalPaginas());
        assertEquals(1, resultado.getPagina());
        assertFalse(resultado.isPrimera());
        assertFalse(resultado.isUltima());
    }

    @Test
    void listarPublicados_conCampoDeOrdenNoPermitido_debeLanzarReglaNegocio() {
        // Simular datos CON error (no se puede ordenar por un campo cualquiera)
        assertThrows(ReglaNegocioException.class,
                () -> productoBaseService.listarPublicados(0, 12, "password", "asc"));
        verify(productoBaseRepository, never()).findByPublicadoTrueAndEliminadoFalse(any(Pageable.class));
    }

    @Test
    void listarPublicados_conTamanoMayorAlMaximo_debeLanzarReglaNegocio() {
        // Simular datos CON error (piden 1000 productos de golpe)
        assertThrows(ReglaNegocioException.class,
                () -> productoBaseService.listarPublicados(0, 1000, "nombre", "asc"));
        verify(productoBaseRepository, never()).findByPublicadoTrueAndEliminadoFalse(any(Pageable.class));
    }

    @Test
    void listarPublicados_conPaginaNegativaODireccionInvalida_debeLanzarReglaNegocio() {
        // Simular datos CON error
        assertThrows(ReglaNegocioException.class,
                () -> productoBaseService.listarPublicados(-1, 12, "nombre", "asc"));
        assertThrows(ReglaNegocioException.class,
                () -> productoBaseService.listarPublicados(0, 12, "nombre", "arriba"));
        verify(productoBaseRepository, never()).findByPublicadoTrueAndEliminadoFalse(any(Pageable.class));
    }
}
