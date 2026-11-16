# Aglome-tienda · API REST con Spring Boot

Back-end de la plataforma web de **Aglome Perú**, una empresa de Chimbote (Perú) dedicada a la venta de madera, melamina y productos de ferretería.
Proyecto académico del curso de **Back-end con Spring Boot**, que cubre dos unidades de aprendizaje y una etapa final de **mejora del back-end** (13 pasos, resumidos al final de este README):

| Unidad | Tema | Qué se implementó |
|---|---|---|
| **1** | API REST con TDD | Controladores, servicios, inyección de dependencias, métodos HTTP y pruebas automáticas |
| **2** | Back-end con bases de datos | JPA e Hibernate sobre PostgreSQL, JPQL, transacciones, Spring Security, roles y JWT |
| **Mejora** | Pasos 1 a 13 | Errores globales, paginación, filtros, marcas, auditoría, flujo de pedidos, historial de stock, rol VENDEDOR, sesión segura, Swagger y 428 pruebas |

**Estado actual:** 428 pruebas, 0 fallos · 15 controladores · ~70 rutas · 3 roles · listo para conectar el frontend en Angular.

---

## Contexto del proyecto

**El problema.** Aglome no tiene catálogo web ni una base de datos digital accesible: su inventario interno no está conectado a internet y la atención es 100 % por llamadas o visitas. Los clientes deben llamar para consultar disponibilidad, precios, colores y medidas, y pueden perder hasta medio día buscando el producto correcto. El personal (administradora y ejecutiva de ventas) se sobrecarga con llamadas repetitivas y no puede dedicarse a tareas de mayor valor.

**La solución.** Una plataforma web formada por una **API REST** (este repositorio) y un **frontend en Angular** (siguiente etapa) que servirá de vitrina digital: catálogo con fotos, precios y medidas, filtros por categoría, marca, color y medida, botón de contacto por WhatsApp y mapa de ubicación.

**Qué hace este back-end.** Guarda el catálogo (productos con marca, variantes de color y medida, SKU, precio y stock) en PostgreSQL y lo entrega por una API REST con paginación y filtros. Incluye carrito, pedidos con flujo de estados, historial de movimientos de stock, reportes y un panel administrativo con tres roles (`ADMIN`, `VENDEDOR`, `CLIENTE`) protegido con JWT, bloqueo por intentos fallidos y errores uniformes en JSON.

**De dónde partimos.** La entrega de las Unidades 1 y 2 dejó una API funcional pero básica: 10 tablas, marca como texto libre, pedidos con 3 estados, borrado físico, errores como texto con `try/catch` en cada controlador, un administrador con contraseña escrita en el código y 49 pruebas. La etapa de mejora la convirtió en un back-end listo para producción académica.

---

## Tabla de contenido

