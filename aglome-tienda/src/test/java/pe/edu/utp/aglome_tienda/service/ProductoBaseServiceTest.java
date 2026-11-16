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
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.MarcaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;

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
    private MarcaRepository marcaRepository;
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
        request.setCategoriaId(1L);
        request.setPublicado(true);
    }

    @Test
    void crear_conDatosValidos_debeGuardarYRetornarProducto() {
        // Simular datos SIN error
        ProductoBase productoGuardado = new ProductoBase(
                UUID.randomUUID(), "Melamina Azul Marino", "Aporta elegancia ejecutiva",
                new Marca(1L, "Aglome"), categoria, true
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
        RecursoNoEncontradoException excepcion = assertThrows(RecursoNoEncontradoException.class,
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

    @Test
        void crear_conImagenUrl_debeGuardarYDevolverLaImagen() {
            request.setImagenUrl("https://ejemplo.com/melamina.jpg");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(productoBaseRepository.save(any(ProductoBase.class)))
                    .thenAnswer(invocacion -> invocacion.getArgument(0));
            when(varianteService.listarPorProducto(any())).thenReturn(java.util.List.of());

        ProductoBaseDTO resultado = productoBaseService.crear(request);

            assertEquals("https://ejemplo.com/melamina.jpg", resultado.getImagenUrl());
    }

    @Test
    void crear_conMarcaExistente_debeAsociarLaMarcaYDevolverSuNombre() {
        // Simular datos SIN error
        request.setMarcaId(5L);
        Marca marca = new Marca(5L, "Aglome");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(marcaRepository.findById(5L)).thenReturn(Optional.of(marca));
        when(productoBaseRepository.save(any(ProductoBase.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(varianteService.listarPorProducto(any())).thenReturn(java.util.List.of());

        ProductoBaseDTO resultado = productoBaseService.crear(request);

        // Verificar el test
        assertEquals("Aglome", resultado.getMarca());
        assertEquals(5L, resultado.getMarcaId());
    }

    @Test
    void crear_conMarcaInexistente_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error (la marca no existe)
        request.setMarcaId(99L);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(marcaRepository.findById(99L)).thenReturn(Optional.empty());

        // Verificar el test
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> productoBaseService.crear(request));
        assertEquals("Marca no encontrada con id: 99", ex.getMessage());
        verify(productoBaseRepository, never()).save(any());
    }

    @Test
    void eliminar_debeMarcarElProductoYSusVariantesSinBorrarlos() {
        // Simular datos SIN error
        UUID id = UUID.randomUUID();
        ProductoBase producto = new ProductoBase(id, "Melamina", "desc", null, categoria, true);
        when(productoBaseRepository.findById(id)).thenReturn(Optional.of(producto));

        productoBaseService.eliminar(id);

        // Verificar el test
        assertEquals(true, producto.getEliminado());
        verify(productoBaseRepository).save(producto);
        verify(varianteService).eliminarPorProducto(id);
        verify(productoBaseRepository, never()).delete(any(ProductoBase.class));
    }

    @Test
    void obtenerPorId_conProductoEliminado_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error
        UUID id = UUID.randomUUID();
        ProductoBase producto = new ProductoBase(id, "Melamina", "desc", null, categoria, true);
        producto.setEliminado(true);
        when(productoBaseRepository.findById(id)).thenReturn(Optional.of(producto));

        // Verificar el test
        assertThrows(RecursoNoEncontradoException.class, () -> productoBaseService.obtenerPorId(id));
    }

    @Test
    void crear_conCategoriaEliminada_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error
        categoria.setEliminado(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Verificar el test
        assertThrows(RecursoNoEncontradoException.class, () -> productoBaseService.crear(request));
        verify(productoBaseRepository, never()).save(any());
    }
}
