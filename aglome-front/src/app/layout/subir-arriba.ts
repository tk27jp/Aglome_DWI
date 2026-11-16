import { Component, HostListener, signal } from '@angular/core';

/** Botón para volver arriba; aparece cuando la persona ya bajó bastante. */
@Component({
  selector: 'app-subir-arriba',
  template: `
    <button type="button" class="subir" [class.ver]="ver()" aria-label="Volver arriba" [attr.tabindex]="ver() ? 0 : -1" (click)="subir()">
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 14 6-6 6 6" /></svg>
    </button>
  `,
  styles: `
    .subir {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      z-index: 39;
      width: 3rem;
      height: 3rem;
      display: grid;
      place-items: center;
      background: #0a1f44;
      color: #fff;
      border: 0;
      border-radius: 3px;
      box-shadow: 0 6px 18px rgb(0 0 0 / 28%);
      cursor: pointer;
      opacity: 0;
      transform: translateY(16px);
      pointer-events: none;
      transition: opacity 0.25s, transform 0.25s, background 0.15s;
      &.ver { opacity: 1; transform: none; pointer-events: auto; }
      &:hover { background: #0049af; }
      &:hover svg { transform: translateY(-2px); }
      svg { transition: transform 0.15s; }
    }
  `,
})
export class SubirArriba {
  protected readonly ver = signal(false);

  @HostListener('window:scroll')
  protected alDesplazar(): void {
    this.ver.set(window.scrollY > 700);
  }

  protected subir(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
