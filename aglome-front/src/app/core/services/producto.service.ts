import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FiltroProducto, OpcionesFiltro, Pagina, Producto } from '../models/api.models';

/** Consume los endpoints públicos del catálogo (/api/productos/**). */
@Injectable({ providedIn: 'root' })
export class ProductoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/productos`;

  /** Catálogo paginado con filtros combinables. Solo se envían los filtros que tienen valor. */
  filtrar(filtro: FiltroProducto): Observable<Pagina<Producto>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtro)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<Producto>>(`${this.base}/filtrar`, { params });
  }

  opcionesDeFiltro(): Observable<OpcionesFiltro> {
    return this.http.get<OpcionesFiltro>(`${this.base}/filtros`);
  }

  sugerencias(texto: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/sugerencias`, { params: { texto } });
  }

  obtener(id: string): Observable<Producto> {
    return this.http.get<Producto>(`${this.base}/${id}`);
  }

  relacionados(id: string): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.base}/${id}/relacionados`);
  }
}
