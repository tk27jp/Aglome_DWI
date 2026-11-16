import { Component, computed, input, output } from '@angular/core';

/** Paginador reutilizable. Recibe la página actual (desde 0) y el total que entrega el back (PaginaDTO). */
@Component({
  selector: 'app-paginacion',
  templateUrl: './paginacion.html',
  styleUrl: './paginacion.scss',
})
export class Paginacion {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly cambio = output<number>();

  /** Hasta 5 números alrededor de la página actual. */
  protected readonly numeros = computed(() => {
    const total = this.totalPaginas();
    const actual = this.pagina();
    const inicio = Math.max(0, Math.min(actual - 2, total - 5));
    const fin = Math.min(total, inicio + 5);
    return Array.from({ length: fin - inicio }, (_, i) => inicio + i);
  });
}
