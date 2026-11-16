import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { ApiError, MovimientoStock, Pagina, Producto, TipoMovimiento, Variante } from '../../core/models/api.models';
import { GestionService } from '../../core/services/gestion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { ProductoService } from '../../core/services/producto.service';
import { Paginacion } from '../../shared/paginacion';
import { variantesActivas } from '../../shared/producto-utils';

interface Elegida {
  producto: string;
  variante: Variante;
}

const TIPOS: { valor: TipoMovimiento | ''; texto: string }[] = [
  { valor: '', texto: 'Todos los movimientos' },
  { valor: 'VENTA', texto: 'Ventas' },
  { valor: 'CANCELACION', texto: 'Cancelaciones' },
  { valor: 'AJUSTE', texto: 'Ajustes manuales' },
  { valor: 'STOCK_INICIAL', texto: 'Stock inicial' },
];
const ETIQUETA_TIPO: Record<TipoMovimiento, string> = { VENTA: 'Venta', CANCELACION: 'Cancelación', AJUSTE: 'Ajuste', STOCK_INICIAL: 'Stock inicial' };

@Component({
  selector: 'app-inventario',
  imports: [FormsModule, DatePipe, Paginacion],
  templateUrl: './inventario.html',
  styleUrl: './inventario.scss',
})
export class Inventario {
  private readonly servicio = inject(GestionService);
  private readonly productos = inject(ProductoService);
  private readonly avisos = inject(NotificacionService);

  /** Texto para buscar al entrar (viene de "Reponer" en el resumen). */
  readonly buscar = input<string>();

  protected readonly tipos = TIPOS;
  protected readonly etiquetaTipo = ETIQUETA_TIPO;
  protected readonly motivosRapidos = ['Reposición de proveedor', 'Merma o rotura', 'Conteo físico', 'Devolución'];

  // ── Ajuste de stock
  protected readonly texto = signal('');
  protected readonly resultados = toSignal(
    toObservable(this.texto).pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((t) => (t.trim().length < 2 ? of<Producto[] | null>(null) : this.productos.filtrar({ texto: t.trim(), tamano: 6 }).pipe(switchMap((p) => of<Producto[] | null>(p.contenido)), catchError(() => of<Producto[] | null>([]))))),
    ),
    { initialValue: null },
  );
  protected readonly elegida = signal<Elegida | null>(null);
  protected readonly entra = signal(true);
  protected readonly cantidad = signal<number | null>(null);
  protected readonly motivo = signal('');
  protected readonly guardando = signal(false);

  protected readonly delta = computed(() => (this.cantidad() ?? 0) * (this.entra() ? 1 : -1));
  protected readonly quedaria = computed(() => (this.elegida()?.variante.stock ?? 0) + this.delta());
  protected readonly problema = computed(() => {
    const e = this.elegida();
    if (!e) return 'Elige el producto y la opción que vas a ajustar.';
    const c = this.cantidad();
    if (!c || c < 1 || !Number.isInteger(c)) return 'Escribe una cantidad entera mayor que cero.';
    if (this.quedaria() < 0) return `Solo hay ${e.variante.stock} unidades: no se pueden sacar ${c}.`;
    if (!this.motivo().trim()) return 'Escribe el motivo del ajuste.';
    return null;
  });

  // ── Historial
  protected readonly tipo = signal<TipoMovimiento | ''>('');
  protected readonly pagina = signal(0);
  protected readonly movimientos = signal<Pagina<MovimientoStock> | null>(null);
  protected readonly errorMov = signal<string | null>(null);

  constructor() {
    effect(() => {
      const b = this.buscar();
      if (b) this.texto.set(b);
    });
    this.cargarMovimientos();
  }

  protected variantes(p: Producto): Variante[] {
    return variantesActivas(p);
  }

  protected nombreOpcion(v: Variante): string {
    return Object.values(v.atributos ?? {}).join(' · ') || v.sku;
  }

  protected elegir(p: Producto, v: Variante): void {
    this.elegida.set({ producto: p.nombre, variante: v });
    this.cantidad.set(null);
  }

  protected motivoRapido(m: string): void {
    this.motivo.set(m);
  }

  protected guardar(): void {
    const e = this.elegida();
    if (!e || this.problema()) return;
    this.guardando.set(true);
    this.servicio.ajustarStock({ varianteId: e.variante.id, cantidad: this.delta(), motivo: this.motivo().trim() }).subscribe({
      next: (mov) => {
        this.guardando.set(false);
        this.elegida.set({ ...e, variante: { ...e.variante, stock: mov.stockResultante } });
        this.cantidad.set(null);
        this.motivo.set('');
        this.avisos.ok(`Stock actualizado: ${mov.producto} quedó en ${mov.stockResultante}.`);
        this.pagina.set(0);
        this.cargarMovimientos();
      },
      error: (err: ApiError) => {
        this.guardando.set(false);
        this.avisos.mostrar(err.mensaje);
      },
    });
  }

  protected elegirTipo(t: string): void {
    this.tipo.set(t as TipoMovimiento | '');
    this.pagina.set(0);
    this.cargarMovimientos();
  }

  protected irA(n: number): void {
    this.pagina.set(n);
    this.cargarMovimientos();
  }

  protected cargarMovimientos(): void {
    this.errorMov.set(null);
    this.servicio.movimientos({ tipo: this.tipo(), pagina: this.pagina(), tamano: 10 }).subscribe({
      next: (p) => this.movimientos.set(p),
      error: (e: ApiError) => this.errorMov.set(e.mensaje),
    });
  }
}
