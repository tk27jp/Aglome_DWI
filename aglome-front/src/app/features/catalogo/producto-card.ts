import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Producto } from '../../core/models/api.models';
import { ProductoImagen } from '../../shared/producto-imagen';
import { precioDesde, sinStock, variantesActivas } from '../../shared/producto-utils';

@Component({
  selector: 'app-producto-card',
  imports: [RouterLink, CurrencyPipe, ProductoImagen],
  templateUrl: './producto-card.html',
  styleUrl: './producto-card.scss',
})
export class ProductoCard {
  readonly producto = input.required<Producto>();

  protected readonly desde = computed(() => precioDesde(this.producto()));
  protected readonly agotado = computed(() => sinStock(this.producto()));
  protected readonly cantidadVariantes = computed(() => variantesActivas(this.producto()).length);
}
