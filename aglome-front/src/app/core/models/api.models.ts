// Tipos que reflejan los DTO del back-end (paquete dto de Spring Boot).

export interface Variante {
  id: string;
  sku: string;
  precioVenta: number;
  stock: number;
  activa: boolean;
  atributos: Record<string, string>;
  imagenUrl: string | null;
}

export interface Producto {
  id: string;
  nombre: string;
  descripcion: string | null;
  marca: string | null;
  marcaId: number | null;
  imagenUrl: string | null;
  categoriaId: number | null;
  categoriaNombre: string | null;
  publicado: boolean;
  variantes: Variante[];
  fechaCreacion: string;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}

export interface Categoria {
  id: number;
  nombre: string;
  padreId: number | null;
  activa: boolean;
  imagenUrl: string | null;
}

export interface Marca {
  id: number;
  nombre: string;
  imagenUrl: string | null;
}

export interface OpcionesFiltro {
  categorias: Categoria[];
  marcas: Marca[];
  colores: string[];
  medidas: string[];
  precioMinimo: number | null;
  precioMaximo: number | null;
}

export interface FiltroProducto {
  texto?: string;
  categoriaId?: number;
  marcaId?: number;
  color?: string;
  medida?: string;
  precioMin?: number;
  precioMax?: number;
  disponible?: boolean;
  pagina?: number;
  tamano?: number;
  orden?: 'nombre' | 'marca';
  direccion?: 'asc' | 'desc';
}

/** Error uniforme del back (ApiError). */
export interface ApiError {
  timestamp?: string;
  status: number;
  error?: string;
  mensaje: string;
  path?: string;
  errores?: { campo: string; mensaje: string }[];
}

// ── Sesión, carrito y pedidos ───────────────────────────────────────
export type Rol = 'ADMIN' | 'VENDEDOR' | 'CLIENTE';

export interface AuthResponse {
  token: string;
  nombre: string;
  email: string;
  rol: Rol;
}

export interface Perfil {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
}

export interface CarritoItem {
  id: string;
  varianteId: string;
  productoNombre: string;
  sku: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
}

export interface Carrito {
  id: string;
  items: CarritoItem[];
  total: number;
}

export interface DetallePedido {
  productoNombre: string;
  sku: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export type EstadoPedido = 'PENDIENTE' | 'CONFIRMADO' | 'EN_PREPARACION' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO';

export interface Pedido {
  id: string;
  fecha: string;
  estado: EstadoPedido;
  total: number;
  detalles: DetallePedido[];
  siguientesEstados: EstadoPedido[];
}

export interface CategoriaArbol {
  id: number;
  nombre: string;
  imagenUrl: string | null;
  hijas: CategoriaArbol[];
}

// ── Panel de gestión (ADMIN y VENDEDOR) ─────────────────────────────
export interface VentaMes {
  mes: string;
  pedidos: number;
  total: number;
}

export interface ProductoMasVendido {
  productoId: string;
  nombre: string;
  unidades: number;
  ingresos: number;
}

export interface StockBajo {
  varianteId: string;
  sku: string;
  producto: string;
  stock: number;
}

export interface ResumenDashboard {
  ingresosTotales: number;
  pedidosValidos: number;
  ticketPromedio: number;
  pedidosPorEstado: Record<string, number>;
  variantesConStockBajo: number;
  ventasPorMes: VentaMes[];
  productosMasVendidos: ProductoMasVendido[];
  stockBajo: StockBajo[];
}

export type TipoMovimiento = 'STOCK_INICIAL' | 'VENTA' | 'CANCELACION' | 'AJUSTE';

export interface MovimientoStock {
  id: string;
  varianteId: string;
  sku: string;
  producto: string;
  tipo: TipoMovimiento;
  cantidad: number;
  stockAnterior: number;
  stockResultante: number;
  motivo: string | null;
  pedidoId: string | null;
  usuario: string | null;
  fecha: string;
}

export interface AjusteStock {
  varianteId: string;
  /** Con signo: positivo entra mercadería, negativo sale. */
  cantidad: number;
  motivo: string;
}

// ── Edición de la tienda (solo ADMIN) ───────────────────────────────
export interface ProductoRequest {
  nombre: string;
  descripcion: string | null;
  marcaId: number | null;
  imagenUrl: string | null;
  categoriaId: number;
  publicado: boolean;
}

export interface VarianteRequest {
  productoBaseId: string;
  sku: string;
  precioVenta: number;
  stock: number;
  imagenUrl: string | null;
  atributos: Record<string, string>;
}

export interface CategoriaRequest {
  nombre: string;
  padreId: number | null;
  imagenUrl: string | null;
}

export interface MarcaRequest {
  nombre: string;
  imagenUrl: string | null;
}

export interface CrearUsuarioRequest {
  nombre: string;
  email: string;
  password: string;
  rol: Rol;
}

export interface VentaCategoria {
  categoria: string;
  unidades: number;
}
