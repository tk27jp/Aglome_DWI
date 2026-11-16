import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoPedido, Pedido } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/pedidos`;

  misPedidos(estado?: EstadoPedido): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.api, { params: estado ? { estado } : {} });
  }

  obtener(id: string): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.api}/${id}`);
  }

  cancelar(id: string): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.api}/${id}/cancelar`, null);
  }
}
