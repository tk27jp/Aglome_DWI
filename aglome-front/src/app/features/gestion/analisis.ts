import { Pedido } from '../../core/models/api.models';

// Cálculos del resumen a partir de la lista de pedidos. Todo en hora local del navegador.

export const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
export const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
const DIA_MS = 86_400_000;

export const inicioDia = (d: Date): Date => new Date(d.getFullYear(), d.getMonth(), d.getDate());
export const sumarDias = (d: Date, n: number): Date => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
export const aISO = (d: Date): string => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
export const deISO = (s: string): Date => {
  const [a, m, d] = s.split('-').map(Number);
  return new Date(a, (m || 1) - 1, d || 1);
};
/** Días que abarca el rango, contando ambos extremos. */
export const diasEntre = (desde: Date, hasta: Date): number => Math.round((inicioDia(hasta).getTime() - inicioDia(desde).getTime()) / DIA_MS) + 1;
/** Lunes = 0 … domingo = 6. */
export const diaSemana = (d: Date): number => (d.getDay() + 6) % 7;

export const esValido = (p: Pedido): boolean => p.estado !== 'CANCELADO';

/** Pedidos cuya fecha cae entre `desde` y `hasta`, ambos días incluidos. */
export function enRango(pedidos: Pedido[], desde: Date, hasta: Date): Pedido[] {
  const a = inicioDia(desde).getTime();
  const b = sumarDias(inicioDia(hasta), 1).getTime();
  return pedidos.filter((p) => {
    const t = new Date(p.fecha).getTime();
    return t >= a && t < b;
  });
}

export interface Totales {
  ingresos: number;
  pedidos: number;
  ticket: number;
  unidades: number;
  cancelados: number;
  tasaCancelacion: number;
}

export function totales(lista: Pedido[]): Totales {
  const validos = lista.filter(esValido);
  const ingresos = validos.reduce((s, p) => s + p.total, 0);
  const unidades = validos.reduce((s, p) => s + p.detalles.reduce((u, d) => u + d.cantidad, 0), 0);
  const cancelados = lista.length - validos.length;
  return {
    ingresos,
    pedidos: validos.length,
    ticket: validos.length ? ingresos / validos.length : 0,
    unidades,
    cancelados,
    tasaCancelacion: lista.length ? (cancelados / lista.length) * 100 : 0,
  };
}

/** Variación porcentual contra el período anterior. null si antes era 0 y ahora no (no hay base para comparar). */
export function variacion(actual: number, previo: number): number | null {
  if (previo === 0) return actual === 0 ? 0 : null;
  return ((actual - previo) / previo) * 100;
}

export interface PuntoSerie {
  clave: string;
  etiqueta: string;
  detalle: string;
  ingresos: number;
  pedidos: number;
}

export type Granularidad = 'dia' | 'semana' | 'mes';

export function granularidadPara(dias: number): Granularidad {
  return dias <= 62 ? 'dia' : dias <= 400 ? 'semana' : 'mes';
}

const corto = (d: Date): string => `${d.getDate()} ${MESES[d.getMonth()]}`;

