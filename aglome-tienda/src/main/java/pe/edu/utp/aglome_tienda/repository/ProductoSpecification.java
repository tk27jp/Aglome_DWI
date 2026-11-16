package pe.edu.utp.aglome_tienda.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.utp.aglome_tienda.dto.FiltroProductoRequest;
import pe.edu.utp.aglome_tienda.model.Categoria;
import pe.edu.utp.aglome_tienda.model.Marca;
import pe.edu.utp.aglome_tienda.model.ProductoBase;
import pe.edu.utp.aglome_tienda.model.Variante;
import pe.edu.utp.aglome_tienda.model.VarianteAtributo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma, pieza por pieza, la consulta del catálogo con filtros (Criteria API / Specifications).
 * Cada filtro que viene vacío simplemente no se agrega.
 */
public final class ProductoSpecification {

    private ProductoSpecification() {
    }

    /** La consulta completa: siempre solo publicados y no eliminados, más los filtros que lleguen. */
    public static Specification<ProductoBase> desde(FiltroProductoRequest filtro) {
        Specification<ProductoBase> spec = publicadosNoEliminados();

        if (filtro.getCategoriaId() != null) {
            spec = spec.and(conCategoria(filtro.getCategoriaId()));
        }
        if (filtro.getMarcaId() != null) {
            spec = spec.and(conMarca(filtro.getMarcaId()));
        }
        if (hayTexto(filtro.getTexto())) {
            spec = spec.and(conTexto(filtro.getTexto().trim()));
        }
        boolean soloConStock = Boolean.TRUE.equals(filtro.getSoloDisponibles());
        boolean hayFiltroDeVariante = hayTexto(filtro.getColor()) || hayTexto(filtro.getMedida())
                || filtro.getPrecioMin() != null || filtro.getPrecioMax() != null || soloConStock;
        if (hayFiltroDeVariante) {
            spec = spec.and(conVarianteQueCumpla(
                    hayTexto(filtro.getColor()) ? filtro.getColor().trim() : null,
                    hayTexto(filtro.getMedida()) ? filtro.getMedida().trim() : null,
                    filtro.getPrecioMin(), filtro.getPrecioMax(), soloConStock));
        }
        return spec;
    }

    public static Specification<ProductoBase> publicadosNoEliminados() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("publicado")),
                cb.isFalse(root.get("eliminado")));
    }

    /**
     * Productos de la categoría pedida Y de sus subcategorías: al elegir "Ferretería" también salen
     * los de "Bisagras" o "Correderas". Se usa LEFT JOIN porque las categorías principales no tienen padre.
     */
    public static Specification<ProductoBase> conCategoria(Long categoriaId) {
        return (root, query, cb) -> {
            Join<ProductoBase, Categoria> categoria = root.join("categoria", JoinType.LEFT);
            Join<Categoria, Categoria> padre = categoria.join("padre", JoinType.LEFT);
            return cb.or(
                    cb.equal(categoria.get("id"), categoriaId),
                    cb.equal(padre.get("id"), categoriaId));
        };
    }

    public static Specification<ProductoBase> conMarca(Long marcaId) {
        return (root, query, cb) -> cb.equal(root.get("marca").get("id"), marcaId);
    }

    /**
     * Búsqueda por palabras: "bisagra lenta" encuentra "Bisagra de cierre lento" aunque las palabras
     * no estén juntas. Cada palabra debe aparecer en el nombre del producto o en el de su marca.
     */
    public static Specification<ProductoBase> conTexto(String texto) {
        return (root, query, cb) -> {
            Join<ProductoBase, Marca> marca = root.join("marca", JoinType.LEFT);
            List<Predicate> porPalabra = new ArrayList<>();
            for (String palabra : texto.toLowerCase().trim().split("\\s+")) {
                if (palabra.isEmpty()) continue;
                String patron = "%" + palabra + "%";
                porPalabra.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(marca.get("nombre")), patron)));
            }
            return cb.and(porPalabra.toArray(new Predicate[0]));
        };
    }

    /**
     * El producto entra si TIENE AL MENOS UNA variante (activa y no eliminada) que cumpla todos los filtros
     * a la vez. Se usa EXISTS en lugar de JOIN para que un producto con varias variantes que coinciden
     * no salga repetido y la paginación cuente bien.
     */
    public static Specification<ProductoBase> conVarianteQueCumpla(String color, String medida,
                                                                  BigDecimal precioMin, BigDecimal precioMax) {
        return conVarianteQueCumpla(color, medida, precioMin, precioMax, false);
    }

    /** Igual que la anterior; con soloConStock = true la variante además debe tener stock mayor a cero. */
    public static Specification<ProductoBase> conVarianteQueCumpla(String color, String medida,
                                                                  BigDecimal precioMin, BigDecimal precioMax,
                                                                  boolean soloConStock) {
        return (root, query, cb) -> {
            Subquery<Integer> variantes = query.subquery(Integer.class);
            Root<Variante> v = variantes.from(Variante.class);

            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(v.get("productoBase"), root));
            condiciones.add(cb.isFalse(v.get("eliminado")));
            condiciones.add(cb.isTrue(v.get("activa")));

            if (precioMin != null) {
                condiciones.add(cb.greaterThanOrEqualTo(v.<BigDecimal>get("precioVenta"), precioMin));
            }
            if (precioMax != null) {
                condiciones.add(cb.lessThanOrEqualTo(v.<BigDecimal>get("precioVenta"), precioMax));
            }
            if (soloConStock) {
                condiciones.add(cb.greaterThan(v.<Integer>get("stock"), 0));
            }
            if (color != null) {
                condiciones.add(cb.exists(atributo(variantes, cb, v, "Color", color)));
            }
            if (medida != null) {
                condiciones.add(cb.exists(atributo(variantes, cb, v, "Medida", medida)));
            }

            variantes.select(cb.literal(1)).where(condiciones.toArray(new Predicate[0]));
            return cb.exists(variantes);
        };
    }

    // subconsulta: "esta variante tiene el atributo <tipo> con el valor <valor>" (sin importar mayúsculas)
    private static Subquery<Integer> atributo(Subquery<Integer> padre,
                                              jakarta.persistence.criteria.CriteriaBuilder cb,
                                              Root<Variante> variante, String tipo, String valor) {
        Subquery<Integer> sub = padre.subquery(Integer.class);
        Root<VarianteAtributo> va = sub.from(VarianteAtributo.class);
        sub.select(cb.literal(1)).where(
                cb.equal(va.get("variante"), variante),
                cb.equal(cb.lower(va.get("tipoAtributo").get("nombre")), tipo.toLowerCase()),
                cb.equal(cb.lower(va.get("valor")), valor.toLowerCase()));
        return sub;
    }

    private static boolean hayTexto(String s) {
        return s != null && !s.isBlank();
    }
}
