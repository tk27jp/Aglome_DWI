import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Rol } from '../../core/models/api.models';

interface Seccion {
  ruta: string;
  texto: string;
  /** Trazo del ícono (24×24). */
  icono: string;
  roles: Rol[];
}

const TODOS: Rol[] = ['ADMIN', 'VENDEDOR'];

const GRUPOS: { titulo: string; secciones: Seccion[] }[] = [
  {
    titulo: 'Operación',
    secciones: [
      { ruta: 'resumen', texto: 'Resumen', icono: 'M4 20V10M10 20V4M16 20v-7M22 20H2', roles: ['ADMIN'] },
      { ruta: 'pedidos', texto: 'Pedidos', icono: 'M6 3h12l2 4v13H4V7l2-4zM4 7h16M9 11h6', roles: TODOS },
      { ruta: 'inventario', texto: 'Inventario', icono: 'M3 7l9-4 9 4-9 4-9-4zM3 7v10l9 4 9-4V7M12 11v10', roles: TODOS },
    ],
  },
  {
    titulo: 'Tienda',
    secciones: [
      { ruta: 'catalogo', texto: 'Productos', icono: 'M20 12l-8 8-9-9V3h8l9 9zM7.5 7.5h.01', roles: ['ADMIN'] },
      { ruta: 'categorias', texto: 'Categorías', icono: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z', roles: ['ADMIN'] },
      { ruta: 'marcas', texto: 'Marcas', icono: 'M12 3l2.7 5.5 6 .9-4.4 4.2 1 6-5.3-2.8-5.3 2.8 1-6L3.3 9.4l6-.9L12 3z', roles: ['ADMIN'] },
    ],
  },
  {
    titulo: 'Administración',
    secciones: [{ ruta: 'equipo', texto: 'Equipo', icono: 'M16 11a4 4 0 1 0-8 0 4 4 0 0 0 8 0zM4 21c0-4 3.6-6 8-6s8 2 8 6', roles: ['ADMIN'] }],
  },
];

/** Marco del panel: barra lateral con las secciones que corresponden al rol y la pantalla elegida. */
@Component({
  selector: 'app-gestion',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './gestion.html',
  styleUrl: './gestion.scss',
})
export class Gestion {
  protected readonly auth = inject(AuthService);
  protected readonly grupos = computed(() => {
    const rol = this.auth.rol();
    return GRUPOS.map((g) => ({ titulo: g.titulo, secciones: g.secciones.filter((s) => rol && s.roles.includes(rol)) })).filter((g) => g.secciones.length);
  });
  protected readonly inicial = computed(() => (this.auth.nombre().trim()[0] ?? '?').toUpperCase());
}
