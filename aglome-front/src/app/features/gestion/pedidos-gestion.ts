import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ApiError, EstadoPedido, Pedido } from '../../core/models/api.models';
import { GestionService } from '../../core/services/gestion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { ETIQUETA_ESTADO, codigoCorto } from '../../shared/estados';

const ACCION: Record<EstadoPedido, string> = {
  PENDIENTE: 'Dejar pendiente',
  CONFIRMADO: 'Confirmar',
  EN_PREPARACION: 'Pasar a preparación',
  ENVIADO: 'Marcar como enviado',
  ENTREGADO: 'Marcar como entregado',
  CANCELADO: 'Cancelar pedido',
};

@Component({
  selector: 'app-pedidos-gestion',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './pedidos-gestion.html',
  styleUrl: './pedidos-gestion.scss',
})
export class PedidosGestion {
  private readonly servicio = inject(GestionService);
  private readonly avisos = inject(NotificacionService);

  protected readonly filtros: { valor: EstadoPedido | ''; texto: string }[] = [
    { valor: '', texto: 'Todos' },
    ...(['CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO', 'CANCELADO'] as EstadoPedido[]).map((e) => ({ valor: e, texto: ETIQUETA_ESTADO[e] })),
  ];
  protected readonly etiqueta = ETIQUETA_ESTADO;
  protected readonly codigo = codigoCorto;

  protected readonly filtro = signal<EstadoPedido | ''>('');
  protected readonly pedidos = signal<Pedido[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly abierto = signal<string | null>(null);
  protected readonly cambiando = signal<string | null>(null);
  /** Pedido cuya cancelación espera una segunda confirmación. */
  protected readonly porCancelar = signal<string | null>(null);

  constructor() {
    this.cargar();
  }

  protected elegirFiltro(valor: EstadoPedido | ''): void {
    this.filtro.set(valor);
    this.cargar();
  }

  protected cargar(): void {
    this.pedidos.set(null);
    this.error.set(null);
    this.servicio.pedidos(this.filtro() || undefined).subscribe({
      next: (lista) => this.pedidos.set(lista),
      error: (e: ApiError) => this.error.set(e.mensaje),
    });
  }

  protected alternar(id: string): void {
    this.abierto.update((a) => (a === id ? null : id));
    this.porCancelar.set(null);
  }

  protected textoAccion(estado: EstadoPedido): string {
    return ACCION[estado] ?? estado;
  }

  protected mover(p: Pedido, estado: EstadoPedido): void {
    if (estado === 'CANCELADO' && this.porCancelar() !== p.id) {
      this.porCancelar.set(p.id);
      return;
    }
    this.porCancelar.set(null);
    this.cambiando.set(p.id);
    this.servicio.cambiarEstado(p.id, estado).subscribe({
      next: (nuevo) => {
        this.cambiando.set(null);
        // Con un filtro activo, el pedido sale de la lista al cambiar de estado.
        this.pedidos.update((l) => (l ?? []).flatMap((x) => (x.id !== nuevo.id ? [x] : this.filtro() && this.filtro() !== nuevo.estado ? [] : [nuevo])));
        this.avisos.ok(`Pedido ${codigoCorto(nuevo.id)}: ${ETIQUETA_ESTADO[nuevo.estado]}.`);
      },
      error: (e: ApiError) => {
        this.cambiando.set(null);
        this.avisos.mostrar(e.mensaje);
      },
    });
  }

  protected unidades(p: Pedido): number {
    return p.detalles.reduce((s, d) => s + d.cantidad, 0);
  }
}
