import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models/api.models';
import { NotificacionService } from '../services/notificacion.service';

/**
 * Convierte cualquier fallo HTTP en un ApiError (el formato uniforme del back) y lo relanza,
 * así cada componente trabaja siempre con el mismo tipo de error.
 * Los fallos de conexión y los 5xx además avisan al usuario con una notificación.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificaciones = inject(NotificacionService);

  return next(req).pipe(
    catchError((respuesta: HttpErrorResponse) => {
      const error = aApiError(respuesta);
      if (error.status === 0 || error.status >= 500) {
        notificaciones.mostrar(error.mensaje, "error");
      }
      return throwError(() => error);
    }),
  );
};

function aApiError(r: HttpErrorResponse): ApiError {
  if (r.status === 0) {
    return { status: 0, mensaje: 'No se pudo conectar con el servidor. Revisa que la API esté encendida.' };
  }
  const cuerpo = r.error as Partial<ApiError> | null;
  if (cuerpo && typeof cuerpo === 'object' && typeof cuerpo.mensaje === 'string') {
    return { ...cuerpo, status: cuerpo.status ?? r.status, mensaje: cuerpo.mensaje };
  }
  return { status: r.status, mensaje: r.status >= 500 ? 'Error inesperado del servidor.' : 'No se pudo completar la solicitud.' };
}
