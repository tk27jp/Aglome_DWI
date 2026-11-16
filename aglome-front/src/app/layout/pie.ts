import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { negocio } from '../../environments/negocio';
import { AuthService } from '../core/services/auth.service';
import { CategoriasService } from '../core/services/categorias.service';
import { Logo } from '../shared/logo';

@Component({
  selector: 'app-pie',
  imports: [RouterLink, Logo],
  templateUrl: './pie.html',
  styleUrl: './pie.scss',
})
export class Pie {
  protected readonly negocio = negocio;
  protected readonly categorias = inject(CategoriasService).arbol;
  protected readonly auth = inject(AuthService);
  protected readonly anio = new Date().getFullYear();
}
