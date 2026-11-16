import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { enlaceMapa, negocio, Sede } from '../../../environments/negocio';
import { Producto } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { CategoriasService } from '../../core/services/categorias.service';
import { ProductoService } from '../../core/services/producto.service';
import { colorDe } from '../../shared/colores';
import { Revelar } from '../../shared/revelar';
import { ProductoCard } from '../catalogo/producto-card';

interface Muestra {
  nombre: string;
  color: string;
}

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, ProductoCard, Revelar],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  private readonly productos = inject(ProductoService);
  protected readonly auth = inject(AuthService);
  protected readonly categorias = inject(CategoriasService).arbol;
  protected readonly negocio = negocio;

  /** Colores de melamina para probar en la portada. Llevan al catálogo filtrado por ese color. */
  protected readonly muestras: Muestra[] = ['Blanco', 'Roble', 'Wengué', 'Gris', 'Natural', 'Negro'].map((nombre) => ({ nombre, color: colorDe(nombre) }));
  protected readonly elegida = signal<Muestra>(this.muestras[1]);

  protected readonly destacados = signal<Producto[] | null>(null);
  protected readonly pista = viewChild<ElementRef<HTMLElement>>('pista');
  protected readonly anios = new Date().getFullYear() - negocio.desde;
  protected mapaDe(sede: Sede): string {
    return enlaceMapa(sede);
  }

  constructor() {
    this.productos.filtrar({ tamano: 10 }).subscribe({
      next: (p) => this.destacados.set(p.contenido),
      error: () => this.destacados.set([]),
    });
  }

  /** Inclinación del tablero de la portada según dónde esté el mouse (de -1 a 1). */
  protected readonly giro = signal({ x: 0, y: 0 });

  protected inclinar(e: PointerEvent | null): void {
    if (!e || e.pointerType === 'touch') return this.giro.set({ x: 0, y: 0 });
    const caja = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.giro.set({ x: ((e.clientX - caja.left) / caja.width) * 2 - 1, y: ((e.clientY - caja.top) / caja.height) * 2 - 1 });
  }

  protected mover(direccion: 1 | -1): void {
    const el = this.pista()?.nativeElement;
    if (el) el.scrollBy({ left: direccion * el.clientWidth * 0.85, behavior: 'smooth' });
  }

  protected subnombres(hijas: { nombre: string }[]): string {
    return hijas.slice(0, 3).map((h) => h.nombre).join(' · ');
  }
}
