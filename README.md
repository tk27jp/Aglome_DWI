# Aglome · Plataforma web de ventas

Plataforma web de **Aglome Perú S.R.L.**, empresa de Chimbote (Perú) dedicada a la venta de **madera, melamina y ferretería**. Es una tienda en línea con catálogo, carrito y pedidos, más un panel de gestión para el personal.

El repositorio contiene las dos partes del sistema:

| Carpeta | Qué es | Tecnología |
|---|---|---|
| [`aglome-tienda/`](aglome-tienda) | **Back-end**: API REST, seguridad, base de datos | Java 21 · Spring Boot 4 · PostgreSQL |
| [`aglome-front/`](aglome-front) | **Front-end**: tienda pública y panel de gestión | Angular 22 · TypeScript · SASS |

> Proyecto académico (UTP) desarrollado por etapas: API REST con TDD, base de datos con JPA/Hibernate, seguridad con JWT y front-end en Angular.

---

## Tabla de contenido

1. [Contexto](#contexto)
2. [Funcionalidades](#funcionalidades)
3. [Estructura del repositorio](#estructura-del-repositorio)
4. [Tecnologías](#tecnologías)
5. [Requisitos](#requisitos)
6. [Puesta en marcha](#puesta-en-marcha)
7. [Back-end en detalle](#back-end-en-detalle)
8. [Front-end en detalle](#front-end-en-detalle)
9. [Documentación y pruebas](#documentación-y-pruebas)
10. [Problemas frecuentes](#problemas-frecuentes)

---

## Contexto

Aglome no tenía catálogo web: el inventario no estaba en internet y la atención era solo por llamadas o visitas. Los clientes debían llamar para consultar disponibilidad, precios, colores y medidas, y el personal se saturaba con consultas repetitivas.

La plataforma resuelve esto con una **vitrina digital** (catálogo con fotos, precios, medidas y filtros), **compra en línea** (carrito y pedidos), **contacto por WhatsApp** y un **panel interno** para gestionar pedidos, inventario y catálogo.

---

## Funcionalidades

### Tienda pública y clientes
- Portada con categorías, productos destacados y muestras de color de melamina.
- Catálogo con filtros (texto, categoría, marca, color, medida, precio), orden y paginación.
- Ficha de producto: variantes, stock, calculadora de planchas por m², productos relacionados y consulta por WhatsApp.
- Registro e ingreso con validación; sesión con JWT.
- Carrito (panel lateral y página propia), confirmación de compra y **Mis pedidos** con cancelación cuando está permitido.
- Sección **Nosotros** con las sedes, horarios y enlace "Cómo llegar".

### Panel de gestión (`/gestion`)
- **Resumen** (ADMIN): ingresos, pedidos por estado, ticket promedio, ventas por mes, más vendidos y stock bajo.
- **Pedidos** (ADMIN y VENDEDOR): filtro por estado y cambios de estado según el flujo permitido.
- **Inventario** (ADMIN y VENDEDOR): ajustes de stock con motivo e historial de movimientos.
- **Productos, categorías, marcas y equipo** (solo ADMIN): altas, ediciones y borrado lógico.

### Reglas del back-end
- Flujo de pedido: `PENDIENTE → CONFIRMADO → EN_PREPARACION → ENVIADO → ENTREGADO` (o `CANCELADO`); al cancelar se devuelve el stock.
- Historial de cada cambio de stock (quién, cuándo, cuánto y por qué).
- Borrado lógico y auditoría de fechas en categorías, productos, variantes y pedidos.
- Bloqueo de cuenta durante 3 minutos tras 5 contraseñas incorrectas (HTTP 423).
- Errores con formato JSON uniforme en toda la API.

### Roles

| Rol | Puede |
|---|---|
| `CLIENTE` | Ver catálogo, usar carrito, hacer y cancelar sus pedidos, ver su perfil |
| `VENDEDOR` | Lo anterior + gestionar pedidos e inventario |
| `ADMIN` | Todo, incluido catálogo, reportes y gestión de usuarios |

---

## Estructura del repositorio

```
Aglome_DWI/
├── README.md                     ← este archivo
├── aglome-tienda/                ← BACK (proyecto Maven)
│   ├── pom.xml · mvnw · mvnw.cmd
│   └── src/
│       ├── main/java/pe/edu/utp/aglome_tienda/
│       │   ├── config/           Swagger, auditoría JPA, admin inicial, SPA
│       │   ├── controller/       16 controladores (públicos, cliente y admin)
│       │   ├── service/          Reglas de negocio y transacciones
│       │   ├── repository/       Spring Data JPA + filtros dinámicos (Specifications)
│       │   ├── model/            Entidades JPA
│       │   ├── dto/              Objetos de entrada y salida
│       │   ├── exception/        Excepciones y GlobalExceptionHandler
│       │   └── security/         SecurityConfig, JWT, filtros
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── static/           Build del front servido por Spring Boot
│       └── test/                 Pruebas automáticas (JUnit 5, Mockito, MockMvc, H2)
└── aglome-front/                 ← FRONT (Angular)
    ├── src/app/
    │   ├── core/                 modelos, servicios, interceptores y guards
    │   ├── layout/               cabecera, pie, carrito lateral, WhatsApp
    │   ├── shared/               componentes y utilidades comunes
    │   └── features/             inicio, catalogo, auth, pedidos, gestion
    ├── src/environments/         URL de la API y datos del negocio (sedes, contacto)
    ├── src/styles/               Estilos SASS globales (marca en _variables.scss)
    ├── public/                   Logo, favicon y fotos de las sedes
    ├── scripts/                  Copia del build al back y carga de datos
    └── proxy.conf.json           Proxy /api → http://localhost:8080 en desarrollo
```

**Cómo se conectan:** el front llama a la API con rutas relativas (`/api/...`). En producción/presentación el build de Angular se copia a `aglome-tienda/src/main/resources/static` y **Spring Boot sirve todo desde un solo puerto (8080)**. En desarrollo, `ng serve` usa un proxy hacia el back.

```
Navegador → Angular (SPA) → /api → Controller → Service → Repository → PostgreSQL
                                        ↓ (si algo falla)
                              GlobalExceptionHandler → JSON uniforme
```

---

## Tecnologías

**Back-end:** Java 21 · Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation) · Hibernate · PostgreSQL · JWT (jjwt 0.12.6) · springdoc-openapi (Swagger UI) · Lombok · Maven Wrapper · JUnit 5, Mockito, MockMvc y H2 (pruebas).

**Front-end:** Angular 22 (componentes standalone, rutas con carga diferida, guards e interceptores) · TypeScript · RxJS · SASS · Vitest · fuentes Barlow Condensed y Public Sans.

---

## Requisitos

- **JDK 21**
- **PostgreSQL** en ejecución (puerto 5432)
- **Node.js 22** y npm (solo para compilar o desarrollar el front)

---

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/tk27jp/Aglome_DWI.git
cd Aglome_DWI
```

### 2. Crear la base de datos

```sql
CREATE DATABASE aglome_spring_db;
```

Las tablas las crea Hibernate al arrancar (`ddl-auto=update`).

### 3. Configurar el back

Edita `aglome-tienda/src/main/resources/application.properties` con el usuario y la contraseña de tu PostgreSQL (`spring.datasource.*`). El resto se configura con variables de entorno, todas con valor por defecto para desarrollo:

| Variable | Descripción | Por defecto |
|---|---|---|
| `JWT_SECRET` | Clave que firma los tokens (mínimo 32 caracteres) | clave de desarrollo |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `36000000` (10 h) |
| `CORS_ORIGENES` | Orígenes permitidos del front (separados por coma) | `http://localhost:4200` |
| `ADMIN_EMAIL` | Email del administrador creado al arrancar | `admin@aglome.com` |
| `ADMIN_PASSWORD` | Contraseña de ese administrador | `Admin123` |

> ⚠️ Los valores por defecto son **solo para desarrollo**. Antes de publicar el proyecto, define tus propias `JWT_SECRET` y `ADMIN_PASSWORD`, y no subas la contraseña real de tu base de datos al repositorio.

### 4. Opción A · Todo en uno (back sirviendo el front)

```bash
# Compilar el front y copiarlo dentro del back
cd aglome-front
npm install
npm run build:back

# Arrancar el back
cd ../aglome-tienda
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Abrir **http://localhost:8080/**. Cada vez que cambies el front, repite `npm run build:back` y reinicia el back.

### 4. Opción B · Desarrollo con recarga automática

Terminal 1 (back):

```bash
cd aglome-tienda
./mvnw spring-boot:run
```

Terminal 2 (front):

```bash
cd aglome-front
npm install
npm start                       # ng serve
```

Abrir **http://localhost:4200/**. El archivo `proxy.conf.json` reenvía `/api` al back (puerto 8080), por lo que no hace falta configurar CORS.

### Administrador inicial

Al arrancar por primera vez se crea un administrador (si no existe):

| Email | Contraseña | Rol |
|---|---|---|
| `admin@aglome.com` | `Admin123` | `ADMIN` |

Quien se registra desde la web recibe siempre el rol `CLIENTE`. Los usuarios `VENDEDOR` (u otros administradores) los crea un ADMIN desde **Gestión → Equipo**.

### Cargar datos de ejemplo (opcional)

Con el back corriendo, desde `aglome-front`:

```bash
npm run cargar-demo       # 7 categorías, 4 marcas, 16 productos de demostración
npm run cargar-catalogo   # selección del catálogo real de Aglome (precios y stock del Excel)
```

Ambos scripts se pueden repetir sin duplicar datos. Variables opcionales: `API`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`.
Si el back no corre en `localhost:8080`, indícalo con `API`, por ejemplo `API=http://localhost:8080 npm run cargar-catalogo`.

---

## Back-end en detalle

### Cómo viaja una petición

Cada capa tiene una sola responsabilidad: el **controlador** recibe y responde, el **servicio** aplica las reglas del negocio y abre las transacciones, el **repositorio** habla con la base de datos y la **entidad** representa una tabla. Los errores no se atrapan en los controladores: se lanzan como excepciones con nombre y un único `GlobalExceptionHandler` los convierte en JSON uniforme.

### Modelo de datos (12 tablas)

| Tabla | Qué guarda |
|---|---|
| `usuario` | Cuentas: email único, contraseña BCrypt, rol, intentos fallidos y bloqueo |
| `categoria` | Categorías, con subcategorías opcionales (`padre_id`) |
| `marca` | Marcas (nombre único) |
| `producto_base` | Producto general: nombre, descripción, imagen, marca, publicado y categoría |
| `variante` | Lo que se vende: SKU único, precio, stock y estado |
| `tipo_atributo` | Nombres de atributos (Color, Medida) |
| `variante_atributo` | Valor de cada atributo por variante (clave compuesta) |
| `carrito` · `carrito_item` | Un carrito por usuario y sus ítems |
| `pedido` · `detalle_pedido` | Pedido (fecha, total, estado, usuario) y sus líneas |
| `movimiento_stock` | Historial de cada cambio de stock (quién, cuándo, cuánto y por qué) |

### Lista de endpoints

La documentación con ejemplos está en Swagger (`/swagger-ui.html`).

**Acceso público**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crea una cuenta de cliente y devuelve un token |
| POST | `/api/auth/login` | Inicia sesión (423 si la cuenta está bloqueada) |
| GET | `/api/productos` | Catálogo paginado: `pagina`, `tamano` (12, máx. 50), `orden`, `direccion` |
| GET | `/api/productos/filtrar` | Filtros: `texto`, `categoriaId`, `marcaId`, `color`, `medida`, `precioMin`, `precioMax` |
| GET | `/api/productos/filtros` | Opciones para armar los filtros (colores, medidas, marcas, rango de precio) |
| GET | `/api/productos/sugerencias?texto=` | Autocompletado del buscador |
| GET | `/api/productos/buscar?texto=` | Búsqueda por nombre (JPQL) |
| GET | `/api/productos/{id}` · `/{id}/relacionados` | Detalle y productos relacionados |
| GET | `/api/productos/categoria/{categoriaId}` | Productos de una categoría |
| GET | `/api/categorias` · `/arbol` · `/{id}` | Categorías (lista, árbol, detalle) |
| GET | `/api/marcas` · `/{id}` | Marcas |

**Requieren token (cualquier usuario autenticado)**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/perfil` | Datos del usuario autenticado |
| PUT | `/api/perfil/contrasena` | Cambia la contraseña (8–72 caracteres con letras y números) |
| GET | `/api/carrito` | Ver el carrito |
| POST | `/api/carrito/items` | Agregar una variante |
| PUT · DELETE | `/api/carrito/items/{itemId}` | Cambiar cantidad · quitar ítem |
| POST | `/api/pedidos/confirmar` | Convierte el carrito en pedido `CONFIRMADO` y descuenta el stock |
| GET | `/api/pedidos` · `/{id}` | Mis pedidos (`?estado=`) y su detalle |
| POST | `/api/pedidos/{id}/cancelar` | Cancela mi pedido (solo en `PENDIENTE` o `CONFIRMADO`) |

**Roles `ADMIN` y `VENDEDOR`**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/admin/pedidos` · `/{id}` | Todos los pedidos y su detalle |
| PATCH | `/api/admin/pedidos/{id}/estado?estado=` | Avanza el pedido según el flujo permitido |
| GET | `/api/admin/stock/movimientos` | Historial de stock (filtrable y paginado) |
| POST | `/api/admin/stock/ajustes` | Ajuste manual de stock con motivo |

**Solo rol `ADMIN`**

| Método | Ruta | Descripción |
|---|---|---|
| POST · PUT · DELETE | `/api/admin/categorias` · `/{id}` | Gestión de categorías |
| POST · PUT · DELETE | `/api/admin/marcas` · `/{id}` | Gestión de marcas |
| GET · POST · PUT · DELETE | `/api/admin/productos` · `/{id}` | Gestión de productos |
| POST · PUT · DELETE | `/api/admin/variantes` · `/{id}` | Gestión de variantes |
| GET | `/api/admin/dashboard` | Ingresos, pedidos por estado, ticket promedio, ventas por mes, más vendidos y stock bajo |
| GET | `/api/admin/reportes/ventas-por-categoria` · `/ventas-por-mes` · `/productos-mas-vendidos` · `/stock-bajo` | Reportes (excluyen pedidos cancelados) |
| GET · POST | `/api/admin/usuarios` | Listar y crear administradores o vendedores |

Las rutas protegidas usan `Authorization: Bearer <token>`.

### Reglas de negocio

**Flujo de estados del pedido**

| Estado actual | Puede pasar a |
|---|---|
| `PENDIENTE` | `CONFIRMADO`, `CANCELADO` |
| `CONFIRMADO` | `EN_PREPARACION`, `CANCELADO` |
| `EN_PREPARACION` | `ENVIADO`, `CANCELADO` |
| `ENVIADO` | `ENTREGADO` |
| `ENTREGADO`, `CANCELADO` | (finales) |

Hoy `confirmar` crea el pedido ya en `CONFIRMADO`. El pedido devuelve `siguientesEstados` para que el front muestre solo los botones válidos. El cliente solo puede cancelar en `PENDIENTE` o `CONFIRMADO`; al cancelar se **devuelve el stock** (salvo desde `PENDIENTE`) y los pedidos cancelados no cuentan en reportes.

**Stock con historial.** Todo cambio de stock deja un `movimiento_stock` (`STOCK_INICIAL`, `VENTA`, `CANCELACION` o `AJUSTE`) con cantidad con signo, stock anterior y resultante, motivo, pedido relacionado y email de quien lo hizo, dentro de la misma transacción.

**Borrado lógico y auditoría.** Borrar una categoría, producto o variante lo marca `eliminado` y deja de aparecer en la API, sin perder los pedidos históricos. Categorías, productos, variantes y pedidos guardan `fecha_creacion` y `fecha_actualizacion`.

**Sesión segura.** 5 contraseñas incorrectas seguidas bloquean la cuenta 3 minutos (423); contraseña nueva de 8 a 72 caracteres con letras y números; el admin inicial sale de variables de entorno. Limitación conocida: el JWT no se revoca al cambiar la contraseña; vence a las 10 horas.

### Roles y permisos

| Acción | Sin token | Cliente | Vendedor | Administrador |
|---|---|---|---|---|
| Ver catálogo, categorías y marcas | 200 | 200 | 200 | 200 |
| Registrarse e iniciar sesión | 200 | 200 | 200 | 200 |
| Carrito, pedidos propios y perfil | 401 | 200 | 200 | 200 |
| `/api/admin/pedidos/**` y `/api/admin/stock/**` | 401 | 403 | 200 | 200 |
| Resto de `/api/admin/**` (catálogo, reportes, usuarios) | 401 | 403 | 403 | 200 |

**401** = no te identificaste; **403** = identificado pero sin permiso. Las reglas están en `SecurityConfig`.

### Errores de la API

Todos los errores tienen la misma forma:

```json
{
  "timestamp": "2026-11-10T15:30:00",
  "status": 409,
  "error": "Conflict",
  "mensaje": "Ya existe una variante con ese SKU",
  "path": "/api/admin/variantes",
  "errores": [ { "campo": "sku", "mensaje": "no debe estar vacío" } ]
}
```

`errores` solo aparece en validaciones. Las listas paginadas responden `PaginaDTO` (`contenido`, `pagina`, `tamano`, `totalElementos`, `totalPaginas`, `primera`, `ultima`).

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos o regla de negocio incumplida (transición no permitida, stock insuficiente) |
| 401 | Sin token, token inválido o credenciales incorrectas |
| 403 | Rol sin permiso |
| 404 | El recurso no existe |
| 409 | Duplicado (SKU, email, nombre de marca) |
| 423 | Cuenta bloqueada por intentos fallidos |

### Pruebas automáticas

Usan **H2 en memoria**, así que no tocan tu PostgreSQL.

| Grupo | Qué comprueban |
|---|---|
| Arranque | La aplicación completa arranca |
| Modelo | Entidades (estados, roles, auditoría) |
| Servicio | Lógica con Mockito: pedidos, stock, usuarios, reportes, bloqueo de cuenta |
| Transacción | Rollback y bloqueo con base real |
| Repositorio | Consultas JPQL, filtros con Specifications, auditoría y borrado lógico |
| Controlador | Rutas, validaciones y errores con `@WebMvcTest` |
| Seguridad | Cada ruta admin × (sin token, cliente, vendedor, admin) y una prueba que falla si aparece una ruta admin sin cubrir |

### Base de una versión anterior

`ddl-auto=update` agrega tablas y columnas, pero **no actualiza los CHECK** de las columnas enum (`pedido.estado`, `usuario.rol`). Si tu base viene de una versión antigua, lo más simple es recrearla (son datos de prueba):

```sql
DROP DATABASE aglome_spring_db;
CREATE DATABASE aglome_spring_db;
```

---

## Front-end en detalle

### Pantallas y rutas

| Ruta | Acceso | Pantalla |
|---|---|---|
| `/` | Público | Portada: muestras de color, categorías, destacados y pie |
| `/productos` · `/productos/:id` | Público | Catálogo con filtros y ficha de producto |
| `/ingresar` · `/registro` | Solo invitados | Inicio de sesión y registro |
| `/cuenta` | Con sesión | Mi cuenta (cambio de contraseña) |
| `/carrito` · `/pedido-confirmado/:id` · `/mis-pedidos` | Con sesión | Carrito, confirmación y mis pedidos |
| `/gestion/resumen` | ADMIN | Indicadores y reportes |
| `/gestion/pedidos` · `/gestion/inventario` | ADMIN y VENDEDOR | Pedidos y stock |
| `/gestion/catalogo` · `categorias` · `marcas` · `equipo` | ADMIN | Edición de la tienda y del equipo |

Las pantallas se cargan de forma diferida y las rutas se protegen con *guards* por sesión y rol (además de la validación del back). El token se envía solo en las llamadas a `/api`; si vence o el back responde 401, se cierra la sesión y se vuelve al ingreso.

### Detalles de la interfaz
- Ficha de producto: muestras de color, calculadora de planchas por m², "Comprar ahora", consulta por WhatsApp, pestañas, relacionados, ampliación de foto y animación al agregar al carrito.
- Carrito en panel lateral (se abre al agregar) y en página propia.
- Estilos con **SASS**, adaptables a móvil; las animaciones respetan `prefers-reduced-motion`.

### Datos del negocio

Contacto, WhatsApp (si queda vacío, el botón no se muestra), misión, visión y sedes (dirección, horarios y foto en `public/sedes/`) están en `aglome-front/src/environments/negocio.ts`. Cada sede tiene un botón "Cómo llegar" que abre Google Maps. El color de marca se cambia en `src/styles/_variables.scss`.

---

## Documentación y pruebas

| Recurso | Dónde |
|---|---|
| Swagger UI (API interactiva) | http://localhost:8080/swagger-ui.html |
| OpenAPI en JSON | http://localhost:8080/v3/api-docs |
| Catálogo público (JSON) | http://localhost:8080/api/productos |

**Pruebas del back** (428 pruebas esperadas, 0 fallos; usan H2 en memoria, no tocan tu PostgreSQL):

```bash
cd aglome-tienda
./mvnw clean test
```

**Pruebas y build del front:**

```bash
cd aglome-front
npm test          # Vitest
npm run build     # compilación de producción en dist/
```

---

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `Connection refused` / `password authentication failed` | PostgreSQL apagado, base inexistente o credenciales incorrectas en `application.properties` |
| `Port 8080 was already in use` | Hay otra copia corriendo; deténla o cambia `server.port` |
| `class file version 69.0 ... only recognizes up to 65.0` | Usa JDK 21 |
| La web se ve en blanco en `localhost:8080` | Falta compilar el front: ejecuta `npm run build:back` y reinicia el back |
| `401` con un token que antes funcionaba | El token venció o cambió `JWT_SECRET`: vuelve a iniciar sesión |
| `403` en `/gestion` o `/api/admin/**` | Tu usuario es CLIENTE; ingresa con un ADMIN o VENDEDOR |
| `violates check constraint` en `pedido.estado` o `usuario.rol` | Base de una versión antigua: recrea la base de datos (`DROP DATABASE` / `CREATE DATABASE`) |

---

## Autores

Proyecto académico — Universidad Tecnológica del Perú (UTP).

<!-- Agrega aquí los nombres del equipo y, si quieres, una licencia. -->
