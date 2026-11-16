import { Producto, Variante } from '../core/models/api.models';
import { variantesActivas } from './producto-utils';

/**
 * Las ofertas se guardan como una característica más de la variante: "Precio anterior".
 * El precio que se cobra es siempre `precioVenta`; el anterior solo sirve para mostrar el descuento.
 * Así el back no necesita cambios: el administrador crea una oferta desde el editor de variantes.
 */
export const ATRIBUTO_OFERTA = 'Precio anterior';

/** Precio anterior de la variante, o null si no está en oferta. */
export function precioAnterior(v: Variante | null | undefined): number | null {
  const bruto = v?.atributos?.[ATRIBUTO_OFERTA];
  if (!bruto) return null;
  const n = parseFloat(String(bruto).replace(/[^\d.,]/g, '').replace(',', '.'));
  return Number.isFinite(n) && v && n > v.precioVenta ? n : null;
}

/** Porcentaje de descuento (entero) de la variante; 0 si no hay oferta. */
export function descuento(v: Variante | null | undefined): number {
  const anterior = precioAnterior(v);
  return anterior && v ? Math.round((1 - v.precioVenta / anterior) * 100) : 0;
}

/** Cuánto se ahorra por unidad. */
export function ahorro(v: Variante | null | undefined): number {
  const anterior = precioAnterior(v);
  return anterior && v ? anterior - v.precioVenta : 0;
}

/** El mayor descuento entre las variantes activas con stock (o sin stock si no hay otra). */
export function mejorDescuento(p: Producto): number {
  const activas = variantesActivas(p);
  const conStock = activas.filter((v) => v.stock > 0);
  return Math.max(0, ...(conStock.length ? conStock : activas).map(descuento));
}

/** Características de una variante sin las que son solo para uso interno. */
export function atributosVisibles(atributos: Record<string, string> | null | undefined): [string, string][] {
  return Object.entries(atributos ?? {}).filter(([k]) => k !== ATRIBUTO_OFERTA);
}
