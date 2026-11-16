import { CurrencyPipe } from '@angular/common';
import { Component, ElementRef, HostListener, effect, inject, untracked, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ApiError } from '../core/models/api.models';
import { AuthService } from '../core/services/auth.service';
import { CarritoService } from '../core/services/carrito.service';
import { NotificacionService } from '../core/services/notificacion.service';

/** Panel lateral del carrito: se abre al agregar un producto y permite ajustar cantidades sin salir de la página. */
@Component({
  selector: 'app-panel-carrito',
  imports: [CurrencyPipe],
  templateUrl: './panel-carrito.html',
  styleUrl: './panel-carrito.scss',
})
export class PanelCarrito {
  protected readonly carrito = inject(CarritoService);
  protected readonly auth = inject(AuthService);
  private readonly avisos = inject(NotificacionService);
  private readonly router = inject(Router);
  private readonly cerrar = viewChild<ElementRef<HTMLButtonElement>>('cerrar');

  constructor() {
    // al abrir, el foco pasa al panel (accesibilidad con teclado)
    effect(() => {
      if (this.carrito.panelAbierto()) untracked(() => setTimeout(() => this.cerrar()?.nativeElement.focus()));
    });
  }

  @HostListener('document:keydown.escape')
  protected cerrarConEscape(): void {
    this.carrito.panelAbierto.set(false);
  }

  protected cerrarPanel(): void {
    this.carrito.panelAbierto.set(false);
  }

  protected irA(ruta: string): void {
    this.cerrarPanel();
    this.router.navigate([ruta]);
  }

  protected cambiar(itemId: string, cantidad: number): void {
    if (cantidad < 1) return this.quitar(itemId);
    this.carrito.cambiarCantidad(itemId, cantidad).subscribe({ error: (e: ApiError) => this.avisos.mostrar(e.mensaje) });
  }

  protected quitar(itemId: string): void {
    this.carrito.quitar(itemId).subscribe({ error: (e: ApiError) => this.avisos.mostrar(e.mensaje) });
  }
}
