import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError, Pedido } from '../../core/models/api.models';
import { PedidoService } from '../../core/services/pedido.service';
import { codigoCorto } from '../../shared/estados';

/** Pantalla que ve el cliente justo después de confirmar su compra. */
@Component({
  selector: 'app-pedido-confirmado',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './pedido-confirmado.html',
  styleUrl: './pedido-confirmado.scss',
})
export class PedidoConfirmado {
  private readonly pedidos = inject(PedidoService);
  readonly id = input.required<string>();

  protected readonly pedido = signal<Pedido | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly codigo = codigoCorto;

  constructor() {
    effect(() => {
      this.pedidos.obtener(this.id()).subscribe({
        next: (p) => this.pedido.set(p),
        error: (e: ApiError) => this.error.set(e.mensaje),
      });
    });
  }
}
