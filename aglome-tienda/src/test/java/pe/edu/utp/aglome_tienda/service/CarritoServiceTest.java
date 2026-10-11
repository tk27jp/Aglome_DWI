package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.aglome_tienda.dto.AgregarAlCarritoRequest;
import pe.edu.utp.aglome_tienda.dto.CarritoDTO;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock private CarritoRepository carritoRepository;
    @Mock private CarritoItemRepository carritoItemRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private VarianteRepository varianteRepository;

    @InjectMocks
    private CarritoService carritoService;

    private Variante crearVariante(int stock) {
        Categoria categoria = new Categoria(1L, "Melaminas", null, true);
        ProductoBase producto = new ProductoBase(UUID.randomUUID(), "Melamina", "desc", "Aglome", categoria, true);
        return new Variante(UUID.randomUUID(), producto, "SKU-1", new BigDecimal("45.90"), stock, true);
    }

    @Test
    void agregarItem_conStockSuficiente_debeAgregarloAlCarrito() {
        // Simular datos SIN error
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Variante variante = crearVariante(30);

        AgregarAlCarritoRequest request = new AgregarAlCarritoRequest();
        request.setVarianteId(variante.getId());
        request.setCantidad(2);

        when(usuarioRepository.findByEmail("jonathan@test.com")).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(varianteRepository.findById(variante.getId())).thenReturn(Optional.of(variante));
        when(carritoItemRepository.findByCarritoIdAndVarianteId(any(), any())).thenReturn(Optional.empty());
        when(carritoItemRepository.findByCarritoId(any())).thenReturn(List.of());

        CarritoDTO resultado = carritoService.agregarItem("jonathan@test.com", request);

        // Verificar el test
        assertNotNull(resultado);
        verify(carritoItemRepository, times(1)).save(any(CarritoItem.class));
    }

    @Test
    void agregarItem_conStockInsuficiente_debeLanzarExcepcion() {
        // Simular datos CON error (piden 5 y solo queda 1)
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        Variante variante = crearVariante(1);

        AgregarAlCarritoRequest request = new AgregarAlCarritoRequest();
        request.setVarianteId(variante.getId());
        request.setCantidad(5);

        when(usuarioRepository.findByEmail("jonathan@test.com")).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(varianteRepository.findById(variante.getId())).thenReturn(Optional.of(variante));

        // Verificar el test
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> carritoService.agregarItem("jonathan@test.com", request));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(carritoItemRepository, never()).save(any());
    }

    @Test
    void agregarItem_conVarianteInexistente_debeLanzarExcepcion() {
        // Simular datos CON error (la variante no existe)
        Usuario usuario = new Usuario(1L, "Jonathan Navarro", "jonathan@test.com", "hash", Rol.CLIENTE);
        Carrito carrito = new Carrito(UUID.randomUUID(), usuario);
        UUID varianteInventada = UUID.randomUUID();

        AgregarAlCarritoRequest request = new AgregarAlCarritoRequest();
        request.setVarianteId(varianteInventada);
        request.setCantidad(1);

        when(usuarioRepository.findByEmail("jonathan@test.com")).thenReturn(Optional.of(usuario));
        when(carritoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(carrito));
        when(varianteRepository.findById(varianteInventada)).thenReturn(Optional.empty());

        // Verificar el test
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> carritoService.agregarItem("jonathan@test.com", request));
        assertEquals("Variante no encontrada", ex.getMessage());
        verify(carritoItemRepository, never()).save(any());
    }
}