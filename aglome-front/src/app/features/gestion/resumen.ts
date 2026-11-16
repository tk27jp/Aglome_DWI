import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { EstadoPedido, Pedido, ResumenDashboard, VentaCategoria } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { GestionService } from '../../core/services/gestion.service';
import { ETIQUETA_ESTADO, codigoCorto } from '../../shared/estados';
import { aCsv, aISO, deISO, diasEntre, enRango, haceCuanto, inicioDia, mapaDeCalor, masVendidos, porDiaSemana, serie, sumarDias, totales, variacion } from './analisis';
import { Contador } from './contador';

type Preset = 'hoy' | '7' | '30' | 'mes' | '90' | 'anio' | 'todo' | 'otro';
type Metrica = 'ingresos' | 'pedidos';

const PRESETS: { valor: Preset; texto: string }[] = [
  { valor: 'hoy', texto: 'Hoy' },
  { valor: '7', texto: '7 días' },
  { valor: '30', texto: '30 días' },
  { valor: 'mes', texto: 'Este mes' },
  { valor: '90', texto: '90 días' },
  { valor: 'anio', texto: 'Este año' },
  { valor: 'todo', texto: 'Todo' },
];

const COLOR_ESTADO: Record<EstadoPedido, string> = {
  PENDIENTE: '#7e7e7e',
  CONFIRMADO: '#0049af',
  EN_PREPARACION: '#c8892b',
  ENVIADO: '#5b8fd6',
  ENTREGADO: '#1e8e3e',
  CANCELADO: '#c62828',
};
const ORDEN_ESTADOS: EstadoPedido[] = ['CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO', 'PENDIENTE', 'CANCELADO'];

// Medidas del gráfico principal (unidades del viewBox)
const W = 760;
const H = 280;
const ML = 56;
const MR = 14;
const MT = 14;
const MB = 34;
const CLAVE_META = 'aglome.meta-mensual';

const corto = (n: number): string => (n >= 1000 ? `${(n / 1000).toLocaleString('es-PE', { maximumFractionDigits: 1 })} mil` : n.toLocaleString('es-PE', { maximumFractionDigits: 0 }));