1. [Estructura del proyecto](#estructura-del-proyecto)
2. [Tecnologías](#tecnologías)
3. [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
4. [Probar en el navegador y con Swagger](#probar-en-el-navegador-y-con-swagger)
5. [Pruebas automáticas](#pruebas-automáticas)
6. [Demostración paso a paso con Thunder Client](#demostración-paso-a-paso-con-thunder-client)
7. [Qué se guarda y cómo verlo en la base de datos](#qué-se-guarda-y-cómo-verlo-en-la-base-de-datos)
8. [Lista de endpoints](#lista-de-endpoints)
9. [Reglas de negocio importantes](#reglas-de-negocio-importantes)
10. [Roles y permisos](#roles-y-permisos)
11. [Errores de la API](#errores-de-la-api)
12. [Unidad 1: resumen](#unidad-1-resumen)
13. [Unidad 2: explicación detallada](#unidad-2-explicación-detallada)
14. [Mejoras del back-end (pasos 1 a 13)](#mejoras-del-back-end-pasos-1-a-13)
15. [Problemas frecuentes](#problemas-frecuentes)

---

## Estructura del proyecto

La carpeta que se abre en VS Code es la raíz del repositorio, `Aglome-tienda`, y el proyecto Maven está dentro de la carpeta `aglome-tienda`.

```
Aglome-tienda/                                 ← raíz del repositorio
├── README.md
└── aglome-tienda/                             ← proyecto Maven (aquí se ejecutan los comandos)
    ├── pom.xml   mvnw   mvnw.cmd              Maven y su Wrapper
    └── src/
        ├── main/java/pe/edu/utp/aglome_tienda/
        │   ├── AglomeTiendaApplication.java
        │   ├── config/        DataInitializer (admin inicial), JpaAuditingConfig, OpenApiConfig (Swagger)
        │   ├── controller/    15 controladores: Auth, Perfil, Categoria, Marca, Producto, Carrito, Pedido
        │   │                  + Admin{Categoria,Marca,Producto,Variante,Pedido,Stock,Reporte,Dashboard,Usuario}
        │   ├── service/       Auth, Categoria, Marca, ProductoBase, Variante, Carrito, Pedido,
        │   │                  MovimientoStock, Reporte, UsuarioAdmin
        │   ├── repository/    Spring Data JPA + ProductoSpecification (filtros dinámicos)
        │   ├── model/         Entidades: Usuario, Rol, Categoria, Marca, ProductoBase, Variante, TipoAtributo,
        │   │                  VarianteAtributo, Carrito, CarritoItem, Pedido, EstadoPedido, DetallePedido,
        │   │                  MovimientoStock, TipoMovimiento, Auditable
        │   ├── dto/           Objetos de entrada y salida (incluye PaginaDTO, ApiError, reportes)
        │   ├── exception/     Excepciones con nombre + GlobalExceptionHandler + ApiError
        │   └── security/      SecurityConfig, JwtService, JwtAuthFilter, ErrorJsonWriter, CustomUserDetailsService
        └── test/              428 pruebas (JUnit 5, Mockito, MockMvc, @DataJpaTest, @SpringBootTest, H2)
```

### Cómo viaja una petición

```
Cliente (HTTP) → Controller → Service → Repository → Entity → PostgreSQL
                     ↓ (si algo falla)
              GlobalExceptionHandler → JSON uniforme (ApiError)
```

Cada capa tiene una sola responsabilidad: el controlador recibe y responde, el servicio aplica las reglas del negocio y abre las transacciones, el repositorio habla con la base de datos y la entidad representa una tabla. Los errores no se atrapan en los controladores: se lanzan como excepciones con nombre y un único `GlobalExceptionHandler` los convierte en respuestas JSON iguales para toda la API.

### Modelo de datos (12 tablas)

| Tabla | Qué guarda |
|---|---|
| `usuario` | Cuentas: email único, contraseña BCrypt, rol, intentos fallidos y bloqueo |
| `categoria` | Categorías, con subcategorías opcionales (`padre_id`) |
| `marca` | Marcas (nombre único); los productos la referencian |
| `producto_base` | Producto general: nombre, descripción, imagen, marca, publicado y categoría |
| `variante` | Lo que se vende: SKU único, precio, stock y estado |
| `tipo_atributo` | Nombres de atributos (Color, Medida) |
| `variante_atributo` | Valor de cada atributo por variante (clave compuesta) |
| `carrito` · `carrito_item` | Un carrito por usuario y sus ítems |
| `pedido` · `detalle_pedido` | Pedido (fecha, total, estado, usuario) y sus líneas |
| `movimiento_stock` | Historial de cada cambio de stock (quién, cuándo, cuánto y por qué) |

Categorías, productos, variantes y pedidos llevan **auditoría de fechas** (`fecha_creacion`, `fecha_actualizacion`). Categorías, productos y variantes tienen además **borrado lógico** (campo `eliminado`): borrar oculta el registro pero conserva el historial de pedidos. Quién hizo cada cambio de stock queda en `movimiento_stock`.

---

## Tecnologías

| Herramienta | Para qué se usa |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 4.1.1 | Framework: Web MVC, Data JPA, Security y Validation |
| Hibernate 7 | Convierte las clases Java en tablas |
| PostgreSQL | Base de datos de la aplicación |
| H2 en memoria | Base temporal solo para las pruebas automáticas |
| jjwt 0.12.6 | Creación y validación de tokens JWT |
| springdoc-openapi 3.2.0 | Documentación interactiva (Swagger UI) |
| Lombok | Evita escribir getters, setters y constructores |
| JUnit 5, Mockito, MockMvc y Spring Security Test | Pruebas automáticas |
| Maven (`mvnw`) | Compila, ejecuta y prueba (no hace falta instalarlo) |
| Thunder Client | Pruebas manuales de la API |
| pgAdmin | Ver las tablas y los datos |

---

## Cómo ejecutar el proyecto

### 1. Requisitos

- **JDK 21** (`java -version`) y **PostgreSQL** encendido (puerto 5432).
- **VS Code** con las extensiones de Java y Spring Boot, y **Thunder Client**.

### 2. Clonar el repositorio

```bash
git clone <URL-DE-ESTE-REPOSITORIO>
cd Aglome-tienda/aglome-tienda
```

Todos los comandos se ejecutan dentro de `aglome-tienda`, la carpeta que contiene el `pom.xml`.

### 3. Crear tu base de datos

```sql
CREATE DATABASE aglome_spring_db;
```

No hay que crear tablas: Hibernate las crea al arrancar (`ddl-auto=update`). **Si ya tenías una base de una versión anterior**, mira [Base de una versión anterior](#base-de-una-versión-anterior).

### 4. Configurar la aplicación

La conexión a la base está en `src/main/resources/application.properties` (`spring.datasource.url`, `username` y `password`): ajústala a tu PostgreSQL local. El resto se controla con **variables de entorno**, todas con valor por defecto para desarrollo:

| Variable | Qué es | Por defecto |
|---|---|---|
| `JWT_SECRET` | Clave que firma los tokens (mínimo 32 caracteres) | clave de desarrollo |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `36000000` (10 horas) |
| `CORS_ORIGENES` | Orígenes del frontend permitidos (separados por coma) | `http://localhost:4200` |
| `ADMIN_EMAIL` | Email del administrador que se crea al arrancar | `admin@aglome.com` |
| `ADMIN_PASSWORD` | Contraseña de ese administrador | `Admin123` |

**Windows (PowerShell):**

```powershell
$env:JWT_SECRET = "una-clave-larga-de-al-menos-32-caracteres"
$env:ADMIN_EMAIL = "jefa@aglome.pe"
$env:ADMIN_PASSWORD = "UnaClaveSegura2026"
```

**Linux / macOS:** `export JWT_SECRET="..."`, `export ADMIN_EMAIL="..."`, etc.

Las variables duran mientras esa terminal esté abierta; define las variables **antes** de arrancar.

> **Importante:** los valores por defecto (`JWT_SECRET`, `Admin123`) son solo para desarrollo. Si publicas la aplicación, define los tuyos. Además `application.properties` contiene la contraseña de tu PostgreSQL local en texto plano: no la subas a un repositorio público (o cámbiala por `${DB_PASSWORD:...}`).

### 5. Arrancar la API

```powershell
.\mvnw.cmd clean spring-boot:run
```

En Linux o macOS: `chmod +x mvnw` y luego `./mvnw clean spring-boot:run`. También desde VS Code con el botón **Run** sobre `main` en `AglomeTiendaApplication.java`, o como jar: `.\mvnw.cmd clean package "-DskipTests"` y `java -jar target\aglome-tienda-0.0.1-SNAPSHOT.jar`.

Arrancó bien si la consola muestra `Tomcat started on port 8080` y `Started AglomeTiendaApplication`. Se detiene con `Ctrl + C`; cada cambio de código requiere reiniciar. Si aparece `Port 8080 was already in use`, ya hay otra copia corriendo.

### 6. Administrador automático

Al arrancar, `DataInitializer` **crea un administrador si no existe**, con `ADMIN_EMAIL` y `ADMIN_PASSWORD` (o los valores por defecto):

| Email | Contraseña | Rol |
|---|---|---|
| `admin@aglome.com` | `Admin123` | `ADMIN` |

Si usa la contraseña por defecto, la consola muestra una advertencia. Si el administrador ya existe, no se modifica. Quien se registre por la API siempre recibe el rol `CLIENTE`. Los usuarios `VENDEDOR` (y otros administradores) los crea un ADMIN con `POST /api/admin/usuarios`.

### Base de una versión anterior

`ddl-auto=update` agrega tablas y columnas, pero **no actualiza los CHECK** que PostgreSQL pone a las columnas enum (`pedido.estado`, `usuario.rol`) ni migra datos antiguos (por ejemplo, la marca que antes era texto libre). Si tu base viene de una versión anterior al paso 6, lo más simple es **recrearla**, porque son datos de prueba:

```sql
DROP DATABASE aglome_spring_db;
CREATE DATABASE aglome_spring_db;
```

Al arrancar, Hibernate crea todas las tablas y `DataInitializer` crea el administrador. Con una base nueva no hace falta nada más.

---|---|---|
| `paso6_migrar_marcas.sql` | Tras el paso 6 | Pasa la marca de texto libre a la tabla `marca` |
| `paso7_auditoria_borrado_logico.sql` | Tras el paso 7 | Rellena las fechas de auditoría y comprueba `eliminado` |
| `paso10_estados_pedido.sql` | Tras el paso 10 | Amplía el CHECK de `pedido.estado` a los 6 estados |
| `paso11_historial_inicial.sql` | Opcional | Crea el movimiento `STOCK_INICIAL` de las variantes existentes |
| `paso12_rol_vendedor.sql` | Tras el paso 12 | Amplía el CHECK de `usuario.rol` para `VENDEDOR` |
| `paso13_seguridad_usuario.sql` | Tras el paso 13 | Verifica las columnas de bloqueo y permite desbloquear una cuenta |

Con una base nueva y vacía no se necesita ninguno.

---

## Probar en el navegador y con Swagger

Al abrir `http://localhost:8080/` el navegador muestra una pantalla en blanco, y **es lo esperado**: es una API que responde JSON, sin página web. Lo que sí se abre en el navegador son las rutas **GET públicas**:

| URL | Qué muestra |
|---|---|
| `http://localhost:8080/api/productos` | Catálogo paginado de productos publicados |
| `http://localhost:8080/api/productos/filtrar?texto=melamina&marcaId=1` | Filtros combinados |
| `http://localhost:8080/api/categorias/arbol` | Categorías con sus subcategorías |
| `http://localhost:8080/api/marcas` | Marcas |
| `http://localhost:8080/swagger-ui.html` | **Documentación interactiva** de toda la API |

En Swagger se pulsa **Authorize**, se pega el token (sin la palabra `Bearer`) y se pueden probar también las rutas protegidas. El JSON crudo está en `/v3/api-docs`. El resto (POST, PUT, PATCH, DELETE y `/api/admin/**`) se prueba con Swagger, Thunder Client o Postman.

---

## Pruebas automáticas

```powershell
.\mvnw.cmd clean test
```

Resultado esperado: **428 tests, 0 fallos, `BUILD SUCCESS`**. Usan **H2 en memoria**, así que **no tocan tu PostgreSQL**.

| Grupo | Qué comprueban |
|---|---|
| Arranque | La aplicación completa arranca |
| Modelo | Comportamiento de entidades (estados, roles, auditoría) |
| Servicio | Lógica con Mockito: pedidos, stock, usuarios, reportes, bloqueo de cuenta |
| Transacción | `PedidoTransaccionTest` y `AuthBloqueoIntegracionTest`: rollback y bloqueo con base real |
| Repositorio | Consultas JPQL, filtros con Specifications, auditoría y borrado lógico contra una base real |
| Controlador | Rutas, validaciones y respuestas de error con `@WebMvcTest` |
| Seguridad | `SeguridadRutasAdminTest`: cada ruta admin × (sin token, cliente, vendedor, admin), más una prueba que falla si aparece una ruta admin nueva sin cubrir; `JwtServiceTest` y `JwtAuthFilterTest` |



---

## Demostración paso a paso con Thunder Client

Esta secuencia crea una **categoría, un producto y dos variantes nuevos** (Ferretería › Bisagra de Acero), y recorre todos los temas de la Unidad 2: datos, JPQL, transacciones, roles y JWT. Sigue el orden, porque cada paso usa datos del anterior.

### Antes de empezar

- La API debe estar corriendo. Crea dos pestañas con nombre en Thunder Client: **ADMIN** y **CLIENTE**.
- Para enviar el token, en la pestaña **Headers** agrega `Authorization` con el valor `Bearer <tu-token>`: con un espacio después de `Bearer`, sin comillas y copiando solo el valor del campo `token`. No lo pongas también en la pestaña *Auth*, o se duplicará.
- Thunder guarda los headers **por pestaña**: si cambias de token, actualízalo en cada una.
- En los JSON, reemplaza los textos en mayúsculas (`ID_CATEGORIA`, `UUID_PRODUCTO`, `UUID_VARIANTE_3PUL`, `ID_PEDIDO`) por lo que devuelva cada paso, **sin** los signos `<` ni `>`.

### Con el token del administrador

**D1. Login**

```
POST http://localhost:8080/api/auth/login
```
```json
{ "email": "admin@aglome.com", "password": "Admin123" }
```
Esperado: **200** con `token` y `rol: "ADMIN"`. Copia el token a la pestaña ADMIN.

**D2. Crear la categoría**

```
POST http://localhost:8080/api/admin/categorias
```
```json
{ "nombre": "Ferretería" }
```
Esperado: **201** con el `id`. Anótalo. *Se guarda una fila en `categoria`.*

**D3. Crear el producto**

```
POST http://localhost:8080/api/admin/productos
```
```json
{
  "nombre": "Bisagra de Acero",
  "descripcion": "Bisagra de acero para puertas y muebles",
  "marcaId": ID_MARCA,
  "categoriaId": ID_CATEGORIA,
  "publicado": true
}
```
Esperado: **201** con el `id` (UUID) del producto. (Antes crea la marca con `POST /api/admin/marcas` y `{ "nombre": "Aglome" }`, y usa su `id` como `ID_MARCA`.) *Se guarda una fila en `producto_base`.*

**D4. Crear la variante de 3 pulgadas**

```
POST http://localhost:8080/api/admin/variantes
```
```json
{
  "productoBaseId": "UUID_PRODUCTO",
  "sku": "BISAG-ACE-3PUL",
  "precioVenta": 4.50,
  "stock": 100,
  "atributos": { "Material": "Acero", "Medida": "3 pulgadas" }
}
```
Esperado: **201** con el `id` de la variante. *Se guarda en `variante`, `tipo_atributo` y `variante_atributo`, en una sola transacción.* Si repites el SKU: **409 "Ya existe una variante con ese SKU"**.

**D5. Crear la variante de 4 pulgadas**

```
POST http://localhost:8080/api/admin/variantes
```
```json
{
  "productoBaseId": "UUID_PRODUCTO",
  "sku": "BISAG-ACE-4PUL",
  "precioVenta": 6.00,
  "stock": 80,
  "atributos": { "Material": "Acero", "Medida": "4 pulgadas" }
}
```
Esperado: **201**. Muestra la diferencia entre **producto** (la bisagra) y **variante** (lo que se vende, con su SKU, precio y stock).

### Sin token

**D6. Ver el catálogo:** `GET http://localhost:8080/api/productos`. Esperado: **200**, con la bisagra y sus dos variantes.

**D7. Buscar con JPQL:** `GET http://localhost:8080/api/productos/buscar?texto=bisagra`. Esperado: **200**, solo la bisagra.

### Con el token del cliente

**D8. Registrar un cliente**

```
POST http://localhost:8080/api/auth/registro
```
```json
{ "nombre": "Cliente Demo", "email": "demo1@test.com", "password": "123456" }
```
Esperado: **200** con el `token` y `rol: "CLIENTE"`. Cópialo a la pestaña CLIENTE. Si ese email ya existe, usa `POST /api/auth/login`. *Se guarda una fila en `usuario`, con la contraseña encriptada (empieza con `$2a$`).*

**D9. Agregar al carrito**

```
POST http://localhost:8080/api/carrito/items
```
```json
{ "varianteId": "UUID_VARIANTE_3PUL", "cantidad": 10 }
```
Esperado: **200** con el carrito. *Se guardan filas en `carrito` y `carrito_item`.*

**D10. Confirmar la compra** (sin body)

```
POST http://localhost:8080/api/pedidos/confirmar
```
Esperado: **200**, estado `CONFIRMADO` y total **45.00** (10 por 4.50). Copia el `id` del pedido. *En una sola transacción: se crea el pedido y sus detalles, baja el stock y se vacía el carrito.*

### Resultados

**D11. El stock bajó:** `GET http://localhost:8080/api/productos` (sin token). La variante de 3 pulgadas pasa de 100 a **90**.

**D12. Reporte de ventas (JPQL con JOIN, SUM y GROUP BY):** `GET http://localhost:8080/api/admin/reportes/ventas-por-categoria` con el token del admin. Esperado: **200**, con las categorías que tienen ventas, de mayor a menor. Cada elemento trae `categoria` y `unidades`.

**D13. El admin cancela el pedido**

```
PATCH http://localhost:8080/api/admin/pedidos/ID_PEDIDO/estado?estado=CANCELADO
```
Esperado: **200** y `"estado": "CANCELADO"`. Repite D11: el stock vuelve a **100**, porque la cancelación devuelve el stock dentro de una transacción. Si repites el PATCH, responde **400** porque `CANCELADO` es un estado final (ver [flujo de estados](#flujo-de-estados-del-pedido)). El cliente también puede cancelar su pedido (mientras esté `PENDIENTE` o `CONFIRMADO`) con `POST /api/pedidos/{id}/cancelar`.

### Seguridad

| Petición | Token | Resultado | Por qué |
|---|---|---|---|
| `GET /api/carrito` | ninguno | **401** | No se identificó |
| `GET /api/admin/pedidos` | cliente | **403** | Está identificado, pero su rol no es ADMIN ni VENDEDOR |
| `GET /api/admin/pedidos` | admin | **200** | Tiene el rol requerido |

### Ensayar y presentar

- El **SKU** y el **email** son únicos y repetirlos da error 400. Si ensayas con `BISAG-ACE-3PUL` y `demo1@test.com`, para presentar usa otros (`BISAG-ACE-3PUL-B`, `demo2@test.com`) o borra antes el producto del ensayo con `DELETE /api/admin/productos/{id}` (solo es posible si no tiene pedidos).
- Para una demostración corta en vivo bastan D2, D3, D4, D6, D9, D10, D11 y D12. Los logins se dejan hechos antes.
- La **marca** ahora es una entidad (`marca`): se crea una vez y los productos la referencian por `marcaId`.

---

Con la API corriendo también hay documentación interactiva, con todas las rutas, en **`http://localhost:8080/swagger-ui.html`**.

---

## Qué se guarda y cómo verlo en la base de datos

Con PostgreSQL, todos los datos creados **se conservan al apagar la aplicación**. Lo único que no se guarda en la base son los **tokens**: el servidor solo comprueba su firma.

| Qué | Dónde queda | ¿Se pierde al reiniciar? |
|---|---|---|
| Cuentas (admin, vendedores, clientes) | Tabla `usuario` | No |
| Categorías, productos y variantes | PostgreSQL (borrado lógico) | No |
| Carritos, pedidos e historial de stock | PostgreSQL | No |
| Token JWT | Solo en el cliente (Thunder, Swagger, navegador) | Deja de servir a las 10 horas o si cambia `JWT_SECRET` |

### Ver las tablas en pgAdmin

*Servers › PostgreSQL › Databases › aglome_spring_db › Schemas › public › Tables*, clic derecho › *View/Edit Data*, o en el *Query Tool*:

```sql
SELECT id, nombre, email, rol, intentos_fallidos, bloqueado_hasta FROM usuario;
SELECT id, nombre FROM marca;
SELECT id, nombre, publicado, eliminado FROM producto_base;
SELECT sku, precio_venta, stock FROM variante;
SELECT id, estado, total, fecha FROM pedido;
SELECT tipo, cantidad, stock_anterior, stock_resultante, usuario_email, fecha
FROM movimiento_stock ORDER BY fecha DESC;

SELECT c.nombre, SUM(d.cantidad)
FROM detalle_pedido d
JOIN pedido pe ON pe.id = d.pedido_id
JOIN variante v ON v.id = d.variante_id
JOIN producto_base p ON p.id = v.producto_base_id
JOIN categoria c ON c.id = p.categoria_id
WHERE pe.estado <> 'CANCELADO'
GROUP BY c.nombre;
```

La última consulta es la versión SQL del reporte y debe coincidir con `GET /api/admin/reportes/ventas-por-categoria`.

| Petición | Tablas que cambian |
|---|---|
| `POST /api/auth/registro` | `usuario` |
| `POST /api/admin/categorias` · `/marcas` | `categoria` · `marca` |
| `POST /api/admin/productos` | `producto_base` |
| `POST /api/admin/variantes` | `variante`, `tipo_atributo`, `variante_atributo`, `movimiento_stock` (STOCK_INICIAL) |
| `POST /api/carrito/items` | `carrito`, `carrito_item` |
| `POST /api/pedidos/confirmar` | `pedido`, `detalle_pedido`, `variante` (baja el stock), `movimiento_stock` (VENTA) y `carrito_item` (se vacía) |
| `PATCH /api/admin/pedidos/{id}/estado` · `POST /api/pedidos/{id}/cancelar` | `pedido`; al cancelar también `variante` y `movimiento_stock` (CANCELACION) |
| `POST /api/admin/stock/ajustes` | `variante` y `movimiento_stock` (AJUSTE) |
| `POST /api/admin/usuarios` | `usuario` |

### Crear otro administrador o un vendedor

Con la API, sin tocar la base: un ADMIN hace `POST /api/admin/usuarios` con `{ "nombre": "...", "email": "...", "password": "...", "rol": "VENDEDOR" }` (el rol puede ser `ADMIN` o `VENDEDOR`; `CLIENTE` se rechaza con 400 y un email repetido con 409).

---

## Lista de endpoints

La documentación completa con ejemplos está en Swagger (`/swagger-ui.html`).

**Acceso público**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crea una cuenta de cliente y devuelve un token |
| POST | `/api/auth/login` | Inicia sesión (423 si la cuenta está bloqueada) |
| GET | `/api/productos` | Catálogo paginado: `pagina`, `tamano` (12, máx. 50), `orden` (`nombre`/`marca`), `direccion` |
| GET | `/api/productos/filtrar` | Filtros: `texto`, `categoriaId`, `marcaId`, `color`, `medida`, `precioMin`, `precioMax` (+ paginación) |
| GET | `/api/productos/filtros` | Opciones disponibles para armar los filtros (colores, medidas, marcas, rango de precio) |
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
| PUT | `/api/perfil/contrasena` | Cambia la contraseña (pide la actual; 8–72 caracteres con letras y números) |
| GET | `/api/carrito` | Ver el carrito |
| POST | `/api/carrito/items` | Agregar una variante |
| PUT · DELETE | `/api/carrito/items/{itemId}` | Cambiar cantidad · quitar ítem |
| POST | `/api/pedidos/confirmar` | Convierte el carrito en un pedido `CONFIRMADO` y descuenta el stock |
| GET | `/api/pedidos` · `/{id}` | Mis pedidos (admite `?estado=`) y su detalle |
| POST | `/api/pedidos/{id}/cancelar` | Cancela mi pedido (solo en `PENDIENTE` o `CONFIRMADO`) |

**Roles `ADMIN` y `VENDEDOR`**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/admin/pedidos` · `/{id}` | Todos los pedidos (`?estado=`) y su detalle |
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
| GET | `/api/admin/dashboard` | Resumen: ingresos, pedidos por estado, ticket promedio, ventas por mes, más vendidos y stock bajo |
| GET | `/api/admin/reportes/ventas-por-categoria` · `/ventas-por-mes` · `/productos-mas-vendidos` · `/stock-bajo` | Reportes (excluyen pedidos cancelados) |
| GET · POST | `/api/admin/usuarios` | Listar y crear administradores o vendedores |

---

## Reglas de negocio importantes

### Flujo de estados del pedido

```
PENDIENTE → CONFIRMADO → EN_PREPARACION → ENVIADO → ENTREGADO
    └──────────┴───────────────┘
              ↓ (cancelar)
          CANCELADO
```

| Estado actual | Puede pasar a |
|---|---|
| `PENDIENTE` | `CONFIRMADO`, `CANCELADO` |
| `CONFIRMADO` | `EN_PREPARACION`, `CANCELADO` |
| `EN_PREPARACION` | `ENVIADO`, `CANCELADO` |
| `ENVIADO` | `ENTREGADO` |
| `ENTREGADO`, `CANCELADO` | (finales: no cambian) |

`PENDIENTE` queda como estado inicial posible del flujo; hoy `confirmar` crea el pedido ya en `CONFIRMADO`. El pedido devuelve `siguientesEstados` para que el frontend muestre solo los botones válidos. El cliente solo puede cancelar en `PENDIENTE` o `CONFIRMADO`. Al cancelar se **devuelve el stock** (salvo desde `PENDIENTE`) y queda el movimiento `CANCELACION`. Los pedidos cancelados **no cuentan** en reportes ni dashboard.

### Stock con historial

Todo cambio de stock deja un `movimiento_stock` (tipo `STOCK_INICIAL`, `VENTA`, `CANCELACION` o `AJUSTE`) con cantidad con signo, stock anterior y resultante, motivo, pedido relacionado y el email de quien lo hizo. Se guarda dentro de la misma transacción: si falla el cambio, tampoco queda el registro.

### Borrado lógico y auditoría

Borrar una categoría, producto o variante lo marca `eliminado` y deja de aparecer en la API, sin perder los pedidos históricos. Categorías, productos, variantes y pedidos guardan cuándo se crearon y modificaron (`fecha_creacion`, `fecha_actualizacion`).

### Sesión segura

- **5 contraseñas incorrectas seguidas** bloquean la cuenta **3 minutos** (respuesta **423**); un login correcto reinicia el contador. Equivocarse con la contraseña actual al cambiarla también cuenta.
- Contraseña nueva: 8 a 72 caracteres, con letras y números.
- El administrador inicial sale de variables de entorno, no del código.
- Limitación conocida: el JWT no se revoca al cambiar la contraseña (es sin estado); vence a las 10 horas.

---

## Roles y permisos

| Acción | Sin token | Cliente | Vendedor | Administrador |
|---|---|---|---|---|
| Ver catálogo, categorías y marcas | 200 | 200 | 200 | 200 |
| Registrarse e iniciar sesión | 200 | 200 | 200 | 200 |
| Carrito, pedidos propios y perfil | 401 | 200 | 200 | 200 |
| `/api/admin/pedidos/**` y `/api/admin/stock/**` | 401 | 403 | 200 | 200 |
| Resto de `/api/admin/**` (catálogo, reportes, usuarios) | 401 | 403 | 403 | 200 |

Las reglas están en `SecurityConfig`. **401** = no te identificaste (sin token o token inválido); **403** = estás identificado pero tu rol no tiene permiso.

---

## Errores de la API

Todos los errores (de validación, de negocio y de seguridad) tienen la misma forma:

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

`errores` solo aparece en validaciones (400 por campo). Códigos usados:

| Código | Cuándo | Excepción |
|---|---|---|
| 400 | Datos inválidos o regla de negocio incumplida (transición de estado no permitida, stock insuficiente) | `ReglaNegocioException` / validación |
| 401 | Sin token, token inválido o credenciales incorrectas | `CredencialesInvalidasException` |
| 403 | Rol sin permiso | Filtro de seguridad |
| 404 | El recurso no existe | `RecursoNoEncontradoException` |
| 409 | Duplicado (SKU, email, nombre de marca) | `ConflictoException` |
| 423 | Cuenta bloqueada por intentos fallidos | `CuentaBloqueadaException` |

Las listas paginadas responden `PaginaDTO`: `contenido`, `pagina`, `tamano`, `totalElementos`, `totalPaginas`, `primera`, `ultima`.

---

## Unidad 1: resumen

La Unidad 1 dejó construida la base de la API: arquitectura en capas, los endpoints REST iniciales y pruebas automáticas.

| Tema | Qué se hizo |
|---|---|
| Estructura de Spring Boot | Proyecto Maven con los starters web, JPA, security y validation; configuración en `application.properties` |
| Controladores y endpoints | controladores `@RestController` (hoy 15, con ~70 rutas) |
| Inyección de dependencias | Por constructor con `@RequiredArgsConstructor`: Spring entrega los objetos, sin usar `new` |
| DTO y validación | Los datos de entrada se validan con `@Valid`; si no cumplen, la API responde 400 |
| Métodos HTTP | GET lee, POST crea, PUT reemplaza, PATCH modifica una parte y DELETE borra |
| TDD | Pruebas de modelo, de servicio con Mockito y de controlador con MockMvc |

TDD sigue un ciclo: escribir una prueba que falla, escribir el código mínimo para que pase y mejorar el código sin romper nada. PATCH se usa para cambiar el estado de un pedido porque modifica un solo campo; PUT reemplazaría el recurso completo.

---

## Unidad 2: explicación detallada

La Unidad 2 llevó la API a una base de datos real y le agregó control de acceso. Temario de la unidad: manejo de datos con JPA e Hibernate, operaciones CRUD, JPQL, transacciones, Spring Security, roles y permisos, autenticación con JWT e integración de temas (la demostración de arriba integra todos).

### JPA, Hibernate y CRUD

Cada entidad de `model/` es una clase con `@Entity` que se convierte en una tabla. Las relaciones se declaran con `@ManyToOne`, `@OneToMany` y `@OneToOne`, y restricciones como `@Column(unique = true)` hacen únicos el email y el SKU. Los repositorios extienden `JpaRepository`, que ya trae las operaciones CRUD sin escribir SQL:

| Operación | Método | Endpoint de ejemplo |
|---|---|---|
| Crear | `save()` | `POST /api/admin/categorias` |
| Leer | `findById()`, `findAll()` | `GET /api/productos` |
| Actualizar | `save()` sobre un registro existente | `PUT /api/admin/productos/{id}` |
| Borrar | `delete()` | `DELETE /api/admin/productos/{id}` |

También hay consultas generadas por el nombre del método, como `findByEmail` y `existsBySku`. Con `ddl-auto=update`, Hibernate crea las tablas la primera vez y las conserva con sus datos en los siguientes arranques.

### JPQL

JPQL consulta las **clases Java** y no las tablas. Se usa cuando el nombre del método ya no alcanza. Las tres consultas originales con `@Query` (hoy hay más, para reportes y dashboard):

| Consulta | Dónde está | Qué hace |
|---|---|---|
| `buscarPublicadosPorNombre` | `ProductoBaseRepository` | Busca productos publicados por nombre, sin distinguir mayúsculas |
| `buscarPorUsuarioYEstado` | `PedidoRepository` | Pedidos de un usuario filtrados por estado |
| `unidadesVendidasPorCategoria` | `DetallePedidoRepository` | `JOIN`, `SUM` y `GROUP BY`: unidades vendidas por categoría |

```java
@Query("""
        SELECT c.nombre, SUM(d.cantidad)
        FROM DetallePedido d
        JOIN d.variante v
        JOIN v.productoBase p
        JOIN p.categoria c
        GROUP BY c.nombre
        ORDER BY SUM(d.cantidad) DESC
        """)
List<Object[]> unidadesVendidasPorCategoria();
```

Los parámetros con nombre (`:texto`, `:estado`) evitan la inyección SQL. Las tres consultas se prueban contra una base real en `ConsultasJpqlTest` (`@DataJpaTest`), porque los tests con mocks nunca ejecutan la consulta.

### Transacciones

Una transacción hace que un grupo de operaciones se guarde **completo o no se guarde**. Se activa con `@Transactional`.

| Operación | Qué incluye |
|---|---|
| Confirmar una compra | Crear el pedido y sus detalles, descontar el stock y vaciar el carrito |
| Cancelar un pedido (admin) | Cambiar el estado y devolver el stock de cada producto |
| Crear o actualizar una variante | Guardar la variante y sus atributos en varias tablas |
| Operaciones del carrito | Agregar, cambiar y quitar ítems |

Sin transacción, un fallo a la mitad dejaría datos inconsistentes, por ejemplo un pedido creado con el stock sin descontar. `PedidoTransaccionTest` fuerza un fallo al final de la compra y comprueba que todo vuelve a su estado original (rollback).

### Spring Security, roles y permisos

Autenticación es saber **quién eres** y autorización es saber **qué puedes hacer**. Las reglas están en `SecurityConfig`:

| Ruta | Quién puede entrar |
|---|---|
| `/api/auth/**` | Cualquiera (registro y login) |
| `GET /api/productos/**` y `GET /api/categorias/**` | Cualquiera |
| `/api/admin/pedidos/**` y `/api/admin/stock/**` | ADMIN y VENDEDOR |
| Resto de `/api/admin/**` | Solo el rol ADMIN |
| Cualquier otra ruta | Cualquier usuario con token válido |

`AdminPedidoController` usa además `@PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")` como segundo candado, activado con `@EnableMethodSecurity`. Las respuestas de seguridad son:

- **401 Unauthorized:** no hay token o el token no es válido (no te identificaste).
- **403 Forbidden:** el token es válido, pero tu rol no tiene permiso.

Otros detalles: las contraseñas se guardan con BCrypt, la sesión es sin estado (`STATELESS`) y la ruta interna `/error` está permitida para que Spring no reemplace un 403 por un 401 al armar el mensaje de error.

(La tabla completa de permisos por rol está en [Roles y permisos](#roles-y-permisos).)

### Autenticación con JWT

Un JWT es un texto firmado, con tres partes (cabecera, contenido y firma), que contiene el email del usuario y su rol. Flujo completo:

1. El usuario hace `POST /api/auth/login` con su email y contraseña.
2. `AuthService` valida la contraseña y `JwtService` genera el token firmado.
3. En cada petición posterior, el cliente envía `Authorization: Bearer <token>`.
4. `JwtAuthFilter` lee el token, comprueba la firma y la vigencia, e identifica al usuario y su rol.
5. `SecurityConfig` decide si ese rol puede acceder a la ruta pedida.

La clave de firma **no está en el código**: se lee de `jwt.secret` (variable `JWT_SECRET`). Si se cambia, los tokens emitidos antes dejan de ser válidos y hay que volver a iniciar sesión.

---

## Mejoras del back-end (pasos 1 a 13)

Después de las Unidades 1 y 2 se hizo una etapa de mejora en 13 pasos, uno a la vez: cambio, pruebas en verde, verificación en Swagger o Thunder Client y commit. Resumen:

| # | Paso | Resultado |
|---|---|---|
| 1 | CORS, PATCH e `imagenUrl` | El frontend Angular puede llamar a la API; los productos tienen imagen |
| 2 | Pruebas faltantes | Se cubrieron los huecos de la entrega inicial |
| 3 | Errores globales | Excepciones con nombre, `GlobalExceptionHandler` y JSON uniforme; sin `try/catch` en controladores |
| 4 | Paginación y orden | `PaginaDTO` y parámetros `pagina`, `tamano`, `orden`, `direccion` |
| 5 | Swagger | Documentación interactiva de toda la API |
| 6 | Entidad Marca | La marca deja de ser texto libre |
| 7 | Auditoría y borrado lógico | Fechas de creación/modificación; borrar no pierde historial |
| 8 | Filtros y extras | Filtros combinables, sugerencias, relacionados, categorías en árbol |
| 9 | Reportes y dashboard | Reportes sin pedidos cancelados y `GET /api/admin/dashboard` |
| 10 | Flujo de pedidos | Estados con transiciones válidas y cancelación por el cliente |
| 11 | Historial de stock | `movimiento_stock` y ajustes manuales con motivo |
| 12 | Rol VENDEDOR | Tercer rol y pruebas 401/403 en todas las rutas admin |
| 13 | Sesión segura | Bloqueo por intentos, cambio de contraseña, admin por variables de entorno, pruebas de JWT |

**Fuera de alcance** (decidido desde el inicio): Docker, Flyway, SUNAT, pasarela de pago, correo, PDF y stock por sucursal.

---

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Connection refused` o `password authentication failed` al arrancar | PostgreSQL apagado, base inexistente o credenciales incorrectas. Revisa `DB_URL`, `DB_USER` y `DB_PASSWORD` y que la base exista. |
| `Port 8080 was already in use` | Otro programa o una copia de la API usa el puerto. Detenla o define `SERVER_PORT`. |
| `class file version 69.0 ... only recognizes up to 65.0` | Se compiló con un JDK más nuevo que el que usa Maven. Usa JDK 21 y ejecuta `mvnw clean test`. |
| **401** con un token que antes funcionaba | El token venció o se cambió `JWT_SECRET`. Haz login otra vez. |
| **403** en una ruta `/api/admin/**` | Tu usuario es cliente. Usa el token del administrador. |
| **401** solo en algunas pestañas de Thunder | Thunder guarda los headers por pestaña. Actualiza el token en cada una. |
| **400** al cambiar el estado de un pedido | Dejaste el texto de ejemplo en la URL. Pon el UUID real, sin `<` ni `>`. |
| **409 "Ya existe una variante con ese SKU"** | El SKU es único. Usa otro. |
| El registro dice que el email ya existe | Esa cuenta ya fue creada. Usa `POST /api/auth/login`. |
| Productos duplicados en el catálogo | El nombre puede repetirse; solo el SKU es único. Bórralos con `DELETE /api/admin/productos/{id}`. |
| **423 Locked** al iniciar sesión | La cuenta se bloqueó por 5 contraseñas incorrectas seguidas. Espera el tiempo que indica el mensaje (3 minutos). |
| **400** al cambiar el estado de un pedido a uno no permitido | El flujo es ordenado (PENDIENTE → CONFIRMADO → EN_PREPARACION → ENVIADO → ENTREGADO). La respuesta indica los estados permitidos. |
| `violates check constraint` en `pedido.estado` o `usuario.rol` | PostgreSQL conserva el CHECK viejo de la columna: `ddl-auto=update` no lo actualiza. Recrea la base de datos (ver [Base de una versión anterior](#base-de-una-versión-anterior)). |
| El admin no tiene la contraseña que esperaba | Si ya existía, `DataInitializer` no lo toca. `ADMIN_EMAIL`/`ADMIN_PASSWORD` solo se usan al **crear** el admin. |