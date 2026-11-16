import { Component, effect, input, signal, untracked } from '@angular/core';

/** Muestra un número que "sube" hasta su valor cuando cambia. Respeta prefers-reduced-motion. */
@Component({
  selector: 'app-contador',
  template: '{{ texto() }}',
})
export class Contador {
  readonly valor = input.required<number>();
  readonly formato = input<'moneda' | 'entero' | 'porcentaje'>('entero');

  private readonly actual = signal(0);
  private cuadro = 0;

  protected readonly texto = () => {
    const n = this.actual();
    switch (this.formato()) {
      case 'moneda': return 'S/ ' + n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      case 'porcentaje': return n.toLocaleString('es-PE', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + ' %';
      default: return Math.round(n).toLocaleString('es-PE');
    }
  };

  constructor() {
    effect((alLimpiar) => {
      const destino = this.valor();
      const inicio = untracked(() => this.actual());
      const reducido = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (reducido || inicio === destino) {
        untracked(() => this.actual.set(destino));
        return;
      }
      const t0 = performance.now();
      const duracion = 700;
      const paso = (t: number) => {
        const k = Math.min(1, (t - t0) / duracion);
        const suave = 1 - Math.pow(1 - k, 3);
        this.actual.set(inicio + (destino - inicio) * suave);
        if (k < 1) this.cuadro = requestAnimationFrame(paso);
      };
      this.cuadro = requestAnimationFrame(paso);
      alLimpiar(() => cancelAnimationFrame(this.cuadro));
    });
  }
}
