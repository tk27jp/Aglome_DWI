import { EstadoPedido } from '../core/models/api.models';

export const ETIQUETA_ESTADO: Record<EstadoPedido, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADO: 'Confirmado',
  EN_PREPARACION: 'En preparación',
  ENVIADO: 'Enviado',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

/** Pasos que ve el cliente en la línea de avance de su pedido. */
export const PASOS_PEDIDO: EstadoPedido[] = ['CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO'];

/** Posición del estado en la línea de avance (-1 si no aparece, por ejemplo cancelado). */
export function pasoActual(estado: EstadoPedido): number {
  return estado === 'PENDIENTE' ? 0 : PASOS_PEDIDO.indexOf(estado);
}

/** El cliente solo puede cancelar mientras el pedido no empezó a prepararse. */
export function sePuedeCancelar(estado: EstadoPedido): boolean {
  return estado === 'PENDIENTE' || estado === 'CONFIRMADO';
}

export function codigoCorto(id: string): string {
  return id.slice(0, 8).toUpperCase();
}
