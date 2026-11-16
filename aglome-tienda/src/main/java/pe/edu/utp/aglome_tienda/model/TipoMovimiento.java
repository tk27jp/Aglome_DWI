package pe.edu.utp.aglome_tienda.model;

/** Por qué cambió el stock de una variante. */
public enum TipoMovimiento {
    STOCK_INICIAL,   // al crear la variante con stock
    VENTA,           // el cliente confirmó una compra (resta)
    CANCELACION,     // se canceló un pedido y el stock volvió (suma)
    AJUSTE           // el admin corrigió el stock a mano: reposición, merma, conteo físico (suma o resta)
}
