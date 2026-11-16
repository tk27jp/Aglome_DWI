import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/models/api.models';
import { CarritoService } from '../../core/services/carrito.service';
import { NotificacionService } from '../../core/services/notificacion.service';

@Component({
  selector: 'app-carrito',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './carrito.html',
  styleUrl: './carrito.scss',
})
export class Carrito {
  protected readonly carrito = inject(CarritoService);
  private readonly avisos = inject(NotificacionService);
  private readonly router = inject(Router);

  protected readonly confirmando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected cambiar(itemId: string, cantidad: number): void {
    this.error.set(null);
    if (cantidad < 1) return this.quitar(itemId);
    this.carrito.cambiarCantidad(itemId, cantidad).subscribe({ error: (e: ApiError) => this.error.set(e.mensaje) });
  }

  protected quitar(itemId: string): void {
    this.carrito.quitar(itemId).subscribe({ error: (e: ApiError) => this.error.set(e.mensaje) });
  }

  protected confirmar(): void {
    this.confirmando.set(true);
    this.error.set(null);
    this.carrito.confirmar().subscribe({
      next: (pedido) => this.router.navigate(['/pedido-confirmado', pedido.id]),
      error: (e: ApiError) => {
        this.confirmando.set(false);
        // por ejemplo "Stock insuficiente": se muestra el motivo y el carrito se actualiza
        this.error.set(e.mensaje);
        this.avisos.mostrar(e.mensaje);
        this.carrito.cargar().subscribe({ error: () => undefined });
      },
    });
  }
}
