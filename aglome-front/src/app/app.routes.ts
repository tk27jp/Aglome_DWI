import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { authGuard, invitadoGuard, rolGuard } from './core/guards';
import { AuthService } from './core/services/auth.service';

// Rutas con carga diferida: cada pantalla se descarga solo cuando se visita.
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Aglome Perú · Madera, melamina y ferretería',
    loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
  },
  {
    path: 'productos',
    title: 'Catálogo · Aglome',
    loadComponent: () => import('./features/catalogo/catalogo').then((m) => m.Catalogo),
  },
  {
    path: 'productos/:id',
    title: 'Producto · Aglome',
    loadComponent: () => import('./features/catalogo/producto-detalle').then((m) => m.ProductoDetalle),
  },
  {
    path: 'ingresar',
    title: 'Ingresar · Aglome',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: 'registro',
    title: 'Crear cuenta · Aglome',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/auth/registro').then((m) => m.Registro),
  },
  {
    path: 'cuenta',
    title: 'Mi cuenta · Aglome',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auth/cuenta').then((m) => m.Cuenta),
  },
  {
    path: 'carrito',
    title: 'Tu carrito · Aglome',
    canActivate: [authGuard],
    loadComponent: () => import('./features/pedidos/carrito').then((m) => m.Carrito),
  },
  {
    path: 'pedido-confirmado/:id',
    title: 'Pedido confirmado · Aglome',
    canActivate: [authGuard],
    loadComponent: () => import('./features/pedidos/pedido-confirmado').then((m) => m.PedidoConfirmado),
  },
  {
    path: 'mis-pedidos',
    title: 'Mis pedidos · Aglome',
    canActivate: [authGuard],
    loadComponent: () => import('./features/pedidos/mis-pedidos').then((m) => m.MisPedidos),
  },
  {
    path: 'gestion',
    title: 'Panel de gestión · Aglome',
    canActivate: [rolGuard('ADMIN', 'VENDEDOR')],
    loadComponent: () => import('./features/gestion/gestion').then((m) => m.Gestion),
    children: [
      // El administrador empieza en el resumen; el vendedor, en los pedidos.
      { path: '', pathMatch: 'full', redirectTo: () => (inject(AuthService).rol() === 'ADMIN' ? 'resumen' : 'pedidos') },
      {
        path: 'resumen',
        title: 'Resumen · Gestión Aglome',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./features/gestion/resumen').then((m) => m.Resumen),
      },
      {
        path: 'pedidos',
        title: 'Pedidos · Gestión Aglome',
        loadComponent: () => import('./features/gestion/pedidos-gestion').then((m) => m.PedidosGestion),
      },
      {
        path: 'inventario',
        title: 'Inventario · Gestión Aglome',
        loadComponent: () => import('./features/gestion/inventario').then((m) => m.Inventario),
      },
      // Edición de la tienda: solo el administrador
      {
        path: 'catalogo',
        title: 'Productos · Gestión Aglome',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./features/gestion/catalogo-gestion').then((m) => m.CatalogoGestion),
      },
      {
        path: 'categorias',
        title: 'Categorías · Gestión Aglome',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./features/gestion/categorias-gestion').then((m) => m.CategoriasGestion),
      },
      {
        path: 'marcas',
        title: 'Marcas · Gestión Aglome',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./features/gestion/marcas-gestion').then((m) => m.MarcasGestion),
      },
      {
        path: 'equipo',
        title: 'Equipo · Gestión Aglome',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./features/gestion/equipo-gestion').then((m) => m.EquipoGestion),
      },
    ],
  },
  {
    path: '**',
    title: 'No encontrada · Aglome',
    loadComponent: () => import('./features/no-encontrada').then((m) => m.NoEncontrada),
  },
];
