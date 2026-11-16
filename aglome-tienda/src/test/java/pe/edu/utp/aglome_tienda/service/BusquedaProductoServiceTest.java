package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaProductoServiceTest {

    @Mock private ProductoBaseRepository productoBaseRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private VarianteService varianteService;

    @InjectMocks
    private ProductoBaseService productoBaseService;

    @Test
    void buscarPorNombre_conCoincidencias_debeRetornarLosProductos() {
        // Simular datos SIN error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(UUID.randomUUID(), "Melamina Azul Marino",
                "desc", new Marca(1L, "Aglome"), categoria, true);
        when(productoBaseRepository.buscarPublicadosPorNombre(eq("mela"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(producto), PageRequest.of(0, 12), 1));

        PaginaDTO<ProductoBaseDTO> resultado = productoBaseService.buscarPorNombre("mela", 0, 12, "nombre", "asc");

        // Verificar el test
        assertEquals(1, resultado.getContenido().size());
        assertEquals("Melamina Azul Marino", resultado.getContenido().get(0).getNombre());
        assertEquals("Melaminas", resultado.getContenido().get(0).getCategoriaNombre());
    }

    @Test
    void buscarPorNombre_sinCoincidencias_debeRetornarListaVacia() {
        // Simular caso sin resultados
        when(productoBaseRepository.buscarPublicadosPorNombre(eq("xyz"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PaginaDTO<ProductoBaseDTO> resultado = productoBaseService.buscarPorNombre("xyz", 0, 12, "nombre", "asc");

        // Verificar el test
        assertTrue(resultado.getContenido().isEmpty());
        assertEquals(0, resultado.getTotalElementos());
    }
}