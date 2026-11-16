import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiError, Categoria, Marca, Producto, ProductoRequest, Variante, VarianteRequest } from '../../core/models/api.models';
import { CategoriasService } from '../../core/services/categorias.service';
import { GestionService } from '../../core/services/gestion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { Paginacion } from '../../shared/paginacion';
import { aplicarErroresDelServidor, mensajeDeError } from '../../shared/formularios';
import { PanelLateral } from './panel-lateral';

const POR_PAGINA = 10;
// "Precio anterior" convierte la opción en oferta: se muestra tachado y con su % de descuento.
const ATRIBUTOS_COMUNES = ['Color', 'Medida', 'Espesor', 'Precio anterior'];

interface Atributo {
  clave: string;
  valor: string;
}

/** Alta, edición y baja de productos y de sus opciones (color, medida, precio y stock). Solo ADMIN. */
@Component({
  selector: 'app-catalogo-gestion',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink, PanelLateral, Paginacion],
  templateUrl: './catalogo-gestion.html',
  styleUrl: './catalogo-gestion.scss',
})
export class CatalogoGestion {
  private readonly servicio = inject(GestionService);
  private readonly avisos = inject(NotificacionService);
  private readonly categoriasPublicas = inject(CategoriasService);
  private readonly fb = inject(FormBuilder).nonNullable;

  /** Id del producto a abrir al entrar (viene de "Editar este producto" en la tienda). */
  readonly editar = input<string>();

  /** Producto desde el que se llegó ("Editar este producto" en la tienda); permite volver a él. */
  private readonly origenId = signal<string | null>(null);
  protected readonly origen = computed(() => {
    const id = this.origenId();
    return id ? ((this.productos() ?? []).find((p) => p.id === id) ?? null) : null;
  });
  private ultimoEditar: string | undefined;

  protected readonly atributosComunes = ATRIBUTOS_COMUNES;
  protected readonly mensaje = mensajeDeError;

  protected readonly productos = signal<Producto[] | null>(null);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly marcas = signal<Marca[]>([]);
  protected readonly error = signal<string | null>(null);

  // Filtros y paginación
  protected readonly texto = signal('');
  protected readonly categoriaFiltro = signal('');
  protected readonly estadoFiltro = signal<'todos' | 'publicados' | 'borradores' | 'sin-stock'>('todos');
  protected readonly pagina = signal(0);

  protected readonly filtrados = computed(() => {
    const t = this.texto().trim().toLowerCase();
    const c = this.categoriaFiltro();
    const e = this.estadoFiltro();
    return (this.productos() ?? []).filter((p) => {
      if (t && !`${p.nombre} ${p.marca ?? ''} ${p.variantes.map((v) => v.sku).join(' ')}`.toLowerCase().includes(t)) return false;
      if (c && String(p.categoriaId) !== c) return false;
      if (e === 'publicados' && !p.publicado) return false;
      if (e === 'borradores' && p.publicado) return false;
      if (e === 'sin-stock' && this.stockTotal(p) > 0) return false;
      return true;
    });
  });
  protected readonly totalPaginas = computed(() => Math.max(1, Math.ceil(this.filtrados().length / POR_PAGINA)));
  protected readonly visibles = computed(() => this.filtrados().slice(this.pagina() * POR_PAGINA, (this.pagina() + 1) * POR_PAGINA));
  protected readonly resumen = computed(() => {
    const l = this.productos() ?? [];
    return { total: l.length, publicados: l.filter((p) => p.publicado).length, sinStock: l.filter((p) => this.stockTotal(p) <= 0).length };
  });

