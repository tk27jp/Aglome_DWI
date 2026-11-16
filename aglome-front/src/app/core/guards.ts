import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from './models/api.models';
import { AuthService } from './services/auth.service';
import { NotificacionService } from './services/notificacion.service';

/** Solo usuarios con sesión; si no, va a ingresar y luego regresa a donde quería ir. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  if (auth.token()) return true;
  return inject(Router).createUrlTree(['/ingresar'], { queryParams: { retorno: estado.url } });
};

/** Solo ciertos roles (por ejemplo ADMIN y VENDEDOR). */
export const rolGuard =
  (...roles: Rol[]): CanActivateFn =>
  (_ruta, estado) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.token()) return router.createUrlTree(['/ingresar'], { queryParams: { retorno: estado.url } });
    if (auth.tieneRol(...roles)) return true;
    inject(NotificacionService).mostrar('No tienes permiso para entrar a esa sección.', 'info');
    return router.createUrlTree(['/']);
  };

/** Login y registro no tienen sentido con sesión iniciada. */
export const invitadoGuard: CanActivateFn = () => {
  return inject(AuthService).token() ? inject(Router).createUrlTree(['/']) : true;
};
