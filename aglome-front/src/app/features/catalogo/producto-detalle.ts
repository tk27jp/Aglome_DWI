import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, ElementRef, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { negocio } from '../../../environments/negocio';
import { ApiError, Producto, Variante } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { CarritoService } from '../../core/services/carrito.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { ProductoService } from '../../core/services/producto.service';
import { colorDe } from '../../shared/colores';
import { ahorro, atributosVisibles, descuento, precioAnterior } from '../../shared/oferta';
import { ProductoImagen } from '../../shared/producto-imagen';
import { variantesActivas } from '../../shared/producto-utils';
import { Revelar } from '../../shared/revelar';
import { ProductoCard } from './producto-card';

const STOCK_BAJO = 5;
/** La barra de stock se llena con este número de unidades. */
const STOCK_BARRA = 20;
/** Margen para cortes y errores al calcular cuántas planchas hacen falta. */
const DESPERDICIO = 0.1;

type Pestana = 'descripcion' | 'ficha' | 'compra';

@Component({
  selector: 'app-producto-detalle',
  imports: [RouterLink, CurrencyPipe, DecimalPipe, ProductoCard, ProductoImagen, Revelar],
  templateUrl: './producto-detalle.html',
  styleUrl: './producto-detalle.scss',
})
export class ProductoDetalle {
  private readonly servicio = inject(ProductoService);
  protected readonly auth = inject(AuthService);
  private readonly carrito = inject(CarritoService);
  private readonly avisos = inject(NotificacionService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Viene de la ruta /productos/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly negocio = negocio;
  protected readonly desperdicio = DESPERDICIO * 100;
  protected readonly producto = signal<Producto | null>(null);
  protected readonly relacionados = signal<Producto[]>([]);
  /** Muestra del catálogo para armar "combina con" y "recomendados". Si falla, esas secciones simplemente no salen. */
  private readonly catalogo = signal<Producto[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly seleccionada = signal<Variante | null>(null);
  protected readonly cantidad = signal(1);
  protected readonly agregando = signal(false);
  protected readonly agregado = signal(false);
  protected readonly pestana = signal<Pestana>('descripcion');
  protected readonly area = signal<number | null>(null);
  /** Posición del mouse sobre la foto, para ampliarla donde se apunta. */
  protected readonly lupa = signal({ x: 50, y: 50 });

  protected readonly variantes = computed(() => (this.producto() ? variantesActivas(this.producto()!) : []));

  /** Colores distintos entre las opciones, en el orden en que aparecen. */
  protected readonly colores = computed(() => [...new Set(this.variantes().map((v) => this.colorDeVariante(v)).filter((c): c is string => !!c))]);
  protected readonly colorElegido = computed(() => { const v = this.seleccionada(); return v ? this.colorDeVariante(v) : null; });
  /** Con muestras de color, las filas muestran solo las opciones del color elegido (medidas). */
  protected readonly opcionesVisibles = computed(() => {
    const c = this.colorElegido();
    return this.colores().length && c ? this.variantes().filter((v) => this.colorDeVariante(v) === c) : this.variantes();
  });

  protected readonly estadoStock = computed(() => {
    const v = this.seleccionada();
    if (!v) return null;
    if (v.stock <= 0) return { clase: 'agotado', texto: 'Agotado' };
    if (v.stock <= STOCK_BAJO) return { clase: 'poco', texto: `Quedan ${v.stock} unidades` };
    return { clase: 'ok', texto: 'Disponible' };
  });

  protected readonly subtotal = computed(() => (this.seleccionada()?.precioVenta ?? 0) * this.cantidad());
  /** Foto que se está mirando: la de la opción elegida, o la que se llegó con las flechas (portada del producto). */
  protected readonly vista = signal<string | null>(null);
  protected readonly fotoActual = computed(() => this.vista() ?? this.seleccionada()?.imagenUrl ?? this.producto()?.imagenUrl ?? null);
  protected readonly conFoto = computed(() => !!this.fotoActual());
  /** Color de la opción elegida, para pintar la vista previa cuando aún no hay foto. */
  protected readonly colorVista = computed(() => {
    const v = this.seleccionada();
    return v ? this.colorDeVariante(v) : null;
  });

  /** Fotos del producto, una por color (o por opción si no hay colores), más la portada. Las flechas recorren esta lista. */
  protected readonly galeria = computed(() => {
    const p = this.producto();
    const lista: { url: string; color: string | null; variante: Variante | null }[] = [];
    if (p?.imagenUrl) lista.push({ url: p.imagenUrl, color: null, variante: null });
    const vistos = new Set<string>();
    for (const v of this.variantes()) {
      const color = this.colorDeVariante(v);
      const clave = color ?? v.sku;
      if (!v.imagenUrl || vistos.has(clave) || lista.some((f) => f.url === v.imagenUrl)) continue;
      vistos.add(clave);
      lista.push({ url: v.imagenUrl, color, variante: v });
    }
    return lista;
  });
  protected readonly posicion = computed(() => Math.max(0, this.galeria().findIndex((f) => f.url === this.fotoActual())));

  /** Flechas de la foto: pasa a la siguiente o anterior; si esa foto es de un color, también elige ese color. */
  protected pasar(paso: number, e?: Event): void {
    e?.stopPropagation();
    const g = this.galeria();
    if (g.length < 2) return;
    this.ir((this.posicion() + paso + g.length) % g.length);
  }

  protected ir(i: number): void {
    const f = this.galeria()[i];
    if (!f) return;
    if (f.color) {
      this.elegirColor(f.color);
      if (this.seleccionada()?.imagenUrl !== f.url) this.vista.set(f.url);
    } else if (f.variante) this.elegir(f.variante);
    else this.vista.set(f.url);
  }

  /** Medidas de la plancha elegida (ej. "2.14m x 2.50m"), si es un tablero y la medida se puede leer. */
  protected readonly plancha = computed(() => {
    const p = this.producto();
    const v = this.seleccionada();
    if (!p || !v || !/(melamin|mdf|triplay|tablero|plancha|aglomerado)/.test(this.normalizar(`${p.nombre} ${p.categoriaNombre ?? ''}`))) return null;
    const texto = atributosVisibles(v.atributos).map(([, x]) => x).join(' ');
    const m = texto.match(/(\d+(?:[.,]\d+)?)\s*(?:mm|cm|m)?\s*[x×]\s*(\d+(?:[.,]\d+)?)\s*(?:mm|cm|m)?/i);
    if (!m) return null;
    let a = parseFloat(m[1].replace(',', '.'));
    let b = parseFloat(m[2].replace(',', '.'));
    const escala = a >= 500 || b >= 500 ? 1000 : a >= 20 || b >= 20 ? 100 : 1;
    a /= escala;
    b /= escala;
    return a * b >= 0.3 && a * b <= 20 ? { ancho: a, alto: b, area: a * b } : null;
  });

  protected readonly planchasNecesarias = computed(() => {
    const area = this.area();
    const pl = this.plancha();
    return area && area > 0 && pl ? Math.ceil((area * (1 + DESPERDICIO)) / pl.area) : null;
  });

  private static readonly BASE = /(melamin|madera|mdf|triplay|tablero|plancha|aglomerado)/;
  private static readonly HERRAJE = /(bisagra|corredera|tornill|canto|jalador|tirador|ferreter|cerradura|clavo|pegamento|barniz|laca|acabado)/;

  private texto(p: Producto): string {
    return this.normalizar(`${p.nombre} ${p.categoriaNombre ?? ''}`);
  }

  private otros(): Producto[] {
    const actual = this.producto()?.id;
    return this.catalogo().filter((p) => p.id !== actual && variantesActivas(p).length > 0);
  }

  protected readonly relacionadosVista = computed(() => {
    const dados = this.relacionados().filter((r) => r.id !== this.producto()?.id);
    if (dados.length) return dados.slice(0, 4);
    const cat = this.producto()?.categoriaId;
    return cat == null ? [] : this.otros().filter((p) => p.categoriaId === cat).slice(0, 4);
  });

  protected readonly combinan = computed(() => {
    const p = this.producto();
    if (!p) return [];
    const t = this.texto(p);
    const usados = new Set(this.relacionadosVista().map((r) => r.id));
    const quiere = ProductoDetalle.BASE.test(t) ? ProductoDetalle.HERRAJE : ProductoDetalle.HERRAJE.test(t) ? ProductoDetalle.BASE : null;
    return quiere ? this.otros().filter((o) => !usados.has(o.id) && quiere.test(this.texto(o))).slice(0, 4) : [];
  });

  protected readonly recomendados = computed(() => {
    const usados = new Set([...this.relacionadosVista(), ...this.combinan()].map((r) => r.id));
    return this.otros()
      .filter((o) => !usados.has(o.id))
      .sort((a, b) => (b.fechaCreacion ?? '').localeCompare(a.fechaCreacion ?? ''))
      .slice(0, 5);
  });

  protected readonly enlaceConsulta = computed(() => {
    const p = this.producto();
    const v = this.seleccionada();
    const detalle = v ? ` (${atributosVisibles(v.atributos).map(([, x]) => x).join(', ') || v.sku})` : '';
    return p ? `https://wa.me/${negocio.whatsapp}?text=${encodeURIComponent(`Hola, quiero consultar por: ${p.nombre}${detalle}. ${window.location.href}`)}` : '#';
  });

  protected readonly ficha = computed(() => {
    const p = this.producto();
    const v = this.seleccionada();
    if (!p) return [];
    const filas: { nombre: string; valor: string }[] = [];
    if (p.marca) filas.push({ nombre: 'Marca', valor: p.marca });
    if (p.categoriaNombre) filas.push({ nombre: 'Categoría', valor: p.categoriaNombre });
    if (v) {
      for (const a of this.atributos(v)) filas.push(a);
      filas.push({ nombre: 'Código (SKU)', valor: v.sku });
    }
    return filas;
  });

  constructor() {
    effect((limpiar) => {
      const id = this.id();
      this.cargando.set(true);
      this.error.set(null);
      this.relacionados.set([]);
      this.catalogo.set([]);
      this.pestana.set('descripcion');
      this.area.set(null);
      this.cantidad.set(1);

      const s1 = this.servicio.obtener(id).subscribe({
        next: (p) => {
          this.producto.set(p);
          const activas = variantesActivas(p);
          this.vista.set(null);
          this.seleccionada.set(activas.find((v) => v.stock > 0) ?? activas[0] ?? null);
          this.cargando.set(false);
        },
        error: (e: ApiError) => {
          this.producto.set(null);
          this.error.set(e);
          this.cargando.set(false);
        },
      });
      // Los relacionados son un extra: si fallan, simplemente no se muestran.
      const s2 = this.servicio.relacionados(id).subscribe({ next: (r) => this.relacionados.set(r), error: () => undefined });
      const s3 = this.servicio.filtrar({ tamano: 40 }).subscribe({ next: (pg) => this.catalogo.set(pg.contenido), error: () => undefined });
      limpiar(() => {
        s1.unsubscribe();
        s2.unsubscribe();
        s3.unsubscribe();
      });
    });
  }

  protected elegir(v: Variante): void {
    this.vista.set(null);
    this.seleccionada.set(v);
    this.cantidad.set(1);
  }

  /** Al elegir un color, se queda con la misma medida si existe; si no, con la primera opción con stock. */
  protected elegirColor(color: string): void {
    const actual = this.seleccionada();
    const delColor = this.variantes().filter((v) => this.colorDeVariante(v) === color);
    const misma = actual && delColor.find((v) => this.atributosSin(v).map((a) => a.valor).join('|') === this.atributosSin(actual).map((a) => a.valor).join('|'));
    this.elegir(misma ?? delColor.find((v) => v.stock > 0) ?? delColor[0]);
  }

  protected cambiarCantidad(delta: number): void {
    const maximo = this.seleccionada()?.stock ?? 1;
    this.cantidad.update((c) => Math.min(Math.max(1, c + delta), Math.max(1, maximo)));
  }

  protected leerArea(texto: string): void {
    const n = parseFloat(texto.replace(',', '.'));
    this.area.set(Number.isFinite(n) && n > 0 ? n : null);
  }

  protected usarCalculo(): void {
    const n = this.planchasNecesarias();
    const v = this.seleccionada();
    if (!n || !v) return;
    this.cantidad.set(Math.min(n, Math.max(1, v.stock)));
    if (n > v.stock) this.avisos.mostrar(`Para esa área hacen falta ${n} planchas y hay ${v.stock} en stock. Dejamos el máximo disponible.`, 'info', 6500);
  }

  protected mover(e: PointerEvent): void {
    const caja = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.lupa.set({ x: ((e.clientX - caja.left) / caja.width) * 100, y: ((e.clientY - caja.top) / caja.height) * 100 });
  }

  protected agregar(): void {
    const v = this.seleccionada();
    if (!v) return;
    if (!this.auth.token()) {
      this.avisos.mostrar('Ingresa o crea tu cuenta para agregar productos al carrito.', 'info');
      this.router.navigate(['/ingresar'], { queryParams: { retorno: this.router.url } });
      return;
    }
    this.agregando.set(true);
    const vuelo = this.volar(colorDe(this.colorElegido() ?? this.producto()?.nombre));
    this.carrito.agregar(v.id, this.cantidad()).subscribe({
      next: () => {
        this.agregando.set(false);
        this.agregado.set(true);
        setTimeout(() => this.agregado.set(false), 1600);
        vuelo.then(() => this.carrito.panelAbierto.set(true));
      },
      error: (e: ApiError) => {
        this.agregando.set(false);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected comprarAhora(): void {
    const v = this.seleccionada();
    if (!v) return;
    if (!this.auth.token()) return this.agregar();
    this.agregando.set(true);
    this.carrito.agregar(v.id, this.cantidad()).subscribe({
      next: () => { this.agregando.set(false); this.router.navigate(['/carrito']); },
      error: (e: ApiError) => { this.agregando.set(false); this.avisos.mostrar(e.mensaje); },
    });
  }

  protected atributos(v: Variante): { nombre: string; valor: string }[] {
    return atributosVisibles(v.atributos).map(([nombre, valor]) => ({ nombre, valor }));
  }

  protected readonly anterior = precioAnterior;
  protected readonly pct = descuento;
  protected readonly ahorra = ahorro;

  protected atributosSin(v: Variante): { nombre: string; valor: string }[] {
    return this.colores().length ? this.atributos(v).filter((a) => !this.esColor(a.nombre)) : this.atributos(v);
  }

  private esColor(nombre: string): boolean {
    return this.normalizar(nombre) === 'color';
  }

  private colorDeVariante(v: Variante): string | null {
    const par = Object.entries(v.atributos ?? {}).find(([nombre]) => this.esColor(nombre));
    return par ? par[1] : null;
  }

  private normalizar(texto: string): string {
    return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }

  protected tono(color: string): string {
    return colorDe(color);
  }

  private volar(color: string): Promise<void> {
    const origen = this.host.nativeElement.querySelector('.imagen');
    const destino = document.querySelector('.accion.carrito');
    if (!origen || !destino || matchMedia('(prefers-reduced-motion: reduce)').matches) return Promise.resolve();
    const a = origen.getBoundingClientRect();
    const b = destino.getBoundingClientRect();
    const x0 = a.left + a.width / 2;
    const y0 = Math.min(Math.max(a.top + a.height / 2, 80), window.innerHeight - 80);
    const dx = b.left + b.width / 2 - x0;
    const dy = b.top + b.height / 2 - y0;
    const punto = document.createElement('div');
    Object.assign(punto.style, {
      position: 'fixed', left: `${x0 - 28}px`, top: `${y0 - 28}px`, width: '56px', height: '56px', background: color,
      border: '3px solid #fff', borderRadius: '3px', boxShadow: '0 10px 28px rgb(0 0 0 / 35%)', zIndex: '100', pointerEvents: 'none',
    });
    document.body.appendChild(punto);
    const anim = punto.animate(
      [
        { transform: 'translate(0, 0) scale(1)', opacity: 1 },
        { transform: `translate(${dx * 0.55}px, ${dy * 0.55 - 70}px) scale(0.7)`, opacity: 1, offset: 0.55 },
        { transform: `translate(${dx}px, ${dy}px) scale(0.2)`, opacity: 0.5 },
      ],
      { duration: 750, easing: 'cubic-bezier(0.45, 0, 0.55, 1)' },
    );
    return anim.finished.then(() => punto.remove(), () => punto.remove());
  }
}
