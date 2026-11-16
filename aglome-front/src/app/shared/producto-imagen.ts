import { Component, computed, input, signal } from '@angular/core';
import { Producto } from '../core/models/api.models';
import { colorDe, colorDelProducto, tipoDeVista } from './colores';


@Component({
  selector: 'app-producto-imagen',
  templateUrl: './producto-imagen.html',
  styleUrl: './producto-imagen.scss',
})
export class ProductoImagen {
  readonly producto = input.required<Producto>();

  readonly foto = input<string | null>(null);

  readonly tono = input<string | null>(null);

  /** Última URL que no se pudo cargar; mientras no cambie, se dibuja la vista previa en su lugar. */
  protected readonly fallo = signal<string | null>(null);
  protected readonly src = computed(() => {
    const url = this.foto() || this.producto().imagenUrl;
    return url && url !== this.fallo() ? url : null;
  });
  protected readonly color = computed(() => (this.tono() ? colorDe(this.tono()) : colorDelProducto(this.producto())));
  protected readonly tipo = computed(() => tipoDeVista(this.producto()));
}
