import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Producto, Variante } from '../../core/models/api.models';
import { ProductoImagen } from '../../shared/producto-imagen';
import { descuento, mejorDescuento, precioAnterior } from '../../shared/oferta';
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
  protected readonly oferta = computed(() => mejorDescuento(this.producto()));

  protected readonly anterior = computed(() => {
    const barata = variantesActivas(this.producto()).reduce<Variante | null>((m, v) => (!m || v.precioVenta < m.precioVenta ? v : m), null);
    return descuento(barata) ? precioAnterior(barata) : null;
  });
  protected readonly agotado = computed(() => sinStock(this.producto()));
  protected readonly cantidadVariantes = computed(() => variantesActivas(this.producto()).length);
}
