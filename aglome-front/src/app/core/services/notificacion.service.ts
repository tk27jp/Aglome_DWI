import { Injectable, signal } from '@angular/core';

export type TipoAviso = 'error' | 'ok' | 'info';

/** Mensajes breves para el usuario. Los muestra el componente de avisos de la aplicación. */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  readonly aviso = signal<{ texto: string; tipo: TipoAviso } | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  mostrar(texto: string, tipo: TipoAviso = 'error', milisegundos = 5000): void {
    this.aviso.set({ texto, tipo });
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.aviso.set(null), milisegundos);
  }

  ok(texto: string): void {
    this.mostrar(texto, 'ok', 3500);
  }

  cerrar(): void {
    clearTimeout(this.temporizador);
    this.aviso.set(null);
  }
}
