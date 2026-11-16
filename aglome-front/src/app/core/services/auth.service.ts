import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, Perfil, Rol } from '../models/api.models';

const CLAVE = 'aglome.sesion';

/**
 * Sesión del usuario. El token JWT se guarda en localStorage para que sobreviva a recargar la página,
 * y se descarta solo cuando vence (se lee la fecha de vencimiento del propio token).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}`;

  private readonly _sesion = signal<AuthResponse | null>(leerSesionGuardada());
  readonly sesion = this._sesion.asReadonly();
  readonly autenticado = computed(() => this._sesion() !== null);
  readonly rol = computed<Rol | null>(() => this._sesion()?.rol ?? null);
  readonly nombre = computed(() => this._sesion()?.nombre ?? '');
  readonly primerNombre = computed(() => this.nombre().split(' ')[0]);
  readonly esPersonal = computed(() => this.rol() === 'ADMIN' || this.rol() === 'VENDEDOR');

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, { email, password }).pipe(tap((r) => this.guardar(r)));
  }

  registro(nombre: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/auth/registro`, { nombre, email, password }).pipe(tap((r) => this.guardar(r)));
  }

  perfil(): Observable<Perfil> {
    return this.http.get<Perfil>(`${this.api}/perfil`);
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string): Observable<void> {
    return this.http.put<void>(`${this.api}/perfil/contrasena`, { contrasenaActual, contrasenaNueva });
  }

  /** Devuelve el token vigente; si ya venció, cierra la sesión y devuelve null. */
  token(): string | null {
    const s = this._sesion();
    if (!s) return null;
    if (haVencido(s.token)) {
      this.cerrarSesion();
      return null;
    }
    return s.token;
  }

  tieneRol(...roles: Rol[]): boolean {
    const r = this.rol();
    return r !== null && roles.includes(r);
  }

  cerrarSesion(): void {
    this._sesion.set(null);
    try {
      localStorage.removeItem(CLAVE);
    } catch {
      /* sin almacenamiento disponible: la sesión solo vive en memoria */
    }
  }

  private guardar(r: AuthResponse): void {
    this._sesion.set(r);
    try {
      localStorage.setItem(CLAVE, JSON.stringify(r));
    } catch {
      /* ver cerrarSesion */
    }
  }
}

function leerSesionGuardada(): AuthResponse | null {
  try {
    const crudo = localStorage.getItem(CLAVE);
    if (!crudo) return null;
    const s = JSON.parse(crudo) as AuthResponse;
    if (!s?.token || haVencido(s.token)) {
      localStorage.removeItem(CLAVE);
      return null;
    }
    return s;
  } catch {
    return null;
  }
}

/** Lee el campo exp (segundos) del token JWT. Si no se puede leer, se considera vencido. */
function haVencido(token: string): boolean {
  try {
    const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const { exp } = JSON.parse(atob(carga)) as { exp?: number };
    return typeof exp !== 'number' || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}
