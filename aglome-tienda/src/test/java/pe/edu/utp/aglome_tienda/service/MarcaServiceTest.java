package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.MarcaDTO;
import pe.edu.utp.aglome_tienda.dto.MarcaRequest;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.repository.MarcaRepository;
import pe.edu.utp.aglome_tienda.repository.ProductoBaseRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarcaServiceTest {

    @Mock private MarcaRepository marcaRepository;
    @Mock private ProductoBaseRepository productoBaseRepository;

    @InjectMocks
    private MarcaService marcaService;

    private MarcaRequest request(String nombre) {
        MarcaRequest r = new MarcaRequest();
        r.setNombre(nombre);
        return r;
    }

    @Test
    void crear_conNombreNuevo_debeGuardarlaSinEspaciosSobrantes() {
        // Simular datos SIN error
        when(marcaRepository.existsByNombreIgnoreCase("Aglome")).thenReturn(false);
        when(marcaRepository.save(any(Marca.class))).thenAnswer(inv -> inv.getArgument(0));

        MarcaDTO resultado = marcaService.crear(request("  Aglome  "));

        // Verificar el test
        assertEquals("Aglome", resultado.getNombre());
        verify(marcaRepository, times(1)).save(any(Marca.class));
    }

    @Test
    void crear_conNombreRepetido_debeLanzarConflictoYNoGuardar() {
        // Simular datos CON error
        when(marcaRepository.existsByNombreIgnoreCase("Aglome")).thenReturn(true);

        // Verificar el test
        assertThrows(ConflictoException.class, () -> marcaService.crear(request("Aglome")));
        verify(marcaRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_conIdInexistente_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error
        when(marcaRepository.findById(99L)).thenReturn(Optional.empty());

        // Verificar el test
        assertThrows(RecursoNoEncontradoException.class, () -> marcaService.obtenerPorId(99L));
    }

    @Test
    void actualizar_conNombreDeOtraMarca_debeLanzarConflictoYNoGuardar() {
        // Simular datos CON error
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(new Marca(1L, "Aglome")));
        when(marcaRepository.existsByNombreIgnoreCaseAndIdNot("Duratex", 1L)).thenReturn(true);

        // Verificar el test
        assertThrows(ConflictoException.class, () -> marcaService.actualizar(1L, request("Duratex")));
        verify(marcaRepository, never()).save(any());
    }

    @Test
    void eliminar_conProductosAsociados_debeLanzarConflictoYNoBorrar() {
        // Simular datos CON error
        Marca marca = new Marca(1L, "Aglome");
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(productoBaseRepository.existsByMarcaIdAndEliminadoFalse(1L)).thenReturn(true);

        // Verificar el test
        assertThrows(ConflictoException.class, () -> marcaService.eliminar(1L));
        verify(marcaRepository, never()).delete(any());
    }

    @Test
    void eliminar_sinProductosAsociados_debeBorrarla() {
        // Simular datos SIN error
        Marca marca = new Marca(1L, "Aglome");
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(productoBaseRepository.existsByMarcaIdAndEliminadoFalse(1L)).thenReturn(false);

        marcaService.eliminar(1L);

        // Verificar el test
        verify(marcaRepository, times(1)).delete(marca);
    }
}
