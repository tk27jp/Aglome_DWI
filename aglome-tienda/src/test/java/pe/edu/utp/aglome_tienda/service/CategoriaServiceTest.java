package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.CategoriaDTO;
import pe.edu.utp.aglome_tienda.dto.CategoriaRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.repository.CategoriaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;

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
}