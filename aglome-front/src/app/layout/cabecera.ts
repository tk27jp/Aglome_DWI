import { CurrencyPipe } from '@angular/common';
import { Component, ElementRef, HostListener, effect, inject, signal, untracked } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { negocio } from '../../environments/negocio';
import { AuthService } from '../core/services/auth.service';
import { CarritoService } from '../core/services/carrito.service';
import { CategoriasService } from '../core/services/categorias.service';
import { VistaService } from '../core/services/vista.service';
import { Logo } from '../shared/logo';

@Component({
  selector: 'app-cabecera',
  imports: [RouterLink, RouterLinkActive, CurrencyPipe, Logo],
  templateUrl: './cabecera.html',
  styleUrl: './cabecera.scss',
})
export class Cabecera {
  private readonly router = inject(Router);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  protected readonly auth = inject(AuthService);
  protected readonly carrito = inject(CarritoService);
  protected readonly categorias = inject(CategoriasService).arbol;
  protected readonly negocio = negocio;

  private readonly vista = inject(VistaService);
  /** En ingreso y registro la cabecera es simple: solo logo y volver. */
  protected readonly sencilla = this.vista.sencilla;
  /** En el panel de gestión solo quedan el logo y el menú de la persona: nada de buscador, carrito ni categorías. */
  protected readonly panel = this.vista.panel;

  /** Cuando se baja la página, la cabecera se compacta y toma sombra. */
  protected readonly bajo = signal(false);
  protected readonly menuUsuario = signal(false);
  protected readonly menuMovil = signal(false);
  /** Se activa un instante cada vez que se agrega algo, para animar el contador del carrito. */
  protected readonly rebote = signal(false);

  constructor() {
    effect(() => {
      if (this.carrito.agregados() === 0) return;
      untracked(() => {
        this.rebote.set(false);
        setTimeout(() => this.rebote.set(true));
        setTimeout(() => this.rebote.set(false), 700);
      });
    });
  }

  @HostListener('window:scroll')
  protected alDesplazar(): void {
    this.bajo.set(window.scrollY > 8);
  }

  buscar(texto: string): void {
    const limpio = texto.trim();
    this.menuMovil.set(false);
    this.router.navigate(['/productos'], { queryParams: limpio ? { texto: limpio } : {} });
  }

  salir(): void {
    this.menuUsuario.set(false);
    this.auth.cerrarSesion();
    this.router.navigate(['/']);
  }

  @HostListener('document:click', ['$event'])
  cerrarSiClicFuera(e: Event): void {
    if (!this.elemento.nativeElement.contains(e.target as Node)) this.menuUsuario.set(false);
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.menuUsuario.set(false);
    this.menuMovil.set(false);
  }
}