function maximoLimpio(max: number, minimo: number): number {
  if (max <= 0) return minimo;
  const pot = Math.pow(10, Math.floor(Math.log10(max)));
  const f = max / pot;
  return Math.max(minimo, (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * pot);
}

function leerMeta(): number {
  try {
    return Number(localStorage.getItem(CLAVE_META)) || 0;
  } catch {
    return 0;
  }
}

@Component({
  selector: 'app-resumen',
  imports: [CurrencyPipe, RouterLink, Contador],
  templateUrl: './resumen.html',
  styleUrl: './resumen.scss',
})
export class Resumen {
  private readonly servicio = inject(GestionService);
  protected readonly auth = inject(AuthService);

  protected readonly presets = PRESETS;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly codigo = codigoCorto;
  protected readonly haceCuanto = haceCuanto;
  protected readonly corto = corto;
  protected readonly W = W;
  protected readonly H = H;

  protected readonly pedidos = signal<Pedido[] | null>(null);
  protected readonly dash = signal<ResumenDashboard | null>(null);
  protected readonly categorias = signal<VentaCategoria[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly ahora = signal(new Date());

  protected readonly preset = signal<Preset>('30');
  protected readonly desdeTxt = signal('');
  protected readonly hastaTxt = signal('');
  protected readonly metrica = signal<Metrica>('ingresos');
  protected readonly hover = signal<number | null>(null);
  protected readonly meta = signal(leerMeta());
  protected readonly editandoMeta = signal(false);

  // ── Rango y comparación con el período anterior
  protected readonly rango = computed(() => {
    const a = deISO(this.desdeTxt() || aISO(new Date()));
    const b = deISO(this.hastaTxt() || aISO(new Date()));
    return a <= b ? { desde: a, hasta: b } : { desde: b, hasta: a };
  });
  protected readonly dias = computed(() => diasEntre(this.rango().desde, this.rango().hasta));
  private readonly rangoPrevio = computed(() => {
    const hasta = sumarDias(this.rango().desde, -1);
    return { desde: sumarDias(hasta, -(this.dias() - 1)), hasta };
  });
  protected readonly textoPrevio = computed(() => {
    const r = this.rangoPrevio();
    const f = (d: Date) => d.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' });
    return `${f(r.desde)} – ${f(r.hasta)}`;
  });

  protected readonly actuales = computed(() => enRango(this.pedidos() ?? [], this.rango().desde, this.rango().hasta));
  private readonly previos = computed(() => enRango(this.pedidos() ?? [], this.rangoPrevio().desde, this.rangoPrevio().hasta));
  protected readonly tot = computed(() => totales(this.actuales()));
  private readonly totPrevio = computed(() => totales(this.previos()));

  // ── Gráfico principal
  protected readonly datosSerie = computed(() => serie(this.actuales(), this.rango().desde, this.rango().hasta));

  protected readonly kpis = computed(() => {
    const t = this.tot();
    const p = this.totPrevio();
    const puntos = this.datosSerie().puntos;
    return [
      { id: 'ingresos', etiqueta: 'Ingresos', valor: t.ingresos, formato: 'moneda' as const, delta: variacion(t.ingresos, p.ingresos), invertido: false, nota: 'sin pedidos cancelados', spark: puntos.map((x) => x.ingresos) },
      { id: 'pedidos', etiqueta: 'Pedidos', valor: t.pedidos, formato: 'entero' as const, delta: variacion(t.pedidos, p.pedidos), invertido: false, nota: 'sin contar cancelados', spark: puntos.map((x) => x.pedidos) },
      { id: 'ticket', etiqueta: 'Ticket promedio', valor: t.ticket, formato: 'moneda' as const, delta: variacion(t.ticket, p.ticket), invertido: false, nota: 'por pedido', spark: puntos.map((x) => (x.pedidos ? x.ingresos / x.pedidos : 0)) },
      { id: 'unidades', etiqueta: 'Unidades vendidas', valor: t.unidades, formato: 'entero' as const, delta: variacion(t.unidades, p.unidades), invertido: false, nota: 'productos entregados al cliente', spark: [] as number[] },
      { id: 'cancel', etiqueta: 'Cancelaciones', valor: t.tasaCancelacion, formato: 'porcentaje' as const, delta: variacion(t.tasaCancelacion, p.tasaCancelacion), invertido: true, nota: `${t.cancelados} ${t.cancelados === 1 ? 'pedido cancelado' : 'pedidos cancelados'}`, spark: [] as number[] },
    ];
  });

  private readonly valores = computed(() => this.datosSerie().puntos.map((p) => (this.metrica() === 'ingresos' ? p.ingresos : p.pedidos)));
  protected readonly grafico = computed(() => {
    const v = this.valores();
    const n = v.length;
    const iw = W - ML - MR;
    const ih = H - MT - MB;
    const max = maximoLimpio(Math.max(0, ...v), this.metrica() === 'pedidos' ? 4 : 100);
    const x = (i: number) => (n <= 1 ? ML + iw / 2 : ML + (i * iw) / (n - 1));
    const y = (val: number) => MT + ih - (val / max) * ih;
    const xy = v.map((val, i) => ({ x: x(i), y: y(val) }));
    const linea = xy.map((q, i) => `${i ? 'L' : 'M'}${q.x.toFixed(1)} ${q.y.toFixed(1)}`).join(' ');
    const base = MT + ih;
    const area = n ? `${linea} L${xy[n - 1].x.toFixed(1)} ${base} L${xy[0].x.toFixed(1)} ${base} Z` : '';
    const ticks = [0, 1, 2, 3, 4].map((k) => ({ y: y((max / 4) * k), texto: this.metrica() === 'pedidos' ? String(Math.round((max / 4) * k)) : corto((max / 4) * k) }));
    const paso = Math.max(1, Math.ceil(n / 7));
    const etiquetas = this.datosSerie().puntos.map((p, i) => ({ x: x(i), texto: p.etiqueta, i })).filter((e) => e.i % paso === 0);
    return { xy, linea, area, ticks, etiquetas, base, paso: n <= 1 ? iw : iw / (n - 1), hayVentas: v.some((q) => q > 0) };
  });
  protected readonly punto = computed(() => {
    const i = this.hover();
    const g = this.grafico();
    const d = i === null ? null : this.datosSerie().puntos[i];
    return d && g.xy[i!] ? { d, x: g.xy[i!].x, y: g.xy[i!].y, i: i! } : null;
  });

  // ── Estados (dona)
  protected readonly estados = computed(() => {
    const lista = this.actuales();
    const total = lista.length || 1;
    let acumulado = 0;
    return ORDEN_ESTADOS.map((e) => ({ estado: e, n: lista.filter((p) => p.estado === e).length }))
      .filter((e) => e.n > 0)
      .map((e) => {
        const pct = (e.n / total) * 100;
        const seg = { ...e, etiqueta: ETIQUETA_ESTADO[e.estado], color: COLOR_ESTADO[e.estado], pct, desfase: 25 - acumulado };
        acumulado += pct;
        return seg;
      });
  });

  // ── Días y horas con más movimiento
  protected readonly semana = computed(() => {
    const f = porDiaSemana(this.actuales());
    const max = Math.max(1, ...f.map((x) => x.ingresos));
    const mejor = f.reduce((m, x) => (x.ingresos > m.ingresos ? x : m), f[0]);
    return { filas: f.map((x) => ({ ...x, alto: (x.ingresos / max) * 100 })), mejor: mejor.ingresos > 0 ? mejor.dia : null };
  });
  protected readonly calor = computed(() => {
    const f = mapaDeCalor(this.actuales(), 7, 20);
    const max = Math.max(1, ...f.flatMap((x) => x.celdas.map((c) => c.n)));
    return { filas: f, max, horas: f[0].celdas.map((c) => c.hora) };
  });

  // ── Productos y categorías
  protected readonly top = computed(() => {
    const lista = masVendidos(this.actuales(), 6);
    const max = Math.max(1, ...lista.map((p) => p.unidades));
    return lista.map((p) => ({ ...p, ancho: (p.unidades / max) * 100 }));
  });
  protected readonly porCategoria = computed(() => {
    const lista = [...this.categorias()].sort((a, b) => b.unidades - a.unidades).slice(0, 6);
    const max = Math.max(1, ...lista.map((c) => c.unidades));
    return lista.map((c) => ({ ...c, ancho: (c.unidades / max) * 100 }));
  });

  // ── Actividad
  protected readonly recientes = computed(() => [...(this.pedidos() ?? [])].sort((a, b) => b.fecha.localeCompare(a.fecha)).slice(0, 6));
  protected readonly porAtender = computed(() => (this.pedidos() ?? []).filter((p) => p.estado === 'CONFIRMADO' || p.estado === 'EN_PREPARACION').length);

  // ── Meta del mes
  protected readonly ingresosMes = computed(() => {
    const a = this.ahora();
    return totales(enRango(this.pedidos() ?? [], new Date(a.getFullYear(), a.getMonth(), 1), a)).ingresos;
  });
  protected readonly avanceMeta = computed(() => (this.meta() > 0 ? Math.min(100, (this.ingresosMes() / this.meta()) * 100) : 0));
  protected readonly nombreMes = computed(() => this.ahora().toLocaleDateString('es-PE', { month: 'long' }));

  protected readonly saludo = computed(() => {
    const h = this.ahora().getHours();
    return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  });
  protected readonly fechaLarga = computed(() => this.ahora().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }));

  constructor() {
    this.aplicar('30');
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    forkJoin({
      pedidos: this.servicio.pedidos().pipe(catchError(() => of(null))),
      dash: this.servicio.dashboard().pipe(catchError(() => of(null))),
      categorias: this.servicio.ventasPorCategoria().pipe(catchError(() => of([] as VentaCategoria[]))),
    }).subscribe(({ pedidos, dash, categorias }) => {
      this.cargando.set(false);
      if (!pedidos && !dash) {
        this.error.set('No se pudieron cargar los datos del resumen. Revisa tu conexión e intenta de nuevo.');
        return;
      }
      this.ahora.set(new Date());
      this.pedidos.set(pedidos ?? []);
      this.dash.set(dash);
      this.categorias.set(categorias);
      if (this.preset() === 'todo') this.aplicar('todo');
    });
  }

  protected aplicar(p: Preset): void {
    const hoy = inicioDia(new Date());
    let desde = hoy;
    switch (p) {
      case 'hoy': desde = hoy; break;
      case '7': desde = sumarDias(hoy, -6); break;
      case '30': desde = sumarDias(hoy, -29); break;
      case '90': desde = sumarDias(hoy, -89); break;
      case 'mes': desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1); break;
      case 'anio': desde = new Date(hoy.getFullYear(), 0, 1); break;
      case 'todo': {
        const fechas = (this.pedidos() ?? []).map((x) => new Date(x.fecha).getTime());
        desde = fechas.length ? inicioDia(new Date(Math.min(...fechas))) : sumarDias(hoy, -29);
        break;
      }
    }
    this.preset.set(p);
    this.desdeTxt.set(aISO(desde));
    this.hastaTxt.set(aISO(hoy));
    this.hover.set(null);
  }

  protected cambiarFecha(cual: 'desde' | 'hasta', valor: string): void {
    if (!valor) return;
    (cual === 'desde' ? this.desdeTxt : this.hastaTxt).set(valor);
    this.preset.set('otro');
    this.hover.set(null);
  }

  protected mover(ev: PointerEvent): void {
    const g = this.grafico();
    const caja = (ev.currentTarget as SVGElement).getBoundingClientRect();
    const x = ((ev.clientX - caja.left) / caja.width) * W;
    const i = Math.round((x - ML) / (g.paso || 1));
    this.hover.set(Math.max(0, Math.min(g.xy.length - 1, i)));
  }

  protected guardarMeta(texto: string): void {
    const n = Math.max(0, Number(texto) || 0);
    this.meta.set(n);
    this.editandoMeta.set(false);
    try {
      localStorage.setItem(CLAVE_META, String(n));
    } catch {
      /* sin almacenamiento: la meta solo dura esta visita */
    }
  }

  protected exportar(): void {
    const csv = aCsv(this.actuales(), ETIQUETA_ESTADO);
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `pedidos-aglome-${this.desdeTxt()}_a_${this.hastaTxt()}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  /** Línea mínima (sparkline) dentro de un cuadro de 100×28. */
  protected spark(v: number[]): string {
    if (v.length < 2) return '';
    const max = Math.max(...v, 1);
    return v.map((n, i) => `${i ? 'L' : 'M'}${((i / (v.length - 1)) * 100).toFixed(1)} ${(26 - (n / max) * 24).toFixed(1)}`).join(' ');
  }

  protected fmt(d: number): string {
    return Math.abs(d).toLocaleString('es-PE', { maximumFractionDigits: 1 }) + ' %';
  }

  protected claseDelta(d: number | null, invertido: boolean): string {
    if (d === null || d === 0) return 'neutro';
    return (d > 0) !== invertido ? 'bien' : 'mal';
  }
}