/** Ventas agrupadas por día, semana o mes, con todos los casilleros (también los vacíos) para que la línea sea continua. */
export function serie(lista: Pedido[], desde: Date, hasta: Date): { puntos: PuntoSerie[]; granularidad: Granularidad } {
  const dias = diasEntre(desde, hasta);
  const g = granularidadPara(dias);
  const claveDe = (d: Date): string => (g === 'dia' ? aISO(d) : g === 'semana' ? aISO(sumarDias(d, -diaSemana(d))) : `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);

  const mapa = new Map<string, PuntoSerie>();
  let cursor = inicioDia(desde);
  const fin = inicioDia(hasta);
  while (cursor <= fin) {
    const k = claveDe(cursor);
    if (!mapa.has(k)) {
      const base = g === 'mes' ? new Date(cursor.getFullYear(), cursor.getMonth(), 1) : g === 'semana' ? sumarDias(cursor, -diaSemana(cursor)) : cursor;
      const etiqueta = g === 'mes' ? `${MESES[base.getMonth()]} ${String(base.getFullYear()).slice(2)}` : corto(base);
      const detalle = g === 'dia' ? `${corto(base)} ${base.getFullYear()}` : g === 'semana' ? `Semana del ${corto(base)}` : `${MESES[base.getMonth()]} ${base.getFullYear()}`;
      mapa.set(k, { clave: k, etiqueta, detalle, ingresos: 0, pedidos: 0 });
    }
    cursor = sumarDias(cursor, 1);
  }
  for (const p of lista.filter(esValido)) {
    const punto = mapa.get(claveDe(new Date(p.fecha)));
    if (punto) {
      punto.ingresos += p.total;
      punto.pedidos += 1;
    }
  }
  return { puntos: [...mapa.values()], granularidad: g };
}

export function porDiaSemana(lista: Pedido[]): { dia: string; ingresos: number; pedidos: number }[] {
  const filas = DIAS_SEMANA.map((dia) => ({ dia, ingresos: 0, pedidos: 0 }));
  for (const p of lista.filter(esValido)) {
    const f = filas[diaSemana(new Date(p.fecha))];
    f.ingresos += p.total;
    f.pedidos += 1;
  }
  return filas;
}

/** Cantidad de pedidos por día de la semana (filas) y hora (columnas). */
export function mapaDeCalor(lista: Pedido[], horaDesde: number, horaHasta: number): { dia: string; celdas: { hora: number; n: number }[] }[] {
  const filas = DIAS_SEMANA.map((dia) => ({ dia, celdas: Array.from({ length: horaHasta - horaDesde + 1 }, (_, i) => ({ hora: horaDesde + i, n: 0 })) }));
  for (const p of lista.filter(esValido)) {
    const f = new Date(p.fecha);
    const celda = filas[diaSemana(f)].celdas.find((c) => c.hora === f.getHours());
    if (celda) celda.n += 1;
  }
  return filas;
}

export function masVendidos(lista: Pedido[], limite: number): { nombre: string; unidades: number; ingresos: number }[] {
  const mapa = new Map<string, { nombre: string; unidades: number; ingresos: number }>();
  for (const p of lista.filter(esValido)) {
    for (const d of p.detalles) {
      const f = mapa.get(d.productoNombre) ?? { nombre: d.productoNombre, unidades: 0, ingresos: 0 };
      f.unidades += d.cantidad;
      f.ingresos += d.subtotal;
      mapa.set(d.productoNombre, f);
    }
  }
  return [...mapa.values()].sort((a, b) => b.unidades - a.unidades || b.ingresos - a.ingresos).slice(0, limite);
}

/**
 * Pedidos en CSV que abre bien en Excel. Excel en español (Perú) separa columnas con ";", no con ",";
 * por eso se usa ";" y la línea `sep=;` (Excel la lee sea cual sea la configuración regional). Va con BOM
 * para las tildes, fechas sin "a. m./p. m." y números sin comillas para que Excel los trate como números.
 */
export function aCsv(lista: Pedido[], etiquetaEstado: Record<string, string>): string {
  const texto = (v: string) => `"${v.replace(/"/g, '""')}"`;
  const dos = (n: number) => String(n).padStart(2, '0');
  const fecha = (iso: string) => {
    const d = new Date(iso);
    return `${dos(d.getDate())}/${dos(d.getMonth() + 1)}/${d.getFullYear()} ${dos(d.getHours())}:${dos(d.getMinutes())}`;
  };
  // Un código como "12E45678" lo leería Excel como notación científica.
  const codigo = (id: string) => {
    const c = id.slice(0, 8).toUpperCase();
    return /^\d+E\d+$/.test(c) ? `="${c}"` : texto(c);
  };
  const filas: string[] = [['Pedido', 'Fecha', 'Estado', 'Productos', 'Unidades', 'Total (S/)'].map(texto).join(';')];
  for (const p of lista) {
    filas.push([
      codigo(p.id),
      texto(fecha(p.fecha)),
      texto(etiquetaEstado[p.estado] ?? p.estado),
      texto(p.detalles.map((d) => `${d.cantidad} x ${d.productoNombre}`).join(' | ')),
      String(p.detalles.reduce((u, d) => u + d.cantidad, 0)),
      p.total.toFixed(2),
    ].join(';'));
  }
  return '\ufeffsep=;\r\n' + filas.join('\r\n');
}

/** "hace 5 min", "hace 3 h", "ayer", "hace 4 días", o la fecha corta. */
export function haceCuanto(fecha: string, ahora = new Date()): string {
  const min = Math.floor((ahora.getTime() - new Date(fecha).getTime()) / 60_000);
  if (min < 1) return 'ahora mismo';
  if (min < 60) return `hace ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `hace ${h} h`;
  const d = Math.floor(h / 24);
  if (d === 1) return 'ayer';
  if (d < 7) return `hace ${d} días`;
  return corto(new Date(fecha));
}
