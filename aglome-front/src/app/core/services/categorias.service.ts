import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { CategoriaArbol } from '../models/api.models';

/** Categorías con sus subcategorías. Se piden una sola vez y las usan la cabecera, el pie y la portada. */
@Injectable({ providedIn: 'root' })
export class CategoriasService {
  readonly arbol = signal<CategoriaArbol[]>([]);

  private readonly http = inject(HttpClient);

  constructor() {
    this.cargar();
  }

  /** Vuelve a pedir el árbol (después de crear, editar o borrar categorías en el panel). */
  cargar(): void {
    this.http.get<CategoriaArbol[]>(`${environment.apiUrl}/categorias/arbol`).subscribe({ next: (a) => this.arbol.set(a), error: () => undefined });
  }
}
