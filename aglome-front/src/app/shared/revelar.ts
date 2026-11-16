import { Directive, ElementRef, OnDestroy, OnInit, inject } from '@angular/core';

/**
 * Hace que un elemento aparezca suavemente cuando entra en pantalla al hacer scroll.
 * Con "reducir movimiento" activado en el sistema, se muestra de inmediato.
 * El orden de entrada se escalona con la variable CSS --i.
 */
@Directive({ selector: '[revelar]', host: { class: 'revelar' } })
export class Revelar implements OnInit, OnDestroy {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private observador?: IntersectionObserver;

  ngOnInit(): void {
    const e = this.el.nativeElement;
    const sinMovimiento = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (typeof IntersectionObserver === 'undefined' || sinMovimiento) {
      e.classList.add('visto');
      return;
    }
    this.observador = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          e.classList.add('visto');
          this.observador?.disconnect();
        }
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    );
    this.observador.observe(e);
  }

  ngOnDestroy(): void {
    this.observador?.disconnect();
  }
}
