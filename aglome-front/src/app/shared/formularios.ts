import { AbstractControl, ValidationErrors } from '@angular/forms';
import { ApiError } from '../core/models/api.models';

/** Texto de error de un campo, o null si no tiene error o aún no se tocó. */
export function mensajeDeError(control: AbstractControl | null): string | null {
  if (!control || !control.invalid || !(control.touched || control.dirty)) return null;
  const e = control.errors ?? {};
  if (e['servidor']) return String(e['servidor']);
  if (e['required']) return 'Este campo es obligatorio.';
  if (e['email']) return 'Escribe un correo válido, por ejemplo nombre@correo.com.';
  if (e['minlength']) return `Debe tener al menos ${e['minlength'].requiredLength} caracteres.`;
  if (e['maxlength']) return `No puede pasar de ${e['maxlength'].requiredLength} caracteres.`;
  if (e['letrasYNumeros']) return 'Debe incluir letras y números.';
  if (e['distintaDeLaActual']) return 'La contraseña nueva debe ser distinta de la actual.';
  if (e['noCoincide']) return 'Las contraseñas no coinciden.';
  return 'Revisa este campo.';
}

export function letrasYNumeros(c: AbstractControl): ValidationErrors | null {
  const v = String(c.value ?? '');
  return !v || (/[A-Za-z]/.test(v) && /\d/.test(v)) ? null : { letrasYNumeros: true };
}

/** El control `campo` debe ser igual al control `otro` (para repetir contraseña). */
export function igualA(otro: string) {
  return (c: AbstractControl): ValidationErrors | null => {
    const valorOtro = c.parent?.get(otro)?.value;
    return c.value === valorOtro ? null : { noCoincide: true };
  };
}

/** Pasa los errores por campo que devuelve el back (ApiError.errores) a los controles del formulario. */
export function aplicarErroresDelServidor(error: ApiError, controles: Record<string, AbstractControl>): boolean {
  let alguno = false;
  for (const e of error.errores ?? []) {
    const control = controles[e.campo];
    if (control) {
      control.setErrors({ servidor: e.mensaje });
      control.markAsTouched();
      alguno = true;
    }
  }
  return alguno;
}
