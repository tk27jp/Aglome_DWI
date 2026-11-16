# Aglome · Front (Angular)

Frontend de la tienda de **Aglome Perú**. Consume la API REST de Spring Boot (`aglome-tienda`) y se sirve desde la misma URL del back.

## Estado (avance 3, Unidad 3)

- **Nosotros y sedes** con datos reales de Aglome (3 sedes con foto, horarios y mapa).
- **Ficha de producto** con muestras de color, calculadora de planchas por m², "Comprar ahora", consulta por WhatsApp, pestañas, productos relacionados, "Combina con tu compra" y recomendados, ampliación de foto y animación al agregar al carrito.
- **Portada** con muestras de color de melamina que cambian el tablero de la cabecera, categorías, productos destacados en carrusel y pie de página.
- **Catálogo público** con filtros (texto, categoría, marca, color, medida, precio), orden, paginación y chips de filtros activos. Detalle con opciones, precio, stock y relacionados.
- **Registro e ingreso** con validación en el formulario y mensajes del back. Sesión guardada en el navegador; si el token vence o el back responde 401, se cierra la sesión y se vuelve a la pantalla de ingreso.
- **Carrito** en panel lateral (se abre al agregar) y en página propia: cambiar cantidad, quitar, total.
- **Pedidos**: confirmar compra, pantalla de confirmación, "Mis pedidos" con estado y cancelación cuando el back lo permite.
- **Mi cuenta**: cambio de contraseña.
- Rutas protegidas con guards por sesión y rol; el token se envía solo en las llamadas a `/api`.
- Estilos con **SASS** (marca en `src/styles/_variables.scss`), adaptable a móvil. Las animaciones respetan `prefers-reduced-motion`.

- **Panel de gestión** en `/gestion` (solo ADMIN y VENDEDOR; el rol se valida en las rutas y en el back):
  - *Resumen* (solo ADMIN): ingresos, pedidos, ticket promedio, ventas por mes, pedidos por estado, más vendidos y stock bajo, con enlace "Reponer".
  - *Pedidos* (ADMIN y VENDEDOR): lista con filtro por estado, detalle y botones con las transiciones que permite el back; cancelar pide confirmación.
  - *Inventario* (ADMIN y VENDEDOR): ajuste de stock (entra o sale, con motivo) y historial de movimientos paginado.
  - Al ingresar, el personal entra directo a su panel.

## Datos del negocio

`src/environments/negocio.ts` guarda los datos de la empresa: correo, sedes (dirección, horarios y foto en `public/sedes/`), misión y visión, y el número de WhatsApp (`51913974157`). Si el WhatsApp queda vacío, el botón no se muestra. Cada sede tiene un botón "Cómo llegar" que abre Google Maps con su dirección.

## Datos de demostración

Con el back corriendo y un usuario ADMIN:

```bash
npm run cargar-demo
```

Crea 7 categorías, 4 marcas, 16 productos y sus variantes (se puede repetir sin duplicar). Variables opcionales: `API` (por defecto `http://localhost:8080`), `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

## Estructura

```
src/
├── environments/environment.ts   URL de la API (/api)
├── styles/                        _variables.scss (colores, medidas) y _base.scss (botones, formularios)
└── app/
    ├── core/                      models, services (productos, sesión, carrito, pedidos), interceptors (errores y token), guards
    ├── layout/                    cabecera y pie
    ├── shared/                    logo, paginación, utilidades
    └── features/                  inicio, catalogo, auth, carrito, pedidos, cuenta
```

## Cómo ejecutarlo

Requisitos: Node 22 y Angular CLI. La carpeta `aglome-front` debe estar **al lado** de `aglome-tienda`.

### Opción A · Servido por el back (lo que se presenta)

1. Compilar y copiar el front dentro del back:
   ```bash
   npm install
   npm run build:back
   ```
2. Arrancar el back (`mvnw spring-boot:run` dentro de `aglome-tienda`).
3. Abrir **http://localhost:8080/**. La API sigue en `http://localhost:8080/api/...`.

Cada vez que cambies el front, repite el paso 1 y reinicia el back.

### Opción B · Desarrollo con recarga automática

Con el back corriendo en el puerto 8080:

```bash
npm install
ng serve
```

Abrir **http://localhost:4200/**. El archivo `proxy.conf.json` reenvía `/api` al back, así que no hace falta configurar CORS.

## Pruebas y compilación

```bash
ng build        # compilación de producción en dist/
```
