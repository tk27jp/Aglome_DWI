package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.utp.aglome_tienda.dto.FiltroProductoRequest;
import pe.edu.utp.aglome_tienda.dto.OpcionesFiltroDTO;
import pe.edu.utp.aglome_tienda.dto.PaginaDTO;
import pe.edu.utp.aglome_tienda.dto.ProductoBaseDTO;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiltroProductoServiceTest {

    @Mock private ProductoBaseRepository productoBaseRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private MarcaRepository marcaRepository;
    @Mock private VarianteService varianteService;
    @Mock private VarianteAtributoRepository varianteAtributoRepository;
    @Mock private VarianteRepository varianteRepository;

    @InjectMocks
    private ProductoBaseService productoBaseService;

    private ProductoBase productoEjemplo(String nombre) {
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        return new ProductoBase(UUID.randomUUID(), nombre, "desc", new Marca(1L, "Aglome"), categoria, true);
    }

    @Test
    void filtrar_conPrecioMinimoMayorAlMaximo_debeLanzarReglaNegocio() {
        // Simular datos CON error
        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setPrecioMin(new BigDecimal("100"));
        filtro.setPrecioMax(new BigDecimal("10"));

        // Verificar el test
        assertThrows(ReglaNegocioException.class,
                () -> productoBaseService.filtrar(filtro, 0, 12, "nombre", "asc"));
        verify(productoBaseRepository, never())
                .findAll(ArgumentMatchers.<Specification<ProductoBase>>any(), any(Pageable.class));
    }

    @Test
    void filtrar_conPrecioNegativo_debeLanzarReglaNegocio() {
        // Simular datos CON error
        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setPrecioMin(new BigDecimal("-1"));

        // Verificar el test
        assertThrows(ReglaNegocioException.class,
                () -> productoBaseService.filtrar(filtro, 0, 12, "nombre", "asc"));
    }

    @Test
    void filtrar_conDatosValidos_debeDevolverLaPaginaConSusTotales() {
        // Simular datos SIN error
        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setColor("Azul");
        ProductoBase producto = productoEjemplo("Melamina Azul");
        when(productoBaseRepository.findAll(ArgumentMatchers.<Specification<ProductoBase>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(producto), PageRequest.of(0, 12), 1));

        PaginaDTO<ProductoBaseDTO> resultado = productoBaseService.filtrar(filtro, 0, 12, "nombre", "asc");

        // Verificar el test
        assertEquals(1, resultado.getTotalElementos());
        assertEquals("Melamina Azul", resultado.getContenido().get(0).getNombre());
        assertEquals("Aglome", resultado.getContenido().get(0).getMarca());
    }

    @Test
    void sugerencias_conMenosDeDosLetras_debeDevolverListaVaciaSinConsultar() {
        // Verificar el test
        assertTrue(productoBaseService.sugerencias("b").isEmpty());
        assertTrue(productoBaseService.sugerencias("  ").isEmpty());
        assertTrue(productoBaseService.sugerencias(null).isEmpty());
        verify(productoBaseRepository, never()).sugerenciasPorNombre(any(), any());
    }

    @Test
    void sugerencias_conTextoValido_debeConsultarConElTextoSinEspacios() {
        // Simular datos SIN error
        when(productoBaseRepository.sugerenciasPorNombre(eq("bis"), any(Pageable.class)))
                .thenReturn(List.of("Bisagra de Acero"));

        // Verificar el test
        assertEquals(List.of("Bisagra de Acero"), productoBaseService.sugerencias("  bis "));
    }

    @Test
    void relacionados_debeBuscarEnLaMismaCategoriaExcluyendoElMismoProducto() {
        // Simular datos SIN error
        ProductoBase base = productoEjemplo("Melamina A");
        ProductoBase otro = productoEjemplo("Melamina B");
        when(productoBaseRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(productoBaseRepository.findByCategoriaIdAndPublicadoTrueAndEliminadoFalseAndIdNot(
                eq(1L), eq(base.getId()), any(Pageable.class))).thenReturn(List.of(otro));

        List<ProductoBaseDTO> resultado = productoBaseService.relacionados(base.getId());

        // Verificar el test
        assertEquals(1, resultado.size());
        assertEquals("Melamina B", resultado.get(0).getNombre());
    }

    @Test
    void relacionados_conProductoInexistente_debeLanzarRecursoNoEncontrado() {
        // Simular datos CON error
        UUID id = UUID.randomUUID();
        when(productoBaseRepository.findById(id)).thenReturn(Optional.empty());

        // Verificar el test
        assertThrows(RecursoNoEncontradoException.class, () -> productoBaseService.relacionados(id));
    }

    @Test
    void opcionesDeFiltro_debeArmarElPanelConLoQueExisteEnLaTienda() {
        // Simular datos SIN error
        when(categoriaRepository.categoriasConProductosVisibles())
                .thenReturn(List.of(new Categoria(1L, "Melaminas", null, true)));
        when(marcaRepository.marcasConProductosVisibles()).thenReturn(List.of(new Marca(1L, "Aglome")));
        when(varianteAtributoRepository.valoresDisponibles("Color")).thenReturn(List.of("Azul", "Rojo"));
        when(varianteAtributoRepository.valoresDisponibles("Medida")).thenReturn(List.of("18mm"));
        when(varianteRepository.precioMinimoDisponible()).thenReturn(new BigDecimal("4.50"));
        when(varianteRepository.precioMaximoDisponible()).thenReturn(new BigDecimal("52.00"));

        OpcionesFiltroDTO opciones = productoBaseService.opcionesDeFiltro();

        // Verificar el test
        assertEquals("Melaminas", opciones.getCategorias().get(0).getNombre());
        assertEquals("Aglome", opciones.getMarcas().get(0).getNombre());
        assertEquals(List.of("Azul", "Rojo"), opciones.getColores());
        assertEquals(List.of("18mm"), opciones.getMedidas());
        assertEquals(new BigDecimal("4.50"), opciones.getPrecioMinimo());
        assertEquals(new BigDecimal("52.00"), opciones.getPrecioMaximo());
    }
}
