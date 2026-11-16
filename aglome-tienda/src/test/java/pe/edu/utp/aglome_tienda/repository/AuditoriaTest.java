package pe.edu.utp.aglome_tienda.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import pe.edu.utp.aglome_tienda.config.JpaAuditingConfig;
import pe.edu.utp.aglome_tienda.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AuditoriaTest {

    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoBaseRepository productoBaseRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PedidoRepository pedidoRepository;

    private Categoria crearCategoria() {
        Categoria c = new Categoria();
        c.setNombre("Melaminas");
        return categoriaRepository.saveAndFlush(c);
    }

    @Test
    void alGuardar_debeLlenarSolasLasFechasDeCreacionYActualizacion() {
        Categoria categoria = crearCategoria();

        assertNotNull(categoria.getFechaCreacion());
        assertNotNull(categoria.getFechaActualizacion());
        assertEquals(false, categoria.getEliminado());
    }

    @Test
    void alModificar_laFechaDeCreacionNoCambiaYLaDeActualizacionNoRetrocede() {
        ProductoBase producto = new ProductoBase();
        producto.setNombre("Melamina Azul");
        producto.setCategoria(crearCategoria());
        producto = productoBaseRepository.saveAndFlush(producto);
        LocalDateTime creacion = producto.getFechaCreacion();
        LocalDateTime primeraActualizacion = producto.getFechaActualizacion();

        producto.setNombre("Melamina Azul Marino");
        producto = productoBaseRepository.saveAndFlush(producto);

        assertEquals(creacion, producto.getFechaCreacion());
        assertFalse(producto.getFechaActualizacion().isBefore(primeraActualizacion));
    }

    @Test
    void losPedidosTambienGuardanSusFechasDeAuditoria() {
        Usuario u = new Usuario();
        u.setNombre("Jhonatan");
        u.setEmail("jhonatan@test.com");
        u.setPassword("hash");
        u.setRol(Rol.CLIENTE);
        u = usuarioRepository.save(u);

        Pedido pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido.setTotal(BigDecimal.TEN);
        pedido = pedidoRepository.saveAndFlush(pedido);

        assertNotNull(pedido.getFechaCreacion());
        assertNotNull(pedido.getFechaActualizacion());
    }
}
