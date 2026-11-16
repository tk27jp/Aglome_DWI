import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Carrito, Pedido } from '../models/api.models';
import { AuthService } from './auth.service';

/** Estado del carrito de compras. Se vuelve a cargar solo cuando el usuario inicia o cierra sesión. */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly api = `${environment.apiUrl}`;

  readonly carrito = signal<Carrito | null>(null);
  readonly cantidad = computed(() => this.carrito()?.items.reduce((suma, i) => suma + i.cantidad, 0) ?? 0);
  readonly total = computed(() => this.carrito()?.total ?? 0);
  readonly vacio = computed(() => this.cantidad() === 0);

  /** El panel lateral del carrito (se abre al agregar un producto). */
  readonly panelAbierto = signal(false);
  /** Sube cada vez que se agrega algo: la cabecera lo usa para animar el contador. */
  readonly agregados = signal(0);

  constructor() {
    effect(() => {
      const hayUsuario = this.auth.autenticado();
      untracked(() => {
        if (hayUsuario) this.cargar().subscribe({ error: () => undefined });
        else this.carrito.set(null);
      });
    });
  }

  cargar(): Observable<Carrito> {
    return this.http.get<Carrito>(`${this.api}/carrito`).pipe(tap((c) => this.carrito.set(c)));
  }

  agregar(varianteId: string, cantidad = 1): Observable<Carrito> {
    return this.http.post<Carrito>(`${this.api}/carrito/items`, { varianteId, cantidad }).pipe(
      tap((c) => {
        this.carrito.set(c);
        this.agregados.update((n) => n + 1);
      }),
    );
  }

  cambiarCantidad(itemId: string, cantidad: number): Observable<Carrito> {
    return this.http.put<Carrito>(`${this.api}/carrito/items/${itemId}`, null, { params: { cantidad } }).pipe(tap((c) => this.carrito.set(c)));
  }

  quitar(itemId: string): Observable<Carrito> {
    return this.http.delete<Carrito>(`${this.api}/carrito/items/${itemId}`).pipe(tap((c) => this.carrito.set(c)));
  }

  /** Convierte el carrito en un pedido y lo vacía. */
  confirmar(): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.api}/pedidos/confirmar`, null).pipe(tap(() => this.cargar().subscribe({ error: () => undefined })));
  }
}
