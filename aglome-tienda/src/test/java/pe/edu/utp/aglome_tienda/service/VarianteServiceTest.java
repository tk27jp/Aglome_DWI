package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.VarianteDTO;
import pe.edu.utp.aglome_tienda.dto.VarianteRequest;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.model.TipoMovimiento;
import pe.edu.utp.aglome_tienda.model.Variante;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VarianteServiceTest {

    @Mock private VarianteRepository varianteRepository;
    @Mock private ProductoBaseRepository productoBaseRepository;
    @Mock private TipoAtributoRepository tipoAtributoRepository;
    @Mock private VarianteAtributoRepository varianteAtributoRepository;
    @Mock private MovimientoStockService movimientoStockService;

    @InjectMocks
    private VarianteService varianteService;

    private VarianteRequest requestEjemplo(UUID productoId, String sku) {
        VarianteRequest request = new VarianteRequest();
        request.setProductoBaseId(productoId);
        request.setSku(sku);
        request.setPrecioVenta(new BigDecimal("45.90"));
        request.setStock(30);
        request.setAtributos(Map.of());
        return request;
    }

    private Variante varianteEjemplo(UUID id, String sku) {
        Variante variante = new Variante();
        variante.setId(id);
        variante.setSku(sku);
        variante.setPrecioVenta(new BigDecimal("45.90"));
        variante.setStock(30);
        variante.setActiva(true);
        return variante;
    }

    // ── crear ────────────────────────────────────────────────────────

    @Test
    void crear_conImagenUrl_debeGuardarYDevolverLaImagen() {
        UUID productoId = UUID.randomUUID();
        ProductoBase producto = new ProductoBase();
        producto.setId(productoId);

        VarianteRequest request = requestEjemplo(productoId, "MELM-AZM-18MM");
        request.setImagenUrl("https://ejemplo.com/azul-marino.jpg");

        when(productoBaseRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(varianteRepository.save(any(Variante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        VarianteDTO resultado = varianteService.crear(request);

        assertEquals("https://ejemplo.com/azul-marino.jpg", resultado.getImagenUrl());
    }

    @Test
    void crear_conSkuRepetido_debeLanzarExcepcionYNoGuardar() {
        VarianteRequest request = requestEjemplo(UUID.randomUUID(), "SKU-1");
        when(varianteRepository.existsBySku("SKU-1")).thenReturn(true);

        RuntimeException ex = assertThrows(ConflictoException.class,
                () -> varianteService.crear(request));

        assertEquals("Ya existe una variante con ese SKU", ex.getMessage());
        verify(varianteRepository, never()).save(any());
    }

    @Test
    void crear_conProductoInexistente_debeLanzarExcepcion() {
        UUID productoId = UUID.randomUUID();
        VarianteRequest request = requestEjemplo(productoId, "SKU-1");
        when(productoBaseRepository.findById(productoId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> varianteService.crear(request));

        assertEquals("Producto base no encontrado", ex.getMessage());
        verify(varianteRepository, never()).save(any());
    }

    // ── listar ───────────────────────────────────────────────────────

    @Test
    void listarPorProducto_debeDevolverLasVariantesDelProducto() {
        UUID productoId = UUID.randomUUID();
        Variante variante = varianteEjemplo(UUID.randomUUID(), "SKU-1");
        when(varianteRepository.findByProductoBaseIdAndEliminadoFalse(productoId)).thenReturn(List.of(variante));

        List<VarianteDTO> resultado = varianteService.listarPorProducto(productoId);

        assertEquals(1, resultado.size());
        assertEquals("SKU-1", resultado.get(0).getSku());
    }

    // ── actualizar ───────────────────────────────────────────────────

    @Test
    void actualizar_conDatosValidos_debeCambiarSkuYPrecio() {
        UUID id = UUID.randomUUID();
        Variante variante = varianteEjemplo(id, "SKU-VIEJO");
        VarianteRequest request = requestEjemplo(UUID.randomUUID(), "SKU-NUEVO");
        request.setPrecioVenta(new BigDecimal("50.00"));

        when(varianteRepository.findById(id)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any(Variante.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        VarianteDTO resultado = varianteService.actualizar(id, request);

        assertEquals("SKU-NUEVO", resultado.getSku());
        assertEquals(new BigDecimal("50.00"), resultado.getPrecioVenta());
    }

    @Test
    void actualizar_conSkuDeOtraVariante_debeLanzarExcepcionYNoGuardar() {
        UUID id = UUID.randomUUID();
        Variante variante = varianteEjemplo(id, "SKU-1");
        VarianteRequest request = requestEjemplo(UUID.randomUUID(), "SKU-OTRO");

        when(varianteRepository.findById(id)).thenReturn(Optional.of(variante));
        when(varianteRepository.existsBySkuAndIdNot("SKU-OTRO", id)).thenReturn(true);

        RuntimeException ex = assertThrows(ConflictoException.class,
                () -> varianteService.actualizar(id, request));

        assertEquals("Ya existe otra variante con ese SKU", ex.getMessage());
        verify(varianteRepository, never()).save(any());
    }

    // ── eliminar (borrado lógico) ─────────────────────────────────────

    @Test
    void eliminar_conIdExistente_debeMarcarlaComoEliminadaSinBorrarla() {
        UUID id = UUID.randomUUID();
        Variante variante = varianteEjemplo(id, "SKU-1");
        when(varianteRepository.findById(id)).thenReturn(Optional.of(variante));

        varianteService.eliminar(id);

        assertEquals(true, variante.getEliminado());
        verify(varianteRepository).save(variante);
        verify(varianteRepository, never()).deleteById(any());
        verify(varianteRepository, never()).delete(any());
    }

    @Test
    void eliminar_conIdInexistente_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();
        when(varianteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> varianteService.eliminar(id));
        verify(varianteRepository, never()).save(any());
    }

    @Test
    void eliminar_conVarianteYaEliminada_debeLanzarRecursoNoEncontrado() {
        UUID id = UUID.randomUUID();
        Variante variante = varianteEjemplo(id, "SKU-1");
        variante.setEliminado(true);
        when(varianteRepository.findById(id)).thenReturn(Optional.of(variante));

        assertThrows(RecursoNoEncontradoException.class, () -> varianteService.eliminar(id));
        verify(varianteRepository, never()).save(any());
    }

    @Test
    void eliminarPorProducto_debeMarcarTodasSusVariantesComoEliminadas() {
        UUID productoId = UUID.randomUUID();
        Variante v1 = varianteEjemplo(UUID.randomUUID(), "SKU-1");
        Variante v2 = varianteEjemplo(UUID.randomUUID(), "SKU-2");
        when(varianteRepository.findByProductoBaseIdAndEliminadoFalse(productoId)).thenReturn(List.of(v1, v2));

        varianteService.eliminarPorProducto(productoId);

        assertEquals(true, v1.getEliminado());
        assertEquals(true, v2.getEliminado());
        verify(varianteRepository).saveAll(List.of(v1, v2));
    }

    @Test
    void crear_conProductoEliminado_debeLanzarRecursoNoEncontrado() {
        UUID productoId = UUID.randomUUID();
        ProductoBase producto = new ProductoBase();
        producto.setId(productoId);
        producto.setEliminado(true);
        when(varianteRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productoBaseRepository.findById(productoId)).thenReturn(Optional.of(producto));

        assertThrows(RecursoNoEncontradoException.class,
                () -> varianteService.crear(requestEjemplo(productoId, "SKU-1")));
        verify(varianteRepository, never()).save(any());
    }

    // ── historial de stock (paso 11) ─────────────────────────────────

    @Test
    void crear_conStock_registraElStockInicial() {
        UUID productoId = UUID.randomUUID();
        ProductoBase producto = new ProductoBase();
        producto.setId(productoId);
        VarianteRequest request = requestEjemplo(productoId, "SKU-1");   // stock 30

        when(productoBaseRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(varianteRepository.save(any(Variante.class))).thenAnswer(inv -> inv.getArgument(0));

        varianteService.crear(request);

        verify(movimientoStockService).registrar(any(Variante.class), eq(TipoMovimiento.STOCK_INICIAL),
                eq(0), eq(30), anyString(), isNull());
    }

    @Test
    void crear_conStockCero_noRegistraMovimiento() {
        UUID productoId = UUID.randomUUID();
        ProductoBase producto = new ProductoBase();
        producto.setId(productoId);
        VarianteRequest request = requestEjemplo(productoId, "SKU-1");
        request.setStock(0);

        when(productoBaseRepository.findById(productoId)).thenReturn(Optional.of(producto));
        when(varianteRepository.save(any(Variante.class))).thenAnswer(inv -> inv.getArgument(0));

        varianteService.crear(request);

        verifyNoInteractions(movimientoStockService);
    }

    @Test
    void actualizar_cambiandoElStock_registraUnAjuste() {
        UUID id = UUID.randomUUID();
        Variante variante = varianteEjemplo(id, "SKU-1");                 // stock 30
        VarianteRequest request = requestEjemplo(UUID.randomUUID(), "SKU-1");
        request.setStock(45);

        when(varianteRepository.findById(id)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any(Variante.class))).thenAnswer(inv -> inv.getArgument(0));

        varianteService.actualizar(id, request);

        verify(movimientoStockService).registrar(any(Variante.class), eq(TipoMovimiento.AJUSTE),
                eq(30), eq(45), anyString(), isNull());
    }

    @Test
    void actualizar_sinCambiarElStock_noRegistraMovimiento() {
        UUID id = UUID.randomUUID();
        Variante variante = varianteEjemplo(id, "SKU-1");                 // stock 30
        VarianteRequest request = requestEjemplo(UUID.randomUUID(), "SKU-1");   // stock 30

        when(varianteRepository.findById(id)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any(Variante.class))).thenAnswer(inv -> inv.getArgument(0));

        varianteService.actualizar(id, request);

        verifyNoInteractions(movimientoStockService);
    }
}
