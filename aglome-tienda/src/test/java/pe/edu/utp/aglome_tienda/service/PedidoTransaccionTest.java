package pe.edu.utp.aglome_tienda.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pe.edu.utp.aglome_tienda.dto.PedidoDTO;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:txtest;DB_CLOSE_DELAY=-1")
class PedidoTransaccionTest {

    private static final String EMAIL = "jonathan@test.com";

    @Autowired private PedidoService pedidoService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoBaseRepository productoBaseRepository;
    @Autowired private VarianteRepository varianteRepository;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private CarritoItemRepository carritoItemRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private DetallePedidoRepository detallePedidoRepository;

    // Se simula este service para poder forzar un fallo en el último paso de la compra
    @MockitoBean private CarritoService carritoService;

    private UUID varianteId;

    @BeforeEach
    void prepararDatos() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Jonathan Navarro");
        usuario.setEmail(EMAIL);
        usuario.setPassword("hash");
        usuario.setRol(Rol.CLIENTE);
        usuario = usuarioRepository.save(usuario);

        Categoria categoria = new Categoria();
        categoria.setNombre("Melaminas");
        categoria = categoriaRepository.save(categoria);

        ProductoBase producto = new ProductoBase();
        producto.setNombre("Melamina Azul Marino");
        producto.setCategoria(categoria);
        producto.setPublicado(true);
        producto = productoBaseRepository.save(producto);

        Variante variante = new Variante();
        variante.setProductoBase(producto);
        variante.setSku("TX-TEST-001");
        variante.setPrecioVenta(new BigDecimal("45.90"));
        variante.setStock(30);
        variante = varianteRepository.save(variante);
        varianteId = variante.getId();

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito = carritoRepository.save(carrito);

        CarritoItem item = new CarritoItem();
        item.setCarrito(carrito);
        item.setVariante(variante);
        item.setCantidad(2);
        carritoItemRepository.save(item);
    }

    @AfterEach
    void limpiarDatos() {
        detallePedidoRepository.deleteAll();
        pedidoRepository.deleteAll();
        carritoItemRepository.deleteAll();
        carritoRepository.deleteAll();
        varianteRepository.deleteAll();
        productoBaseRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private int stockActual() {
        return varianteRepository.findById(varianteId).orElseThrow().getStock();
    }

    @Test
    void confirmarCompra_sinErrores_debeGuardarPedidoYDescontarStock() {
        // Simular datos SIN error: la compra termina bien
        PedidoDTO pedido = pedidoService.confirmarCompra(EMAIL);

        // Verificar el test: todo se guardó
        assertEquals(0, new BigDecimal("91.80").compareTo(pedido.getTotal()));
        assertEquals(1, pedidoRepository.count());
        assertEquals(28, stockActual()); // 30 - 2
    }

    @Test
    void confirmarCompra_siFallaAlFinal_debeDeshacerPedidoYStock() {
        // Simular datos CON error: falla el último paso (vaciar el carrito)
        doThrow(new RuntimeException("Fallo simulado")).when(carritoService).vaciarCarrito(any());

        assertThrows(RuntimeException.class, () -> pedidoService.confirmarCompra(EMAIL));

        // Verificar el test: se deshizo TODO (rollback)
        assertEquals(0, pedidoRepository.count());
        assertEquals(0, detallePedidoRepository.count());
        assertEquals(30, stockActual()); // el stock sigue intacto
    }
}