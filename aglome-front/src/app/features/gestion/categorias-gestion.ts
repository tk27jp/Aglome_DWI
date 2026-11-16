import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Categoria } from '../../core/models/api.models';
import { CategoriasService } from '../../core/services/categorias.service';
import { GestionService } from '../../core/services/gestion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { aplicarErroresDelServidor, mensajeDeError } from '../../shared/formularios';
import { PanelLateral } from './panel-lateral';

interface Fila {
  cat: Categoria;
  nivel: number;
  hijas: number;
}

/** Categorías y subcategorías de la tienda (las que ven los clientes en el menú azul). Solo ADMIN. */
@Component({
  selector: 'app-categorias-gestion',
  imports: [ReactiveFormsModule, PanelLateral],
  templateUrl: './categorias-gestion.html',
  styleUrl: './categorias-gestion.scss',
})
export class CategoriasGestion {
  private readonly servicio = inject(GestionService);
  private readonly publicas = inject(CategoriasService);
  private readonly avisos = inject(NotificacionService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly mensaje = mensajeDeError;
  protected readonly categorias = signal<Categoria[] | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly filas = computed<Fila[]>(() => {
    const todas = this.categorias() ?? [];
    const filas: Fila[] = [];
    const recorrer = (padre: number | null, nivel: number): void => {
      for (const cat of todas.filter((c) => (c.padreId ?? null) === padre).sort((a, b) => a.nombre.localeCompare(b.nombre))) {
        filas.push({ cat, nivel, hijas: todas.filter((c) => c.padreId === cat.id).length });
        if (nivel < 3) recorrer(cat.id, nivel + 1);
      }
    };
    recorrer(null, 0);
    return filas;
  });
  protected readonly raices = computed(() => (this.categorias() ?? []).filter((c) => !c.padreId));

  protected readonly abierto = signal(false);
  protected readonly editando = signal<Categoria | null>(null);
  protected readonly guardando = signal(false);
  protected readonly porEliminar = signal<number | null>(null);
  protected readonly eliminando = signal(false);
  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    padreId: [0],
    imagenUrl: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.error.set(null);
    this.servicio.categorias().subscribe({ next: (c) => this.categorias.set(c), error: (e: ApiError) => this.error.set(e.mensaje) });
  }

  /** Para el selector de "pertenece a": todas menos ella misma y sus descendientes. */
  protected opcionesPadre(): Categoria[] {
    const yo = this.editando();
    const todas = this.categorias() ?? [];
    if (!yo) return this.raices();
    const excluidas = new Set<number>([yo.id]);
    let crecio = true;
    while (crecio) {
      crecio = false;
      for (const c of todas) if (c.padreId && excluidas.has(c.padreId) && !excluidas.has(c.id)) { excluidas.add(c.id); crecio = true; }
    }
    return this.raices().filter((c) => !excluidas.has(c.id));
  }

  protected abrir(cat: Categoria | null, padreId = 0): void {
    this.editando.set(cat);
    this.form.reset({ nombre: cat?.nombre ?? '', padreId: cat?.padreId ?? padreId, imagenUrl: cat?.imagenUrl ?? '' });
    this.abierto.set(true);
  }

  protected cerrar(): void {
    this.abierto.set(false);
    this.editando.set(null);
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const cuerpo = { nombre: v.nombre.trim(), padreId: v.padreId || null, imagenUrl: v.imagenUrl.trim() || null };
    const actual = this.editando();
    this.guardando.set(true);
    (actual ? this.servicio.actualizarCategoria(actual.id, cuerpo) : this.servicio.crearCategoria(cuerpo)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.avisos.ok(actual ? 'Categoría actualizada.' : 'Categoría creada.');
        this.cerrar();
        this.cargar();
        this.publicas.cargar();
      },
      error: (e: ApiError) => {
        this.guardando.set(false);
        if (!aplicarErroresDelServidor(e, { nombre: this.form.controls.nombre, imagenUrl: this.form.controls.imagenUrl })) this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected eliminar(cat: Categoria): void {
    this.eliminando.set(true);
    this.servicio.eliminarCategoria(cat.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.avisos.ok(`Categoría «${cat.nombre}» eliminada.`);
        this.cargar();
        this.publicas.cargar();
      },
      error: (e: ApiError) => {
        this.eliminando.set(false);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }
}
