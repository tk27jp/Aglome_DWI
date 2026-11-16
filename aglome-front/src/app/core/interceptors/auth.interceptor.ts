import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../services/notificacion.service';

/**
 * Agrega el token a las llamadas a la API (menos /auth). Si el servidor responde 401 estando
 * con sesión, el token ya no sirve: se cierra la sesión y se envía al usuario a ingresar de nuevo.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const avisos = inject(NotificacionService);

  const esApi = req.url.startsWith(environment.apiUrl);
  const esAuth = req.url.startsWith(`${environment.apiUrl}/auth/`);
  const token = esApi && !esAuth ? auth.token() : null;
  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error: { status?: number }) => {
      if (token && error?.status === 401) {
        auth.cerrarSesion();
        avisos.mostrar('Tu sesión venció. Ingresa de nuevo para continuar.', 'info');
        router.navigate(['/ingresar'], { queryParams: { retorno: router.url } });
      }
      return throwError(() => error);
    }),
  );
};