  // Editor del producto
  protected readonly abierto = signal(false);
  protected readonly editando = signal<Producto | null>(null);
  protected readonly guardando = signal(false);
  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: [''],
    categoriaId: [0, [Validators.required, Validators.min(1)]],
    marcaId: [0],
    imagenUrl: ['', [Validators.maxLength(500)]],
    publicado: [true],
  });
  protected readonly urlImagen = toSignal(this.form.controls.imagenUrl.valueChanges, { initialValue: '' });
  protected readonly imagenRota = signal(false);

  // Editor de opciones (variantes)
  protected readonly varianteAbierta = signal<{ id: string | null } | null>(null);
  protected readonly atributos = signal<Atributo[]>([]);
  protected readonly guardandoVariante = signal(false);
  protected readonly formVar = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(60)]],
    precioVenta: [0, [Validators.required, Validators.min(0)]],
    stock: [0, [Validators.required, Validators.min(0)]],
    imagenUrl: ['', [Validators.maxLength(500)]],
  });

  // Confirmaciones de borrado
  protected readonly porEliminar = signal<string | null>(null);
  protected readonly porEliminarVariante = signal<string | null>(null);
  protected readonly eliminando = signal(false);

  protected readonly nombreCategoria = (id: number | null) => this.categorias().find((c) => c.id === id)?.nombre ?? '—';

  constructor() {
    this.cargar();
    // Abre el editor una sola vez por cada producto que llega en ?editar=. Si no, al cerrar el
    // panel el efecto se volvía a ejecutar (abierto cambia) y lo reabría: no se podía salir.
    effect(() => {
      const id = this.editar();
      const lista = this.productos();
      if (!id) {
        this.ultimoEditar = undefined;
        untracked(() => this.origenId.set(null));
        return;
      }
      if (!lista || id === this.ultimoEditar) return;
      const p = lista.find((x) => x.id === id);
      if (!p) return;
      this.ultimoEditar = id;
      untracked(() => {
        this.origenId.set(id);
        this.abrirEditor(p);
      });
    });
    effect(() => {
      this.filtrados();
      this.pagina.update((p) => Math.min(p, this.totalPaginas() - 1));
    });
  }

  protected cargar(): void {
    this.error.set(null);
    this.productos.set(null);
    forkJoin({ p: this.servicio.productosAdmin(), c: this.servicio.categorias(), m: this.servicio.marcas() }).subscribe({
      next: ({ p, c, m }) => {
        this.productos.set([...p].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.categorias.set(c);
        this.marcas.set(m);
      },
      error: (e: ApiError) => this.error.set(e.mensaje),
    });
  }

  protected stockTotal(p: Producto): number {
    return p.variantes.filter((v) => v.activa).reduce((s, v) => s + v.stock, 0);
  }
  protected opciones(p: Producto): Variante[] {
    return p.variantes.filter((v) => v.activa);
  }
  protected precioDesde(p: Producto): number | null {
    const precios = this.opciones(p).map((v) => v.precioVenta);
    return precios.length ? Math.min(...precios) : null;
  }
  protected foto(p: Producto): string | null {
    return p.imagenUrl || this.opciones(p).find((v) => v.imagenUrl)?.imagenUrl || null;
  }

  // ── Producto
  protected abrirEditor(p: Producto | null): void {
    this.editando.set(p);
    this.imagenRota.set(false);
    this.varianteAbierta.set(null);
    this.porEliminarVariante.set(null);
    this.form.reset({
      nombre: p?.nombre ?? '',
      descripcion: p?.descripcion ?? '',
      categoriaId: p?.categoriaId ?? 0,
      marcaId: p?.marcaId ?? 0,
      imagenUrl: p?.imagenUrl ?? '',
      publicado: p?.publicado ?? true,
    });
    this.abierto.set(true);
  }

  protected cerrarEditor(): void {
    this.abierto.set(false);
    this.editando.set(null);
    this.varianteAbierta.set(null);
  }

  private pedido(p: Producto, publicado = p.publicado): ProductoRequest | null {
    if (p.categoriaId === null) return null;
    return { nombre: p.nombre, descripcion: p.descripcion, marcaId: p.marcaId, imagenUrl: p.imagenUrl, categoriaId: p.categoriaId, publicado };
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const cuerpo: ProductoRequest = {
      nombre: v.nombre.trim(),
      descripcion: v.descripcion.trim() || null,
      marcaId: v.marcaId || null,
      imagenUrl: v.imagenUrl.trim() || null,
      categoriaId: v.categoriaId,
      publicado: v.publicado,
    };
    const actual = this.editando();
    this.guardando.set(true);
    const peticion = actual ? this.servicio.actualizarProducto(actual.id, cuerpo) : this.servicio.crearProducto(cuerpo);
    peticion.subscribe({
      next: (p) => {
        this.guardando.set(false);
        this.reemplazar(p);
        this.editando.set(p);
        this.avisos.ok(actual ? 'Producto actualizado.' : 'Producto creado. Ahora agrega sus opciones: color, medida, precio y stock.');
      },
      error: (e: ApiError) => {
        this.guardando.set(false);
        if (!aplicarErroresDelServidor(e, { nombre: this.form.controls.nombre, categoriaId: this.form.controls.categoriaId, imagenUrl: this.form.controls.imagenUrl })) this.avisos.mostrar(e.mensaje);
      },
    });
  }

  /** Publica u oculta desde la lista, sin abrir el editor. */
  protected alternarPublicado(p: Producto): void {
    const cuerpo = this.pedido(p, !p.publicado);
    if (!cuerpo) {
      this.avisos.mostrar('Este producto no tiene categoría. Ábrelo para completarla.');
      this.abrirEditor(p);
      return;
    }
    this.servicio.actualizarProducto(p.id, cuerpo).subscribe({
      next: (n) => {
        this.reemplazar(n);
        this.avisos.ok(n.publicado ? `«${n.nombre}» ya se ve en la tienda.` : `«${n.nombre}» quedó oculto de la tienda.`);
      },
      error: (e: ApiError) => this.avisos.mostrar(e.mensaje),
    });
  }

  protected eliminar(p: Producto): void {
    this.eliminando.set(true);
    this.servicio.eliminarProducto(p.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.productos.update((l) => (l ?? []).filter((x) => x.id !== p.id));
        if (this.editando()?.id === p.id) this.cerrarEditor();
        this.avisos.ok(`«${p.nombre}» se eliminó del catálogo.`);
      },
      error: (e: ApiError) => {
        this.eliminando.set(false);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }

  private reemplazar(p: Producto): void {
    this.productos.update((l) => {
      const lista = l ?? [];
      return lista.some((x) => x.id === p.id) ? lista.map((x) => (x.id === p.id ? p : x)) : [p, ...lista];
    });
  }

  // ── Opciones (variantes)
  protected abrirVariante(v: Variante | null): void {
    this.varianteAbierta.set({ id: v?.id ?? null });
    this.formVar.reset({ sku: v?.sku ?? '', precioVenta: v?.precioVenta ?? 0, stock: v?.stock ?? 0, imagenUrl: v?.imagenUrl ?? '' });
    const atributos = v ? Object.entries(v.atributos).map(([clave, valor]) => ({ clave, valor })) : [{ clave: 'Color', valor: '' }, { clave: 'Medida', valor: '' }];
    this.atributos.set(atributos);
  }

  protected ponerAtributo(i: number, campo: keyof Atributo, valor: string): void {
    this.atributos.update((l) => l.map((a, k) => (k === i ? { ...a, [campo]: valor } : a)));
  }
  protected agregarAtributo(clave = ''): void {
    this.atributos.update((l) => [...l, { clave, valor: '' }]);
  }
  protected quitarAtributo(i: number): void {
    this.atributos.update((l) => l.filter((_, k) => k !== i));
  }

  protected guardarVariante(): void {
    const producto = this.editando();
    const abierta = this.varianteAbierta();
    if (!producto || !abierta) return;
    if (this.formVar.invalid) {
      this.formVar.markAllAsTouched();
      return;
    }
    const v = this.formVar.getRawValue();
    const atributos: Record<string, string> = {};
    for (const a of this.atributos()) if (a.clave.trim() && a.valor.trim()) atributos[a.clave.trim()] = a.valor.trim();
    const cuerpo: VarianteRequest = { productoBaseId: producto.id, sku: v.sku.trim(), precioVenta: Number(v.precioVenta), stock: Number(v.stock), imagenUrl: v.imagenUrl.trim() || null, atributos };
    this.guardandoVariante.set(true);
    const peticion = abierta.id ? this.servicio.actualizarVariante(abierta.id, cuerpo) : this.servicio.crearVariante(cuerpo);
    peticion.subscribe({
      next: (n) => {
        this.guardandoVariante.set(false);
        const variantes = abierta.id ? producto.variantes.map((x) => (x.id === n.id ? n : x)) : [...producto.variantes, n];
        const nuevo = { ...producto, variantes };
        this.reemplazar(nuevo);
        this.editando.set(nuevo);
        this.varianteAbierta.set(null);
        this.avisos.ok(abierta.id ? 'Opción actualizada.' : 'Opción agregada.');
      },
      error: (e: ApiError) => {
        this.guardandoVariante.set(false);
        if (!aplicarErroresDelServidor(e, { sku: this.formVar.controls.sku, precioVenta: this.formVar.controls.precioVenta, stock: this.formVar.controls.stock })) this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected eliminarVariante(v: Variante): void {
    const producto = this.editando();
    if (!producto) return;
    this.eliminando.set(true);
    this.servicio.eliminarVariante(v.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminarVariante.set(null);
        const nuevo = { ...producto, variantes: producto.variantes.filter((x) => x.id !== v.id) };
        this.reemplazar(nuevo);
        this.editando.set(nuevo);
        this.avisos.ok('Opción eliminada.');
      },
      error: (e: ApiError) => {
        this.eliminando.set(false);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected atributosTexto(v: Variante): string {
    return Object.entries(v.atributos).map(([k, val]) => `${k}: ${val}`).join(' · ');
  }
}
