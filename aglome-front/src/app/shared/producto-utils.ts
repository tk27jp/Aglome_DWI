import { Producto, Variante } from '../core/models/api.models';

/** Variantes que se pueden vender (activas). */
export function variantesActivas(p: Producto): Variante[] {
  return (p.variantes ?? []).filter((v) => v.activa);
}

/** Precio más bajo entre las variantes activas, o null si no hay. */
export function precioDesde(p: Producto): number | null {
  const precios = variantesActivas(p).map((v) => v.precioVenta);
  return precios.length ? Math.min(...precios) : null;
}

export function sinStock(p: Producto): boolean {
  const activas = variantesActivas(p);
  return activas.length === 0 || activas.every((v) => v.stock <= 0);
}
