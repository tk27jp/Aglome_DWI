import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, filter, switchMap, tap } from 'rxjs';
import { ApiError, FiltroProducto, OpcionesFiltro, Pagina, Producto } from '../../core/models/api.models';
import { ProductoService } from '../../core/services/producto.service';
import { Paginacion } from '../../shared/paginacion';
import { ProductoCard } from './producto-card';


@Component({
  selector: 'app-catalogo',
  imports: [ProductoCard, Paginacion],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.scss',
})
export class Catalogo {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productos = inject(ProductoService);
  private readonly destruir = inject(DestroyRef);

  protected readonly opciones = signal<OpcionesFiltro | null>(null);
  protected readonly resultado = signal<Pagina<Producto> | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal<FiltroProducto>({});
  protected readonly filtrosAbiertos = signal(false);
  protected readonly sugerencias = signal<string[]>([]);

  private readonly escribiendo = new Subject<string>();

  protected readonly categorias = computed(() => {
    const todas = this.opciones()?.categorias ?? [];
    const principales = todas.filter((c) => c.padreId === null);
    return principales.flatMap((p) => [
      { id: p.id, nombre: p.nombre, hija: false },
      ...todas.filter((c) => c.padreId === p.id).map((c) => ({ id: c.id, nombre: c.nombre, hija: true })),
    ]);
  });

  protected readonly colores = computed(() => ordenNatural(this.opciones()?.colores ?? []));
  protected readonly medidas = computed(() => ordenNatural(this.opciones()?.medidas ?? []));

  protected readonly ordenActual = computed(() => `${this.filtro().orden ?? 'nombre'}-${this.filtro().direccion ?? 'asc'}`);

  protected readonly activos = computed(() => {
    const f = this.filtro();
    const o = this.opciones();
    const chips: { clave: keyof FiltroProducto | 'precio'; texto: string }[] = [];
    if (f.texto) chips.push({ clave: 'texto', texto: `“${f.texto}”` });
    if (f.categoriaId) chips.push({ clave: 'categoriaId', texto: o?.categorias.find((c) => c.id === f.categoriaId)?.nombre ?? 'Categoría' });
    if (f.marcaId) chips.push({ clave: 'marcaId', texto: o?.marcas.find((m) => m.id === f.marcaId)?.nombre ?? 'Marca' });
    if (f.color) chips.push({ clave: 'color', texto: `Color: ${f.color}` });
    if (f.medida) chips.push({ clave: 'medida', texto: `Medida: ${f.medida}` });
    if (f.disponible) chips.push({ clave: 'disponible', texto: 'Solo con stock' });
    if (f.precioMin !== undefined || f.precioMax !== undefined) {
      chips.push({ clave: 'precio', texto: `S/ ${f.precioMin ?? 0} – ${f.precioMax !== undefined ? 'S/ ' + f.precioMax : 'más'}` });
    }
    return chips;
  });

  constructor() {
    this.productos.opcionesDeFiltro().pipe(takeUntilDestroyed(this.destruir)).subscribe({
      next: (o) => this.opciones.set(o),
      error: () => this.opciones.set({ categorias: [], marcas: [], colores: [], medidas: [], precioMinimo: null, precioMaximo: null }),
    });

    // Cada cambio de la URL vuelve a consultar el catálogo; switchMap cancela la consulta anterior.
    this.route.queryParamMap
      .pipe(
        tap((params) => {
          this.filtro.set(leerFiltro(params));
          this.cargando.set(true);
          this.error.set(null);
        }),
        switchMap(() =>
          this.productos.filtrar(this.filtro()).pipe(
            catchError((e: ApiError) => {
              this.error.set(e.mensaje);
              this.resultado.set(null);
              this.cargando.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destruir),
      )
      .subscribe((pagina) => {
        this.resultado.set(pagina);
        this.cargando.set(false);
      });

    // Autocompletado del buscador (espera a que el usuario deje de escribir).
    this.escribiendo
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        filter((t) => t.trim().length >= 2),
        switchMap((t) => this.productos.sugerencias(t.trim()).pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(this.destruir),
      )
      .subscribe((s) => this.sugerencias.set(s));
  }

  protected leerFormulario(form: HTMLFormElement): Record<string, string> {
    return Object.fromEntries(new FormData(form)) as Record<string, string>;
  }

  protected escribir(texto: string): void {
    this.escribiendo.next(texto);
  }

 
  protected aplicar(valores: Record<string, string>, cerrar = true): void {
    const num = (s: string | undefined) => (s && s.trim() !== '' && !isNaN(Number(s)) ? Number(s) : undefined);
    this.navegar({
      texto: valores['texto']?.trim() || undefined,
      categoriaId: num(valores['categoriaId']),
      marcaId: num(valores['marcaId']),
      color: valores['color'] || undefined,
      medida: valores['medida'] || undefined,
      precioMin: num(valores['precioMin']),
      precioMax: num(valores['precioMax']),
      disponible: valores['disponible'] === 'on' ? true : undefined,
      pagina: undefined,
    });
    if (cerrar) this.filtrosAbiertos.set(false);
  }

  protected ordenar(valor: string): void {
    const [orden, direccion] = valor.split('-');
    this.navegar({ orden: orden as 'nombre' | 'marca', direccion: direccion as 'asc' | 'desc', pagina: undefined });
  }

  protected irAPagina(n: number): void {
    this.navegar({ pagina: n || undefined });
  }

  protected quitar(clave: keyof FiltroProducto | 'precio'): void {
    this.navegar(
      clave === 'precio'
        ? { precioMin: undefined, precioMax: undefined, pagina: undefined }
        : ({ [clave]: undefined, pagina: undefined } as FiltroProducto),
    );
  }

  protected limpiar(): void {
    this.router.navigate([], { queryParams: {} });
    this.filtrosAbiertos.set(false);
  }

  private navegar(cambios: FiltroProducto): void {
    this.router.navigate([], { queryParams: cambios, queryParamsHandling: 'merge' });
  }
}

function leerFiltro(p: ParamMap): FiltroProducto {
  const numero = (k: string) => {
    const v = p.get(k);
    return v !== null && v !== '' && !isNaN(Number(v)) ? Number(v) : undefined;
  };
  const orden = p.get('orden');
  const direccion = p.get('direccion');
  return {
    texto: p.get('texto') ?? undefined,
    categoriaId: numero('categoriaId'),
    marcaId: numero('marcaId'),
    color: p.get('color') ?? undefined,
    medida: p.get('medida') ?? undefined,
    precioMin: numero('precioMin'),
    precioMax: numero('precioMax'),
    disponible: p.get('disponible') === 'true' ? true : undefined,
    pagina: numero('pagina'),
    orden: orden === 'marca' ? 'marca' : 'nombre',
    direccion: direccion === 'desc' ? 'desc' : 'asc',
  };
}


function ordenNatural(valores: string[]): string[] {
  return [...valores].sort((a, b) => a.localeCompare(b, 'es', { numeric: true, sensitivity: 'base' }));
}
