package pe.edu.utp.aglome_tienda.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import pe.edu.utp.aglome_tienda.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
class ConsultasJpqlTest {

    @Autowired private MarcaRepository marcaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoBaseRepository productoBaseRepository;
    @Autowired private VarianteRepository varianteRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private DetallePedidoRepository detallePedidoRepository;
    @Autowired private MovimientoStockRepository movimientoStockRepository;


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

    private Pedido crearPedido(Usuario usuario, EstadoPedido estado, String total, LocalDateTime fecha) {
        Pedido p = new Pedido();
        p.setUsuario(usuario);
        p.setFecha(fecha);
        p.setEstado(estado);
        p.setTotal(new BigDecimal(total));
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
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        Usuario elton = crearUsuario("elton@test.com");

        crearPedido(jhonatan, EstadoPedido.CONFIRMADO);
        crearPedido(jhonatan, EstadoPedido.CANCELADO);
        crearPedido(elton, EstadoPedido.CONFIRMADO);

        List<Pedido> resultado = pedidoRepository
                .buscarPorUsuarioYEstado(jhonatan.getId(), EstadoPedido.CONFIRMADO);

        assertEquals(1, resultado.size());
        assertEquals("jhonatan@test.com", resultado.get(0).getUsuario().getEmail());
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
    void buscarPublicadosPorNombre_paginado_cuentaElTotalYOrdena() {
        Categoria cat = crearCategoria("Melaminas");
        crearProducto("Melamina A", cat, true);
        crearProducto("Melamina B", cat, true);
        crearProducto("Melamina C", cat, true);
        crearProducto("Melamina D", cat, false);   // no publicado: no cuenta

        Page<ProductoBase> pagina = productoBaseRepository.buscarPublicadosPorNombre(
                "melamina", PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "nombre")));

        assertEquals(3, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
        assertEquals("Melamina C", pagina.getContent().get(0).getNombre());
    }

    @Test
    void existsByMarcaId_soloEsVerdaderoSiHayProductosDeEsaMarca() {
        Categoria cat = crearCategoria("Melaminas");
        Marca conProductos = marcaRepository.save(new Marca(null, "Aglome"));
        Marca sinProductos = marcaRepository.save(new Marca(null, "Duratex"));
        ProductoBase p = crearProducto("Melamina Azul", cat, true);
        p.setMarca(conProductos);
        productoBaseRepository.save(p);

        assertEquals(true, productoBaseRepository.existsByMarcaIdAndEliminadoFalse(conProductos.getId()));
        assertEquals(false, productoBaseRepository.existsByMarcaIdAndEliminadoFalse(sinProductos.getId()));
    }

