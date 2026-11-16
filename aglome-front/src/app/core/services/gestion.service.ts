import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AjusteStock,
  Categoria,
  CategoriaRequest,
  CrearUsuarioRequest,
  EstadoPedido,
  Marca,
  MarcaRequest,
  MovimientoStock,
  Pagina,
  Pedido,
  Perfil,
  Producto,
  ProductoRequest,
  ResumenDashboard,
  TipoMovimiento,
  Variante,
  VarianteRequest,
  VentaCategoria,
} from '../models/api.models';

/** Endpoints del panel: /api/admin/pedidos y /api/admin/stock (ADMIN y VENDEDOR) y /api/admin/dashboard (solo ADMIN). */
@Injectable({ providedIn: 'root' })
export class GestionService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/admin`;
  private readonly publica = environment.apiUrl;

  dashboard(): Observable<ResumenDashboard> {
    return this.http.get<ResumenDashboard>(`${this.api}/dashboard`);
  }

  pedidos(estado?: EstadoPedido): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.api}/pedidos`, { params: estado ? { estado } : {} });
  }

  cambiarEstado(id: string, estado: EstadoPedido): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.api}/pedidos/${id}/estado`, null, { params: { estado } });
  }

  movimientos(opciones: { tipo?: TipoMovimiento | ''; pagina: number; tamano: number }): Observable<Pagina<MovimientoStock>> {
    let params = new HttpParams().set('pagina', opciones.pagina).set('tamano', opciones.tamano);
    if (opciones.tipo) params = params.set('tipo', opciones.tipo);
    return this.http.get<Pagina<MovimientoStock>>(`${this.api}/stock/movimientos`, { params });
  }

  ajustarStock(ajuste: AjusteStock): Observable<MovimientoStock> {
    return this.http.post<MovimientoStock>(`${this.api}/stock/ajustes`, ajuste);
  }

  ventasPorCategoria(): Observable<VentaCategoria[]> {
    return this.http.get<VentaCategoria[]>(`${this.api}/reportes/ventas-por-categoria`);
  }

  // ── Productos y opciones (variantes): solo ADMIN
  productosAdmin(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.api}/productos`);
  }
  crearProducto(p: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(`${this.api}/productos`, p);
  }
  actualizarProducto(id: string, p: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.api}/productos/${id}`, p);
  }
  eliminarProducto(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/productos/${id}`);
  }
  crearVariante(v: VarianteRequest): Observable<Variante> {
    return this.http.post<Variante>(`${this.api}/variantes`, v);
  }
  actualizarVariante(id: string, v: VarianteRequest): Observable<Variante> {
    return this.http.put<Variante>(`${this.api}/variantes/${id}`, v);
  }
  eliminarVariante(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/variantes/${id}`);
  }

  // ── Categorías y marcas
  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.publica}/categorias`);
  }
  crearCategoria(c: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.api}/categorias`, c);
  }
  actualizarCategoria(id: number, c: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.api}/categorias/${id}`, c);
  }
  eliminarCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/categorias/${id}`);
  }
  marcas(): Observable<Marca[]> {
    return this.http.get<Marca[]>(`${this.publica}/marcas`);
  }
  crearMarca(m: MarcaRequest): Observable<Marca> {
    return this.http.post<Marca>(`${this.api}/marcas`, m);
  }
  actualizarMarca(id: number, m: MarcaRequest): Observable<Marca> {
    return this.http.put<Marca>(`${this.api}/marcas/${id}`, m);
  }
  eliminarMarca(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/marcas/${id}`);
  }

  // ── Equipo
  usuarios(): Observable<Perfil[]> {
    return this.http.get<Perfil[]>(`${this.api}/usuarios`);
  }
  crearUsuario(u: CrearUsuarioRequest): Observable<Perfil> {
    return this.http.post<Perfil>(`${this.api}/usuarios`, u);
  }
}
