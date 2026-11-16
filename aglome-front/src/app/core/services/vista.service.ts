import { Injectable, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

/** Dice en qué parte de la aplicación está el usuario, para mostrar u ocultar cabecera, pie y botones flotantes. */
@Injectable({ providedIn: 'root' })
export class VistaService {
  private readonly router = inject(Router);
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /** Panel de gestión: no es la tienda, así que no lleva buscador, carrito, categorías ni pie. */
  readonly panel = computed(() => /^\/gestion(\?|#|$|\/)/.test(this.url()));
  /** Ingreso y registro: cabecera simple con solo el logo. */
  readonly sencilla = computed(() => /^\/(ingresar|registro)(\?|#|$|\/)/.test(this.url()));
}
