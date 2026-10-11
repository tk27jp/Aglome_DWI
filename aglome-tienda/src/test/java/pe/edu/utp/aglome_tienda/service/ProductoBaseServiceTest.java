package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoBaseServiceTest {

    @Mock
    private ProductoBaseRepository productoBaseRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private VarianteService varianteService;

    @InjectMocks
    private ProductoBaseService productoBaseService;

    private Categoria categoria;
    private ProductoBaseRequest request;

    @BeforeEach
    void setUp() {
        categoria = new Categoria(1L, "Melaminas", null, true);
        request = new ProductoBaseRequest();
        request.setNombre("Melamina Azul Marino");
        request.setDescripcion("Aporta elegancia ejecutiva");
        request.setMarca("Aglome");
        request.setCategoriaId(1L);
        request.setPublicado(true);
    }

    @Test
    void crear_conDatosValidos_debeGuardarYRetornarProducto() {
        // Simular datos SIN error
        ProductoBase productoGuardado = new ProductoBase(
                UUID.randomUUID(), "Melamina Azul Marino", "Aporta elegancia ejecutiva",
                "Aglome", categoria, true
        );

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoBaseRepository.save(any(ProductoBase.class))).thenReturn(productoGuardado);
        when(varianteService.listarPorProducto(any())).thenReturn(java.util.List.of());

        ProductoBaseDTO resultado = productoBaseService.crear(request);

        // Verificar el test
        assertNotNull(resultado);
        assertEquals("Melamina Azul Marino", resultado.getNombre());
        assertEquals("Melaminas", resultado.getCategoriaNombre());
        verify(productoBaseRepository, times(1)).save(any(ProductoBase.class));
    }

    @Test
    void crear_conCategoriaInexistente_debeLanzarExcepcion() {
        // Simular datos CON error (categoría que no existe)
        request.setCategoriaId(99L);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Verificar el test
        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> productoBaseService.crear(request));

        assertEquals("Categoría no encontrada con id: 99", excepcion.getMessage());
        verify(productoBaseRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_conIdInexistente_debeLanzarExcepcion() {
        // Simular datos CON error (producto que no existe)
        UUID idInventado = UUID.randomUUID();
        when(productoBaseRepository.findById(idInventado)).thenReturn(Optional.empty());

        // Verificar el test
        assertThrows(RuntimeException.class,
                () -> productoBaseService.obtenerPorId(idInventado));
    }
}