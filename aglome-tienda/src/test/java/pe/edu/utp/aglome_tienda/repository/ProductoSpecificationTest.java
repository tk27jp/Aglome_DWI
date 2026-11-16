package pe.edu.utp.aglome_tienda.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pe.edu.utp.aglome_tienda.dto.FiltroProductoRequest;
import pe.edu.utp.aglome_tienda.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductoSpecificationTest {

    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoBaseRepository productoBaseRepository;
    @Autowired private VarianteRepository varianteRepository;
    @Autowired private TipoAtributoRepository tipoAtributoRepository;
    @Autowired private VarianteAtributoRepository varianteAtributoRepository;

    private final Pageable pagina = PageRequest.of(0, 10);

    // ---------- Métodos auxiliares ----------

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return categoriaRepository.save(c);
    }

    private Marca marca(String nombre) {
        return marcaRepository.save(new Marca(null, nombre));
    }

    private ProductoBase producto(String nombre, Categoria categoria, Marca marca, boolean publicado) {
        ProductoBase p = new ProductoBase();
        p.setNombre(nombre);
        p.setCategoria(categoria);
        p.setMarca(marca);
        p.setPublicado(publicado);
        return productoBaseRepository.save(p);
    }

    private Variante variante(ProductoBase producto, String precio) {
        Variante v = new Variante();
        v.setProductoBase(producto);
        v.setSku("SKU-" + UUID.randomUUID());
        v.setPrecioVenta(new BigDecimal(precio));
        v.setStock(10);
        v.setActiva(true);
        return varianteRepository.save(v);
    }

    private TipoAtributo tipo(String nombre) {
        return tipoAtributoRepository.findAll().stream()
                .filter(t -> t.getNombre().equalsIgnoreCase(nombre)).findFirst()
                .orElseGet(() -> {
                    TipoAtributo t = new TipoAtributo();
                    t.setNombre(nombre);
                    return tipoAtributoRepository.save(t);
                });
    }

    private void atributo(Variante variante, String tipo, String valor) {
        VarianteAtributo va = new VarianteAtributo();
        va.setVariante(variante);
        va.setTipoAtributo(tipo(tipo));
        va.setValor(valor);
        varianteAtributoRepository.save(va);
    }

    private Page<ProductoBase> buscar(FiltroProductoRequest filtro) {
        return productoBaseRepository.findAll(ProductoSpecification.desde(filtro), pagina);
    }

    private List<String> nombres(Page<ProductoBase> resultado) {
        return resultado.getContent().stream().map(ProductoBase::getNombre).sorted().toList();
    }

    // ---------- Tests de filtros ----------

    @Test
    void sinFiltros_devuelveSoloLosProductosPublicadosYNoEliminados() {
        Categoria cat = categoria("Melaminas");
        producto("Visible", cat, null, true);
        producto("No publicado", cat, null, false);
        ProductoBase borrado = producto("Eliminado", cat, null, true);
        borrado.setEliminado(true);
        productoBaseRepository.save(borrado);

        Page<ProductoBase> resultado = buscar(new FiltroProductoRequest());

        assertEquals(List.of("Visible"), nombres(resultado));
    }

    @Test
    void filtraPorCategoria() {
        Categoria melaminas = categoria("Melaminas");
        Categoria pinturas = categoria("Pinturas");
        producto("Melamina Azul", melaminas, null, true);
        producto("Pintura Blanca", pinturas, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setCategoriaId(melaminas.getId());

        assertEquals(List.of("Melamina Azul"), nombres(buscar(filtro)));
    }

    @Test
    void filtraPorMarca_yDescartaLosProductosSinMarca() {
        Categoria cat = categoria("Melaminas");
        Marca aglome = marca("Aglome");
        producto("Con marca", cat, aglome, true);
        producto("Sin marca", cat, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setMarcaId(aglome.getId());

        assertEquals(List.of("Con marca"), nombres(buscar(filtro)));
    }

    @Test
    void filtraPorTexto_sinImportarMayusculas() {
        Categoria cat = categoria("Melaminas");
        producto("Melamina Azul", cat, null, true);
        producto("Pintura Blanca", cat, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setTexto("  MELAMINA ");

        assertEquals(List.of("Melamina Azul"), nombres(buscar(filtro)));
    }

    @Test
    void filtraPorRangoDePrecio_segunLasVariantes() {
        Categoria cat = categoria("Melaminas");
        ProductoBase barato = producto("Barato", cat, null, true);
        ProductoBase caro = producto("Caro", cat, null, true);
        variante(barato, "10.00");
        variante(caro, "100.00");

        FiltroProductoRequest hasta50 = new FiltroProductoRequest();
        hasta50.setPrecioMax(new BigDecimal("50"));
        FiltroProductoRequest desde50 = new FiltroProductoRequest();
        desde50.setPrecioMin(new BigDecimal("50"));
        FiltroProductoRequest todos = new FiltroProductoRequest();
        todos.setPrecioMin(new BigDecimal("5"));
        todos.setPrecioMax(new BigDecimal("200"));

        assertEquals(List.of("Barato"), nombres(buscar(hasta50)));
        assertEquals(List.of("Caro"), nombres(buscar(desde50)));
        assertEquals(List.of("Barato", "Caro"), nombres(buscar(todos)));
    }

    @Test
    void colorYMedida_debenCoincidirEnLaMismaVariante() {
        Categoria cat = categoria("Melaminas");
        ProductoBase p = producto("Melamina", cat, null, true);
        Variante azul18 = variante(p, "40.00");
        atributo(azul18, "Color", "Azul");
        atributo(azul18, "Medida", "18mm");
        Variante rojo15 = variante(p, "35.00");
        atributo(rojo15, "Color", "Rojo");
        atributo(rojo15, "Medida", "15mm");

        FiltroProductoRequest coincide = new FiltroProductoRequest();
        coincide.setColor("azul");      // sin importar mayúsculas
        coincide.setMedida("18MM");
        FiltroProductoRequest mezclado = new FiltroProductoRequest();
        mezclado.setColor("Azul");      // azul existe y 15mm existe, pero en variantes distintas
        mezclado.setMedida("15mm");

        assertEquals(List.of("Melamina"), nombres(buscar(coincide)));
        assertTrue(buscar(mezclado).isEmpty());
    }

    @Test
    void productoConVariasVariantesQueCoinciden_noSaleRepetidoYElTotalCuentaBien() {
        Categoria cat = categoria("Melaminas");
        ProductoBase p = producto("Melamina", cat, null, true);
        atributo(variante(p, "40.00"), "Color", "Azul");
        atributo(variante(p, "42.00"), "Color", "Azul");

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setColor("Azul");
        Page<ProductoBase> resultado = buscar(filtro);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());
    }

    @Test
    void variantesEliminadasOInactivas_noCuentanParaElFiltro() {
        Categoria cat = categoria("Melaminas");
        ProductoBase p = producto("Melamina", cat, null, true);
        Variante eliminada = variante(p, "10.00");
        eliminada.setEliminado(true);
        varianteRepository.save(eliminada);
        Variante inactiva = variante(p, "12.00");
        inactiva.setActiva(false);
        varianteRepository.save(inactiva);
        variante(p, "100.00");

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setPrecioMax(new BigDecimal("50"));

        assertTrue(buscar(filtro).isEmpty());
    }

    @Test
    void combinaTodosLosFiltrosALaVez() {
        Categoria melaminas = categoria("Melaminas");
        Categoria pinturas = categoria("Pinturas");
        Marca aglome = marca("Aglome");
        Marca otra = marca("Otra");
        ProductoBase buscado = producto("Melamina Azul", melaminas, aglome, true);
        atributo(variante(buscado, "45.90"), "Color", "Azul");
        ProductoBase otraMarca = producto("Melamina Azul 2", melaminas, otra, true);
        atributo(variante(otraMarca, "45.90"), "Color", "Azul");
        ProductoBase otraCategoria = producto("Pintura Azul", pinturas, aglome, true);
        atributo(variante(otraCategoria, "45.90"), "Color", "Azul");

        FiltroProductoRequest filtro = new FiltroProductoRequest("melamina", melaminas.getId(), aglome.getId(),
                "Azul", null, new BigDecimal("40"), new BigDecimal("50"));

        assertEquals(List.of("Melamina Azul"), nombres(buscar(filtro)));
    }

    // ---------- Tests de las consultas de apoyo (panel de filtros, buscador, relacionados) ----------

    @Test
    void valoresDisponiblesYRangoDePrecio_soloConsideranLoQueSePuedeComprar() {
        Categoria cat = categoria("Melaminas");
        ProductoBase visible = producto("Visible", cat, null, true);
        atributo(variante(visible, "20.00"), "Color", "Rojo");
        atributo(variante(visible, "80.00"), "Color", "Azul");
        atributo(variante(visible, "60.00"), "Color", "Azul");   // repetido: debe salir una sola vez
        ProductoBase oculto = producto("Oculto", cat, null, false);
        atributo(variante(oculto, "1.00"), "Color", "Verde");    // no publicado: no cuenta

        assertEquals(List.of("Azul", "Rojo"), varianteAtributoRepository.valoresDisponibles("color"));
        assertEquals(0, new BigDecimal("20.00").compareTo(varianteRepository.precioMinimoDisponible()));
        assertEquals(0, new BigDecimal("80.00").compareTo(varianteRepository.precioMaximoDisponible()));
    }

    @Test
    void marcasYCategoriasConProductosVisibles_ignoranLasQueNoTienenNada() {
        Categoria conProductos = categoria("Melaminas");
        categoria("Vacía");                       // sin productos: no debe salir
        Marca aglome = marca("Aglome");
        marca("Sin productos");                   // sin productos: no debe salir
        producto("Melamina", conProductos, aglome, true);

        assertEquals(List.of("Aglome"), marcaRepository.marcasConProductosVisibles().stream().map(Marca::getNombre).toList());
        assertEquals(List.of("Melaminas"), categoriaRepository.categoriasConProductosVisibles().stream().map(Categoria::getNombre).toList());
    }

    @Test
    void sugerenciasYRelacionados_respetanLimitesYExcluyenElMismoProducto() {
        Categoria melaminas = categoria("Melaminas");
        Categoria pinturas = categoria("Pinturas");
        ProductoBase a = producto("Melamina A", melaminas, null, true);
        producto("Melamina B", melaminas, null, true);
        producto("Melamina oculta", melaminas, null, false);
        producto("Pintura Blanca", pinturas, null, true);

        List<String> sugerencias = productoBaseRepository.sugerenciasPorNombre("melam", PageRequest.of(0, 8));
        List<ProductoBase> relacionados = productoBaseRepository
                .findByCategoriaIdAndPublicadoTrueAndEliminadoFalseAndIdNot(melaminas.getId(), a.getId(), PageRequest.of(0, 4));

        assertEquals(List.of("Melamina A", "Melamina B"), sugerencias);
        assertEquals(1, relacionados.size());
        assertEquals("Melamina B", relacionados.get(0).getNombre());
    }

    // ---------- Mejoras del filtro: subcategorías, búsqueda por palabras y disponibilidad ----------

    @Test
    void filtraPorCategoriaPrincipal_incluyeLosProductosDeSusSubcategorias() {
        Categoria ferreteria = categoria("Ferretería");
        Categoria bisagras = categoria("Bisagras");
        bisagras.setPadre(ferreteria);
        categoriaRepository.save(bisagras);
        Categoria melaminas = categoria("Melaminas");
        producto("Clavos", ferreteria, null, true);
        producto("Bisagra cazoleta", bisagras, null, true);
        producto("Melamina Blanca", melaminas, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setCategoriaId(ferreteria.getId());

        assertEquals(List.of("Bisagra cazoleta", "Clavos"), nombres(buscar(filtro)));
    }

    @Test
    void filtraPorSubcategoria_noTraeLosProductosDeLaCategoriaPadre() {
        Categoria ferreteria = categoria("Ferretería");
        Categoria bisagras = categoria("Bisagras");
        bisagras.setPadre(ferreteria);
        categoriaRepository.save(bisagras);
        producto("Clavos", ferreteria, null, true);
        producto("Bisagra cazoleta", bisagras, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setCategoriaId(bisagras.getId());

        assertEquals(List.of("Bisagra cazoleta"), nombres(buscar(filtro)));
    }

    @Test
    void buscaPorVariasPalabras_sinImportarElOrdenNiSiEstanJuntas() {
        Categoria cat = categoria("Bisagras");
        producto("Bisagra de cierre lento 35 mm", cat, null, true);
        producto("Bisagra económica", cat, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setTexto("lento bisagra");

        assertEquals(List.of("Bisagra de cierre lento 35 mm"), nombres(buscar(filtro)));
    }

    @Test
    void buscaTambienPorElNombreDeLaMarca() {
        Categoria cat = categoria("Melaminas");
        Marca duratex = marca("Duratex");
        producto("Melamina Arena", cat, duratex, true);
        producto("Melamina Arena", cat, null, true);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setTexto("arena duratex");

        assertEquals(1, buscar(filtro).getTotalElements());
    }

    @Test
    void soloDisponibles_dejaSoloLosProductosConAlgunaVarianteConStock() {
        Categoria cat = categoria("Melaminas");
        ProductoBase conStock = producto("Con stock", cat, null, true);
        variante(conStock, "50");
        ProductoBase agotado = producto("Agotado", cat, null, true);
        Variante sinStock = variante(agotado, "50");
        sinStock.setStock(0);
        varianteRepository.save(sinStock);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setSoloDisponibles(true);

        assertEquals(List.of("Con stock"), nombres(buscar(filtro)));
    }

    @Test
    void soloDisponiblesEnFalse_noFiltraNada() {
        Categoria cat = categoria("Melaminas");
        ProductoBase agotado = producto("Agotado", cat, null, true);
        Variante sinStock = variante(agotado, "50");
        sinStock.setStock(0);
        varianteRepository.save(sinStock);

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setSoloDisponibles(false);

        assertEquals(List.of("Agotado"), nombres(buscar(filtro)));
    }
}
