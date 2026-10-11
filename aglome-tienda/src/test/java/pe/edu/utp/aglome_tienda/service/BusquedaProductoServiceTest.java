package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
                "desc", "Aglome", categoria, true);
        when(productoBaseRepository.buscarPublicadosPorNombre("mela")).thenReturn(List.of(producto));

        List<ProductoBaseDTO> resultado = productoBaseService.buscarPorNombre("mela");

        // Verificar el test
        assertEquals(1, resultado.size());
        assertEquals("Melamina Azul Marino", resultado.get(0).getNombre());
        assertEquals("Melaminas", resultado.get(0).getCategoriaNombre());
    }

    @Test
    void buscarPorNombre_sinCoincidencias_debeRetornarListaVacia() {
        // Simular caso sin resultados
        when(productoBaseRepository.buscarPublicadosPorNombre("xyz")).thenReturn(List.of());

        List<ProductoBaseDTO> resultado = productoBaseService.buscarPorNombre("xyz");

        // Verificar el test
        assertTrue(resultado.isEmpty());
    }
}