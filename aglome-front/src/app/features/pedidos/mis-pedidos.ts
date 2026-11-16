import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError, EstadoPedido, Pedido } from '../../core/models/api.models';
import { NotificacionService } from '../../core/services/notificacion.service';
import { PedidoService } from '../../core/services/pedido.service';
import { ETIQUETA_ESTADO, PASOS_PEDIDO, codigoCorto, pasoActual, sePuedeCancelar } from '../../shared/estados';

@Component({
  selector: 'app-mis-pedidos',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './mis-pedidos.html',
  styleUrl: './mis-pedidos.scss',
})
export class MisPedidos {
  private readonly servicio = inject(PedidoService);
  private readonly avisos = inject(NotificacionService);

  protected readonly pedidos = signal<Pedido[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal<EstadoPedido | 'TODOS'>('TODOS');
  protected readonly confirmandoCancelar = signal<string | null>(null);
  protected readonly cancelando = signal<string | null>(null);

  protected readonly etiqueta = ETIQUETA_ESTADO;
  protected readonly pasos = PASOS_PEDIDO;
  protected readonly paso = pasoActual;
  protected readonly cancelable = sePuedeCancelar;
  protected readonly codigo = codigoCorto;
  protected readonly filtros: (EstadoPedido | 'TODOS')[] = ['TODOS', 'CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO', 'CANCELADO'];

  /** Más recientes primero, y solo los del estado elegido. */
  protected readonly visibles = computed(() => {
    const f = this.filtro();
    return [...(this.pedidos() ?? [])]
      .filter((p) => f === 'TODOS' || p.estado === f)
      .sort((a, b) => b.fecha.localeCompare(a.fecha));
  });

  constructor() {
    this.cargar();
  }

  protected etiquetaFiltro(f: EstadoPedido | 'TODOS'): string {
    return f === 'TODOS' ? 'Todos' : ETIQUETA_ESTADO[f];
  }

  protected cancelar(id: string): void {
    this.cancelando.set(id);
    this.servicio.cancelar(id).subscribe({
      next: (actualizado) => {
        this.pedidos.update((lista) => (lista ?? []).map((p) => (p.id === id ? actualizado : p)));
        this.cancelando.set(null);
        this.confirmandoCancelar.set(null);
        this.avisos.ok('Pedido cancelado.');
      },
      error: (e: ApiError) => {
        this.cancelando.set(null);
        this.confirmandoCancelar.set(null);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected cargar(): void {
    this.error.set(null);
    this.servicio.misPedidos().subscribe({
      next: (p) => this.pedidos.set(p),
      error: (e: ApiError) => this.error.set(e.mensaje),
    });
  }
}
