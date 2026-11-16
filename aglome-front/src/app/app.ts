import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Cabecera } from './layout/cabecera';
import { PanelCarrito } from './layout/panel-carrito';
import { Pie } from './layout/pie';
import { SubirArriba } from './layout/subir-arriba';
import { Whatsapp } from './layout/whatsapp';
import { NotificacionService } from './core/services/notificacion.service';
import { VistaService } from './core/services/vista.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Cabecera, Pie, PanelCarrito, Whatsapp, SubirArriba],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly notificaciones = inject(NotificacionService);
  protected readonly vista = inject(VistaService);
}
