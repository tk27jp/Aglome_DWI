package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.CategoriaArbolDTO;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.CategoriaRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProductoBaseRepository productoBaseRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void crear_conDatosValidos_debeGuardarCategoria() {
        // Simular datos SIN error
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre("Melaminas");

        Categoria guardada = new Categoria(1L, "Melaminas", null, true);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(guardada);

        CategoriaDTO resultado = categoriaService.crear(request);

        // Verificar el test
        assertEquals("Melaminas", resultado.getNombre());
        assertTrue(resultado.getActiva());
    }

    @Test
    void crear_conPadreInexistente_debeLanzarExcepcion() {
        // Simular datos CON error (categoría padre que no existe)
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre("Melaminas 18mm");
        request.setPadreId(99L);

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Verificar el test
        assertThrows(RuntimeException.class, () -> categoriaService.crear(request));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminar_conIdInexistente_debeLanzarExcepcion() {
        // Simular datos CON error
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Verificar el test
        assertThrows(RuntimeException.class, () -> categoriaService.eliminar(99L));
    }

    @Test
        void crear_conImagenUrl_debeGuardarYDevolverLaImagen() {
    CategoriaRequest request = new CategoriaRequest();
    request.setNombre("Melaminas");
    request.setImagenUrl("https://ejemplo.com/melaminas.jpg");

    when(categoriaRepository.save(any(Categoria.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

    CategoriaDTO resultado = categoriaService.crear(request);

    assertEquals("https://ejemplo.com/melaminas.jpg", resultado.getImagenUrl());
}

    @Test
    void eliminar_sinProductosNiSubcategorias_debeMarcarlaComoEliminada() {
        // Simular datos SIN error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoBaseRepository.existsByCategoriaIdAndEliminadoFalse(1L)).thenReturn(false);
        when(categoriaRepository.existsByPadreIdAndEliminadoFalse(1L)).thenReturn(false);

        categoriaService.eliminar(1L);

        // Verificar el test
        assertTrue(categoria.getEliminado());
        verify(categoriaRepository).save(categoria);
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void eliminar_conProductosAsociados_debeLanzarConflictoYNoEliminar() {
        // Simular datos CON error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoBaseRepository.existsByCategoriaIdAndEliminadoFalse(1L)).thenReturn(true);

        // Verificar el test
        assertThrows(ConflictoException.class, () -> categoriaService.eliminar(1L));
        assertFalse(categoria.getEliminado());
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminar_conSubcategorias_debeLanzarConflictoYNoEliminar() {
        // Simular datos CON error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoBaseRepository.existsByCategoriaIdAndEliminadoFalse(1L)).thenReturn(false);
        when(categoriaRepository.existsByPadreIdAndEliminadoFalse(1L)).thenReturn(true);

        // Verificar el test
        assertThrows(ConflictoException.class, () -> categoriaService.eliminar(1L));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_conCategoriaEliminada_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        categoria.setEliminado(true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Verificar el test
        assertThrows(RecursoNoEncontradoException.class, () -> categoriaService.obtenerPorId(1L));
    }

    @Test
    void arbol_debeAnidarLasSubcategoriasBajoSuPadreYOrdenarPorNombre() {
        // Simular datos SIN error
        Categoria tableros = new Categoria(3L, "Tableros", null, true);
        Categoria melaminas = new Categoria(1L, "Melaminas", null, true);
        Categoria melaminas18 = new Categoria(2L, "Melaminas 18mm", melaminas, true);
        when(categoriaRepository.findByEliminadoFalse()).thenReturn(List.of(tableros, melaminas18, melaminas));

        List<CategoriaArbolDTO> arbol = categoriaService.arbol();

        // Verificar el test
        assertEquals(2, arbol.size());
        assertEquals("Melaminas", arbol.get(0).getNombre());
        assertEquals("Tableros", arbol.get(1).getNombre());
        assertEquals(1, arbol.get(0).getHijas().size());
        assertEquals("Melaminas 18mm", arbol.get(0).getHijas().get(0).getNombre());
        assertTrue(arbol.get(1).getHijas().isEmpty());
    }

    @Test
    void arbol_sinCategorias_debeDevolverListaVacia() {
        // Simular datos SIN error
        when(categoriaRepository.findByEliminadoFalse()).thenReturn(List.of());

        // Verificar el test
        assertTrue(categoriaService.arbol().isEmpty());
    }
}
