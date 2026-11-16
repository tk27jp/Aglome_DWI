import { Component, computed, input, signal } from '@angular/core';
import { Producto } from '../core/models/api.models';
import { colorDelProducto, tipoDeVista } from './colores';

/**
 * Imagen del producto. Si el producto tiene foto (imagenUrl) la muestra; si no, dibuja una vista previa
 * con el color real de la variante (un tablero con su veta, o el ícono del tipo de producto).
 */
@Component({
  selector: 'app-producto-imagen',
  templateUrl: './producto-imagen.html',
  styleUrl: './producto-imagen.scss',
})
export class ProductoImagen {
  readonly producto = input.required<Producto>();
  /** Foto de una variante elegida, si existe; tiene prioridad sobre la del producto. */
  readonly foto = input<string | null>(null);

  /** Última URL que no se pudo cargar; mientras no cambie, se dibuja la vista previa en su lugar. */
  protected readonly fallo = signal<string | null>(null);
  protected readonly src = computed(() => {
    const url = this.foto() || this.producto().imagenUrl;
    return url && url !== this.fallo() ? url : null;
  });
  protected readonly color = computed(() => colorDelProducto(this.producto()));
  protected readonly tipo = computed(() => tipoDeVista(this.producto()));
}
