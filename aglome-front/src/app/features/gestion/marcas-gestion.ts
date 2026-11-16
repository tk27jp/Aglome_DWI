import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Marca } from '../../core/models/api.models';
import { GestionService } from '../../core/services/gestion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { aplicarErroresDelServidor, mensajeDeError } from '../../shared/formularios';
import { PanelLateral } from './panel-lateral';

/** Marcas que se muestran en los productos y en el filtro del catálogo. Solo ADMIN. */
@Component({
  selector: 'app-marcas-gestion',
  imports: [ReactiveFormsModule, PanelLateral],
  templateUrl: './marcas-gestion.html',
  styleUrl: './marcas-gestion.scss',
})
export class MarcasGestion {
  private readonly servicio = inject(GestionService);
  private readonly avisos = inject(NotificacionService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly mensaje = mensajeDeError;
  protected readonly marcas = signal<Marca[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly abierto = signal(false);
  protected readonly editando = signal<Marca | null>(null);
  protected readonly guardando = signal(false);
  protected readonly porEliminar = signal<number | null>(null);
  protected readonly eliminando = signal(false);
  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    imagenUrl: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.error.set(null);
    this.servicio.marcas().subscribe({ next: (m) => this.marcas.set([...m].sort((a, b) => a.nombre.localeCompare(b.nombre))), error: (e: ApiError) => this.error.set(e.mensaje) });
  }

  protected abrir(m: Marca | null): void {
    this.editando.set(m);
    this.form.reset({ nombre: m?.nombre ?? '', imagenUrl: m?.imagenUrl ?? '' });
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
    const cuerpo = { nombre: v.nombre.trim(), imagenUrl: v.imagenUrl.trim() || null };
    const actual = this.editando();
    this.guardando.set(true);
    (actual ? this.servicio.actualizarMarca(actual.id, cuerpo) : this.servicio.crearMarca(cuerpo)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.avisos.ok(actual ? 'Marca actualizada.' : 'Marca creada.');
        this.cerrar();
        this.cargar();
      },
      error: (e: ApiError) => {
        this.guardando.set(false);
        if (!aplicarErroresDelServidor(e, { nombre: this.form.controls.nombre, imagenUrl: this.form.controls.imagenUrl })) this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected eliminar(m: Marca): void {
    this.eliminando.set(true);
    this.servicio.eliminarMarca(m.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.avisos.ok(`Marca «${m.nombre}» eliminada.`);
        this.cargar();
      },
      error: (e: ApiError) => {
        this.eliminando.set(false);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }
}
