import { Component, HostListener, input, output } from '@angular/core';

/** Panel que se desliza desde la derecha para crear o editar sin salir de la lista. */
@Component({
  selector: 'app-panel-lateral',
  template: `
    @if (abierto()) {
      <div class="velo" (click)="cerrar.emit()"></div>
      <aside class="hoja" role="dialog" aria-modal="true" [attr.aria-label]="titulo()" [style.--ancho]="ancho()">
        <header>
          <h2>{{ titulo() }}</h2>
          <button type="button" class="x" aria-label="Cerrar" (click)="cerrar.emit()">×</button>
        </header>
        <div class="cuerpo"><ng-content /></div>
      </aside>
    }
  `,
  styles: `
    :host { display: contents; }
    .velo { position: fixed; inset: 0; z-index: 60; background: rgb(10 31 68 / 45%); animation: aparece 0.2s ease; }
    .hoja { position: fixed; top: 0; right: 0; bottom: 0; z-index: 61; width: min(var(--ancho, 34rem), 100vw); display: flex; flex-direction: column; background: #fff; box-shadow: -12px 0 40px rgb(10 31 68 / 25%); animation: entra 0.28s cubic-bezier(0.2, 0.8, 0.2, 1); }
    header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1rem 1.25rem; border-bottom: 1px solid #dcdfe6; }
    h2 { margin: 0; font-size: 1.7rem; }
    .x { width: 2.2rem; height: 2.2rem; border: 0; border-radius: 3px; background: none; font-size: 1.6rem; line-height: 1; cursor: pointer; &:hover { background: #f3f4f7; } }
    .cuerpo { flex: 1; overflow-y: auto; padding: 1.25rem; }
    @keyframes aparece { from { opacity: 0; } }
    @keyframes entra { from { transform: translateX(100%); } }
  `,
})
export class PanelLateral {
  readonly abierto = input.required<boolean>();
  readonly titulo = input.required<string>();
  readonly ancho = input('34rem');
  readonly cerrar = output<void>();

  @HostListener('document:keydown.escape')
  protected alEscape(): void {
    if (this.abierto()) this.cerrar.emit();
  }
}