    @Test
    void findByPublicadoTrue_ordenandoPorMarca_noPierdeLosProductosSinMarca() {
        Categoria cat = crearCategoria("Melaminas");
        Marca beta = marcaRepository.save(new Marca(null, "Beta"));
        Marca alfa = marcaRepository.save(new Marca(null, "Alfa"));
        ProductoBase a = crearProducto("Producto 1", cat, true);
        a.setMarca(beta);
        productoBaseRepository.save(a);
        ProductoBase b = crearProducto("Producto 2", cat, true);
        b.setMarca(alfa);
        productoBaseRepository.save(b);
        crearProducto("Producto sin marca", cat, true);

        Page<ProductoBase> pagina = productoBaseRepository.findByPublicadoTrueAndEliminadoFalse(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "marca.nombre")));

        assertEquals(3, pagina.getTotalElements());
        List<String> marcasEnOrden = pagina.getContent().stream()
                .filter(x -> x.getMarca() != null).map(x -> x.getMarca().getNombre()).toList();
        assertEquals(List.of("Alfa", "Beta"), marcasEnOrden);
    }

    @Test
    void productosEliminados_noSalenEnListasNiEnBusquedas() {
        Categoria cat = crearCategoria("Melaminas");
        crearProducto("Melamina Visible", cat, true);
        ProductoBase borrado = crearProducto("Melamina Borrada", cat, true);
        borrado.setEliminado(true);
        productoBaseRepository.save(borrado);

        Page<ProductoBase> lista = productoBaseRepository.findByPublicadoTrueAndEliminadoFalse(PageRequest.of(0, 10));
        List<ProductoBase> busqueda = productoBaseRepository.buscarPublicadosPorNombre("melamina");
        Page<ProductoBase> busquedaPaginada = productoBaseRepository
                .buscarPublicadosPorNombre("melamina", PageRequest.of(0, 10));
        List<ProductoBase> admin = productoBaseRepository.findByEliminadoFalse();

        assertEquals(1, lista.getTotalElements());
        assertEquals(1, busqueda.size());
        assertEquals(1, busquedaPaginada.getTotalElements());
        assertEquals(1, admin.size());
        assertEquals("Melamina Visible", admin.get(0).getNombre());
    }

    @Test
    void buscarPublicadosPorNombre_ordenandoPorMarca_noPierdeLosProductosSinMarca() {
        Categoria cat = crearCategoria("Melaminas");
        Marca beta = marcaRepository.save(new Marca(null, "Beta"));
        ProductoBase conMarca = crearProducto("Melamina con marca", cat, true);
        conMarca.setMarca(beta);
        productoBaseRepository.save(conMarca);
        crearProducto("Melamina sin marca", cat, true);

        Page<ProductoBase> pagina = productoBaseRepository.buscarPublicadosPorNombre(
                "melamina", PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "marca.nombre")));

        assertEquals(2, pagina.getTotalElements());
        assertEquals(2, pagina.getContent().size());
    }

    @Test
    void unidadesVendidasPorCategoria_sumaYOrdenaDeMayorAMenor() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        Categoria melaminas = crearCategoria("Melaminas");
        Categoria pinturas = crearCategoria("Pinturas");
        Variante vMelamina = crearVariante(crearProducto("Melamina Azul", melaminas, true), "MEL-1");
        Variante vPintura = crearVariante(crearProducto("Pintura Blanca", pinturas, true), "PIN-1");

        Pedido pedido = crearPedido(jhonatan, EstadoPedido.CONFIRMADO);
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

    // ---------- Paso 9: reportes que no cuentan pedidos cancelados ----------

    @Test
    void unidadesVendidasPorCategoria_noCuentaPedidosCancelados() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        Categoria melaminas = crearCategoria("Melaminas");
        Variante v = crearVariante(crearProducto("Melamina Azul", melaminas, true), "MEL-1");

        crearDetalle(crearPedido(jhonatan, EstadoPedido.CONFIRMADO), v, 2);
        crearDetalle(crearPedido(jhonatan, EstadoPedido.CANCELADO), v, 50);   // no debe sumar

        List<Object[]> resultado = detallePedidoRepository.unidadesVendidasPorCategoria();

        assertEquals(1, resultado.size());
        assertEquals(2L, ((Number) resultado.get(0)[1]).longValue());
    }

    @Test
    void productosMasVendidos_ordenaExcluyeCanceladosYRespetaElLimite() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        Categoria cat = crearCategoria("Melaminas");
        ProductoBase azul = crearProducto("Melamina Azul", cat, true);
        ProductoBase roja = crearProducto("Melamina Roja", cat, true);
        ProductoBase verde = crearProducto("Melamina Verde", cat, true);
        Variante vAzul = crearVariante(azul, "AZ-1");
        Variante vRoja = crearVariante(roja, "RO-1");
        Variante vVerde = crearVariante(verde, "VE-1");

        Pedido ok = crearPedido(jhonatan, EstadoPedido.CONFIRMADO);
        crearDetalle(ok, vAzul, 2);
        crearDetalle(ok, vRoja, 7);
        crearDetalle(ok, vVerde, 1);
        crearDetalle(crearPedido(jhonatan , EstadoPedido.CANCELADO), vVerde, 100);

        List<Object[]> top = detallePedidoRepository.productosMasVendidos(Pageable.ofSize(2));

        assertEquals(2, top.size());
        assertEquals("Melamina Roja", top.get(0)[1]);
        assertEquals(7L, ((Number) top.get(0)[2]).longValue());
        assertEquals(0, new BigDecimal("70").compareTo((BigDecimal) top.get(0)[3]));
        assertEquals("Melamina Azul", top.get(1)[1]);
    }

    @Test
    void ingresosTotales_yContarValidos_excluyenPedidosCancelados() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        LocalDateTime ahora = LocalDateTime.now();
        crearPedido(jhonatan, EstadoPedido.PENDIENTE, "100.00", ahora);
        crearPedido(jhonatan, EstadoPedido.CONFIRMADO, "200.00", ahora);
        crearPedido(jhonatan, EstadoPedido.CANCELADO, "999.00", ahora);

        assertEquals(0, new BigDecimal("300.00").compareTo(pedidoRepository.ingresosTotales()));
        assertEquals(2L, pedidoRepository.contarValidos());
    }

    @Test
    void ingresosTotales_sinPedidos_retornaNullYContarValidosCero() {
        assertNull(pedidoRepository.ingresosTotales());
        assertEquals(0L, pedidoRepository.contarValidos());
    }

    @Test
    void contarPorEstado_agrupaTodosLosEstados() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        crearPedido(jhonatan, EstadoPedido.PENDIENTE);
        crearPedido(jhonatan, EstadoPedido.CANCELADO);
        crearPedido(jhonatan, EstadoPedido.CANCELADO);

        List<Object[]> filas = pedidoRepository.contarPorEstado();

        long cancelados = filas.stream().filter(f -> f[0] == EstadoPedido.CANCELADO)
                .mapToLong(f -> ((Number) f[1]).longValue()).sum();
        long pendientes = filas.stream().filter(f -> f[0] == EstadoPedido.PENDIENTE)
                .mapToLong(f -> ((Number) f[1]).longValue()).sum();
        assertEquals(2L, cancelados);
        assertEquals(1L, pendientes);
    }

    @Test
    void ventasPorMes_agrupaPorMesOrdenaYExcluyeCancelados() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        crearPedido(jhonatan, EstadoPedido.CONFIRMADO, "100.00", LocalDateTime.of(2026, 2, 10, 9, 0));
        crearPedido(jhonatan, EstadoPedido.PENDIENTE, "50.00", LocalDateTime.of(2026, 1, 20, 9, 0));
        crearPedido(jhonatan, EstadoPedido.CONFIRMADO, "25.00", LocalDateTime.of(2026, 1, 5, 9, 0));
        crearPedido(jhonatan, EstadoPedido.CANCELADO, "900.00", LocalDateTime.of(2026, 1, 6, 9, 0));
        crearPedido(jhonatan, EstadoPedido.CONFIRMADO, "777.00", LocalDateTime.of(2025, 12, 31, 23, 0)); // antes del rango

        List<Object[]> filas = pedidoRepository.ventasPorMes(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertEquals(2, filas.size());
        assertEquals(2026, ((Number) filas.get(0)[0]).intValue());
        assertEquals(1, ((Number) filas.get(0)[1]).intValue());
        assertEquals(2L, ((Number) filas.get(0)[2]).longValue());
        assertEquals(0, new BigDecimal("75.00").compareTo((BigDecimal) filas.get(0)[3]));
        assertEquals(2, ((Number) filas.get(1)[1]).intValue());
        assertEquals(0, new BigDecimal("100.00").compareTo((BigDecimal) filas.get(1)[3]));
    }

    @Test
    void stockBajo_soloVariantesActivasNoEliminadasOrdenadasPorStock() {
        Categoria cat = crearCategoria("Pinturas");
        ProductoBase p = crearProducto("Pintura Blanca", cat, true);
        Variante poco = crearVariante(p, "P-2");
        poco.setStock(2);
        Variante casiNada = crearVariante(p, "P-1");
        casiNada.setStock(0);
        Variante mucho = crearVariante(p, "P-3");
        mucho.setStock(50);
        Variante borrada = crearVariante(p, "P-4");
        borrada.setStock(1);
        borrada.setEliminado(true);
        Variante inactiva = crearVariante(p, "P-5");
        inactiva.setStock(1);
        inactiva.setActiva(false);
        varianteRepository.saveAll(List.of(poco, casiNada, mucho, borrada, inactiva));

        ProductoBase productoBorrado = crearProducto("Pintura Vieja", cat, true);
        productoBorrado.setEliminado(true);
        productoBaseRepository.save(productoBorrado);
        Variante deProductoBorrado = crearVariante(productoBorrado, "V-1");
        deProductoBorrado.setStock(0);
        varianteRepository.save(deProductoBorrado);

        List<Variante> resultado = varianteRepository.stockBajo(5);

        assertEquals(2, resultado.size());
        assertEquals("P-1", resultado.get(0).getSku());   // stock 0 primero
        assertEquals("P-2", resultado.get(1).getSku());
        assertEquals(2L, varianteRepository.contarStockBajo(5));
    }

    @Test
    void listadoAdminDePedidos_ordenaPorFechaDescYFiltraPorEstado() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        crearPedido(jhonatan, EstadoPedido.ENVIADO, "10.00", LocalDateTime.of(2026, 3, 1, 9, 0));
        crearPedido(jhonatan, EstadoPedido.CONFIRMADO, "20.00", LocalDateTime.of(2026, 3, 5, 9, 0));
        crearPedido(jhonatan, EstadoPedido.EN_PREPARACION, "30.00", LocalDateTime.of(2026, 3, 3, 9, 0));

        List<Pedido> todos = pedidoRepository.findAllByOrderByFechaDesc();
        List<Pedido> enviados = pedidoRepository.findByEstadoOrderByFechaDesc(EstadoPedido.ENVIADO);

        assertEquals(3, todos.size());
        assertEquals(EstadoPedido.CONFIRMADO, todos.get(0).getEstado());     // el más reciente primero
        assertEquals(EstadoPedido.EN_PREPARACION, todos.get(1).getEstado());
        assertEquals(1, enviados.size());
    }

    @Test
    void ventasPorMes_losEstadosNuevosSiCuentanComoVenta() {
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 10, 9, 0);
        crearPedido(jhonatan, EstadoPedido.EN_PREPARACION, "10.00", fecha);
        crearPedido(jhonatan, EstadoPedido.ENVIADO, "20.00", fecha);
        crearPedido(jhonatan, EstadoPedido.ENTREGADO, "30.00", fecha);
        crearPedido(jhonatan, EstadoPedido.CANCELADO, "500.00", fecha);

        assertEquals(0, new BigDecimal("60.00").compareTo(pedidoRepository.ingresosTotales()));
        assertEquals(3L, pedidoRepository.contarValidos());
    }

    // ---------- Paso 11: historial de stock ----------

    private void crearMovimiento(Variante v, TipoMovimiento tipo, int cantidad, LocalDateTime fecha) {
        MovimientoStock m = new MovimientoStock();
        m.setVariante(v);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockAnterior(100);
        m.setStockResultante(100 + cantidad);
        m.setMotivo("prueba");
        m.setFecha(fecha);
        movimientoStockRepository.save(m);
    }

    @Test
    void movimientosDeStock_sePuedenFiltrarPorVarianteYTipoYSalenPaginados() {
        Categoria cat = crearCategoria("Melaminas");
        Variante a = crearVariante(crearProducto("Melamina A", cat, true), "A-1");
        Variante b = crearVariante(crearProducto("Melamina B", cat, true), "B-1");
        crearMovimiento(a, TipoMovimiento.STOCK_INICIAL, 100, LocalDateTime.of(2026, 5, 1, 9, 0));
        crearMovimiento(a, TipoMovimiento.VENTA, -2, LocalDateTime.of(2026, 5, 2, 9, 0));
        crearMovimiento(a, TipoMovimiento.AJUSTE, 10, LocalDateTime.of(2026, 5, 3, 9, 0));
        crearMovimiento(b, TipoMovimiento.VENTA, -1, LocalDateTime.of(2026, 5, 4, 9, 0));

        Pageable porFechaDesc = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "fecha"));

        Page<MovimientoStock> deA = movimientoStockRepository.findByVarianteId(a.getId(), porFechaDesc);
        Page<MovimientoStock> ventas = movimientoStockRepository.findByTipo(TipoMovimiento.VENTA, porFechaDesc);
        Page<MovimientoStock> ventasDeA = movimientoStockRepository
                .findByVarianteIdAndTipo(a.getId(), TipoMovimiento.VENTA, porFechaDesc);

        assertEquals(3, deA.getTotalElements());
        assertEquals(2, deA.getContent().size());                          // tamaño de página 2
        assertEquals(TipoMovimiento.AJUSTE, deA.getContent().get(0).getTipo());   // el más reciente primero
        assertEquals(2, ventas.getTotalElements());
        assertEquals(1, ventasDeA.getTotalElements());
        assertEquals(-2, ventasDeA.getContent().get(0).getCantidad());
    }

    @Test
    void findByRol_devuelveSoloEseRolOrdenadoPorNombre() {
        Usuario heidy = crearUsuario("heidy@aglome.com");
        heidy.setNombre("Heidy");
        heidy.setRol(Rol.VENDEDOR);
        usuarioRepository.save(heidy);
        Usuario jose = crearUsuario("jose@aglome.com");
        jose.setNombre("Jose");
        jose.setRol(Rol.VENDEDOR);
        usuarioRepository.save(jose);
        Usuario jhonatan = crearUsuario("jhonatan@test.com");
        jhonatan.setRol(Rol.CLIENTE);
        usuarioRepository.save(jhonatan);

        List<Usuario> vendedores = usuarioRepository.findByRol(Rol.VENDEDOR, Sort.by("nombre"));

        assertEquals(2, vendedores.size());
        assertEquals("Heidy", vendedores.get(0).getNombre());
        assertEquals("Jose", vendedores.get(1).getNombre());
        assertEquals(Rol.VENDEDOR, usuarioRepository.findByEmail("heidy@aglome.com").orElseThrow().getRol());
    }
}
