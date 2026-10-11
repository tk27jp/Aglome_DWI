package pe.edu.utp.aglome_tienda.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import pe.edu.utp.aglome_tienda.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ConsultasJpqlTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoBaseRepository productoBaseRepository;
    @Autowired private VarianteRepository varianteRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private DetallePedidoRepository detallePedidoRepository;

    // ---------- Métodos auxiliares para armar datos de prueba ----------

    private Usuario crearUsuario(String email) {
        Usuario u = new Usuario();
        u.setNombre("Usuario " + email);
        u.setEmail(email);
        u.setPassword("hash");
        u.setRol(Rol.CLIENTE);
        return usuarioRepository.save(u);
    }

    private Categoria crearCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return categoriaRepository.save(c);
    }

    private ProductoBase crearProducto(String nombre, Categoria categoria, boolean publicado) {
        ProductoBase p = new ProductoBase();
        p.setNombre(nombre);
        p.setCategoria(categoria);
        p.setPublicado(publicado);
        return productoBaseRepository.save(p);
    }

    private Variante crearVariante(ProductoBase producto, String sku) {
        Variante v = new Variante();
        v.setProductoBase(producto);
        v.setSku(sku);
        v.setPrecioVenta(BigDecimal.TEN);
        v.setStock(100);
        return varianteRepository.save(v);
    }

    private Pedido crearPedido(Usuario usuario, EstadoPedido estado) {
        Pedido p = new Pedido();
        p.setUsuario(usuario);
        p.setFecha(LocalDateTime.now());
        p.setEstado(estado);
        p.setTotal(BigDecimal.TEN);
        return pedidoRepository.save(p);
    }

    private void crearDetalle(Pedido pedido, Variante variante, int cantidad) {
        DetallePedido d = new DetallePedido();
        d.setPedido(pedido);
        d.setVariante(variante);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(BigDecimal.TEN);
        d.setSubtotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(cantidad)));
        detallePedidoRepository.save(d);
    }

    // ---------- Tests ----------

    @Test
    void buscarPorUsuarioYEstado_devuelveSoloLosQueCoinciden() {
        Usuario ana = crearUsuario("ana@test.com");
        Usuario luis = crearUsuario("luis@test.com");

        crearPedido(ana, EstadoPedido.CONFIRMADO);
        crearPedido(ana, EstadoPedido.CANCELADO);
        crearPedido(luis, EstadoPedido.CONFIRMADO);

        List<Pedido> resultado = pedidoRepository
                .buscarPorUsuarioYEstado(ana.getId(), EstadoPedido.CONFIRMADO);

        assertEquals(1, resultado.size());
        assertEquals("ana@test.com", resultado.get(0).getUsuario().getEmail());
    }

    @Test
    void buscarPublicadosPorNombre_ignoraMayusculasYNoPublicados() {
        Categoria cat = crearCategoria("Melaminas");
        crearProducto("Melamina Azul", cat, true);
        crearProducto("Melamina Roja", cat, false);   // no publicado: no debe salir
        crearProducto("Pintura Blanca", cat, true);   // otro nombre: no debe salir

        List<ProductoBase> resultado = productoBaseRepository.buscarPublicadosPorNombre("MELAMINA");

        assertEquals(1, resultado.size());
        assertEquals("Melamina Azul", resultado.get(0).getNombre());
    }

    @Test
    void unidadesVendidasPorCategoria_sumaYOrdenaDeMayorAMenor() {
        Usuario ana = crearUsuario("ana@test.com");
        Categoria melaminas = crearCategoria("Melaminas");
        Categoria pinturas = crearCategoria("Pinturas");
        Variante vMelamina = crearVariante(crearProducto("Melamina Azul", melaminas, true), "MEL-1");
        Variante vPintura = crearVariante(crearProducto("Pintura Blanca", pinturas, true), "PIN-1");

        Pedido pedido = crearPedido(ana, EstadoPedido.CONFIRMADO);
        crearDetalle(pedido, vMelamina, 2);
        crearDetalle(pedido, vMelamina, 3);   // en total 5 de melaminas
        crearDetalle(pedido, vPintura, 1);

        List<Object[]> resultado = detallePedidoRepository.unidadesVendidasPorCategoria();

        assertEquals(2, resultado.size());
        assertEquals("Melaminas", resultado.get(0)[0]);
        assertEquals(5L, ((Number) resultado.get(0)[1]).longValue());
        assertEquals("Pinturas", resultado.get(1)[0]);
        assertEquals(1L, ((Number) resultado.get(1)[1]).longValue());
    }
}