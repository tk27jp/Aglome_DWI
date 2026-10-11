# Aglome-tienda · API REST con Spring Boot

Back-end de la plataforma web de **Aglome Perú**, una empresa de Chimbote (Perú) dedicada a la venta de madera, melamina y productos de ferretería.
Proyecto académico del curso de **Back-end con Spring Boot**, que cubre dos unidades de aprendizaje:

| Unidad | Tema | Qué se implementó |
|---|---|---|
| **1** | API REST con TDD | Controladores, servicios, inyección de dependencias, métodos HTTP y pruebas automáticas |
| **2** | Back-end con bases de datos | JPA e Hibernate sobre PostgreSQL, JPQL, transacciones, Spring Security, roles y JWT |

---

## Contexto del proyecto

**El problema.** Aglome no tiene catálogo web ni una base de datos digital accesible: su inventario interno no está conectado a internet y la atención es 100 % por llamadas o visitas. Los clientes deben llamar para consultar disponibilidad, precios, colores y medidas, y pueden perder hasta medio día buscando el producto correcto. El personal (administradora y ejecutiva de ventas) se sobrecarga con llamadas repetitivas y no puede dedicarse a tareas de mayor valor.

**La solución.** Una plataforma web formada por una **API REST** (este repositorio) y un **frontend en Angular** (siguiente etapa del curso) que servirá de vitrina digital: catálogo con fotos, precios y medidas, filtros por categoría, color y medida, botón de contacto por WhatsApp y mapa de ubicación.

**Qué hace este back-end.** Guarda el catálogo (productos con variantes de color y medida, SKU, precio y stock) en PostgreSQL y lo entrega por una API REST. Además incluye carrito, pedidos y un panel administrativo (CRUD) protegido por roles y JWT.

---

## Tabla de contenido

1. [Estructura del proyecto](#estructura-del-proyecto)
2. [Tecnologías](#tecnologías)
3. [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
4. [Probar en el navegador (y por qué `/` sale en blanco)](#probar-en-el-navegador-y-por-qué--sale-en-blanco)
5. [Pruebas automáticas](#pruebas-automáticas)
6. [Demostración paso a paso con Thunder Client](#demostración-paso-a-paso-con-thunder-client)
7. [Qué se guarda y cómo verlo en la base de datos](#qué-se-guarda-y-cómo-verlo-en-la-base-de-datos)
8. [Lista de endpoints](#lista-de-endpoints)
9. [Unidad 1: resumen](#unidad-1-resumen)
10. [Unidad 2: explicación detallada](#unidad-2-explicación-detallada)
11. [Problemas frecuentes](#problemas-frecuentes)

---

## Estructura del proyecto

Así se ve el proyecto en el Explorer de VS Code. La carpeta que se abre en VS Code es la raíz del repositorio, `Aglome-tienda`, y el proyecto Maven está dentro de la carpeta `aglome-tienda`.

```
Aglome-tienda/                                 ← raíz del repositorio
├── README.md
├── .vscode/
└── aglome-tienda/                             ← proyecto Maven (aquí se ejecutan los comandos)
    ├── .mvn/wrapper/                          Maven Wrapper
    ├── .vscode/settings.json                  Configuración de VS Code para Java
    ├── .gitattributes
    ├── .gitignore
    ├── HELP.md
    ├── mvnw                                   Ejecutor de Maven (Linux y macOS)
    ├── mvnw.cmd                               Ejecutor de Maven (Windows)
    ├── pom.xml                                Dependencias y configuración de Maven
    ├── target/                                Se genera al compilar
    └── src/
        ├── main/
        │   ├── java/pe/edu/utp/aglome_tienda/
        │   │   ├── AglomeTiendaApplication.java        Clase que arranca Spring Boot
        │   │   │
        │   │   ├── config/
        │   │   │   └── DataInitializer.java            Crea el administrador al arrancar
        │   │   │
        │   │   ├── controller/                         Reciben las peticiones HTTP
        │   │   │   ├── AuthController.java             Registro y login
        │   │   │   ├── PerfilController.java           Datos del usuario autenticado
        │   │   │   ├── CategoriaController.java        Categorías (público)
        │   │   │   ├── ProductoController.java         Catálogo y búsqueda (público)
        │   │   │   ├── CarritoController.java          Carrito del cliente
        │   │   │   ├── PedidoController.java           Pedidos del cliente
        │   │   │   ├── AdminCategoriaController.java   CRUD de categorías (ADMIN)
        │   │   │   ├── AdminProductoController.java    CRUD de productos (ADMIN)
        │   │   │   ├── AdminVarianteController.java    CRUD de variantes (ADMIN)
        │   │   │   ├── AdminPedidoController.java      Ver pedidos y cambiar su estado (ADMIN)
        │   │   │   └── AdminReporteController.java     Reporte de ventas (ADMIN)
        │   │   │
        │   │   ├── service/                            Lógica de negocio y transacciones
        │   │   │   ├── AuthService.java
        │   │   │   ├── CategoriaService.java
        │   │   │   ├── ProductoBaseService.java
        │   │   │   ├── VarianteService.java
        │   │   │   ├── CarritoService.java
        │   │   │   ├── PedidoService.java
        │   │   │   └── ReporteService.java
        │   │   │
        │   │   ├── repository/                         Acceso a la base de datos (Spring Data JPA)
        │   │   │   ├── UsuarioRepository.java
        │   │   │   ├── CategoriaRepository.java
        │   │   │   ├── ProductoBaseRepository.java     Consulta JPQL: búsqueda por nombre
        │   │   │   ├── VarianteRepository.java
        │   │   │   ├── TipoAtributoRepository.java
        │   │   │   ├── VarianteAtributoRepository.java
        │   │   │   ├── CarritoRepository.java
        │   │   │   ├── CarritoItemRepository.java
        │   │   │   ├── PedidoRepository.java           Consulta JPQL: pedidos por usuario y estado
        │   │   │   └── DetallePedidoRepository.java    Consulta JPQL: ventas por categoría
        │   │   │
        │   │   ├── model/                              Entidades JPA (cada una es una tabla)
        │   │   │   ├── Usuario.java    Rol.java
        │   │   │   ├── Categoria.java
        │   │   │   ├── ProductoBase.java
        │   │   │   ├── Variante.java   TipoAtributo.java
        │   │   │   ├── VarianteAtributo.java   VarianteAtributoId.java
        │   │   │   ├── Carrito.java    CarritoItem.java
        │   │   │   └── Pedido.java     EstadoPedido.java    DetallePedido.java
        │   │   │
        │   │   ├── dto/                                Objetos de entrada y salida de la API
        │   │   │   ├── LoginRequest, RegistroRequest, AuthResponse, PerfilDTO
        │   │   │   ├── CategoriaRequest, CategoriaDTO
        │   │   │   ├── ProductoBaseRequest, ProductoBaseDTO
        │   │   │   ├── VarianteRequest, VarianteDTO
        │   │   │   ├── AgregarAlCarritoRequest, CarritoDTO, CarritoItemDTO
        │   │   │   ├── PedidoDTO, DetallePedidoDTO
        │   │   │   └── VentaCategoriaDTO
        │   │   │
        │   │   └── security/                           Seguridad y JWT
        │   │       ├── SecurityConfig.java             Reglas de acceso por ruta y por rol
        │   │       ├── JwtService.java                 Crea y valida los tokens
        │   │       ├── JwtAuthFilter.java              Lee el token en cada petición
        │   │       └── CustomUserDetailsService.java   Busca al usuario en la base de datos
        │   │
        │   └── resources/
        │       └── application.properties              Configuración (variables de entorno)
        │
        └── test/
            ├── java/pe/edu/utp/aglome_tienda/
            │   ├── AglomeTiendaApplicationTests.java   La aplicación arranca (1 test)
            │   ├── model/          5 clases · 8 tests    Entidades
            │   ├── service/        8 clases · 22 tests   Lógica con Mockito, y PedidoTransaccionTest
            │   ├── repository/     ConsultasJpqlTest · 3 tests   Consultas JPQL contra una base real
            │   └── controller/     6 clases · 15 tests   Rutas, validaciones y seguridad (401, 403, 200)
            └── resources/
                └── application.properties              Configuración de pruebas (H2 en memoria)
```

### Cómo viaja una petición

```
Cliente (HTTP) → Controller (@RestController) → Service (@Service) → Repository (JpaRepository) → Entity (@Entity) → PostgreSQL
```

Cada capa tiene una sola responsabilidad: el controlador recibe y responde, el servicio aplica las reglas del negocio, el repositorio habla con la base de datos y la entidad representa una tabla. Ejemplo: al confirmar una compra, `PedidoController` recibe la petición, `PedidoService` valida el carrito y descuenta el stock, y los repositorios guardan el pedido.

### Modelo de datos (10 tablas)

| Tabla | Qué guarda |
|---|---|
| `usuario` | Cuentas: email único, contraseña con BCrypt y rol |
| `categoria` | Categorías, con subcategorías opcionales (`padre_id`) |
| `producto_base` | Producto general: nombre, descripción, marca, publicado y categoría |
| `variante` | Lo que se vende: SKU único, precio, stock y estado |
| `tipo_atributo` | Nombres de atributos (Color, Medida) |
| `variante_atributo` | Valor de cada atributo por variante (clave compuesta) |
| `carrito` | Un carrito por usuario |
| `carrito_item` | Ítems del carrito: variante y cantidad |
| `pedido` | Pedido: fecha, total, estado y usuario |
| `detalle_pedido` | Líneas del pedido: cantidad, precio unitario y subtotal |

Relaciones: una categoría tiene muchos productos, un producto tiene muchas variantes, un usuario tiene un carrito y muchos pedidos, y un pedido tiene muchos detalles. Estados de un pedido: `PENDIENTE`, `CONFIRMADO` y `CANCELADO`.

---

## Tecnologías

| Herramienta | Para qué se usa |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 4.1.1 | Framework: Web MVC, Data JPA, Security y Validation |
| Hibernate 7 | Convierte las clases Java en tablas |
| PostgreSQL | Base de datos de la aplicación |
| H2 en memoria | Base temporal solo para las pruebas automáticas |
| jjwt | Creación y validación de tokens JWT |
| Lombok | Evita escribir getters, setters y constructores |
| JUnit 5, Mockito y Spring Security Test | Pruebas automáticas |
| Maven (`mvnw`) | Compila, ejecuta y prueba (no hace falta instalarlo) |
| Thunder Client | Pruebas manuales de la API |
| pgAdmin | Ver las tablas y los datos |

---

## Cómo ejecutar el proyecto

### 1. Requisitos

- **JDK 21** instalado. Comprueba con `java -version`.
- **PostgreSQL** instalado y encendido (puerto 5432 por defecto).
- **VS Code** con las extensiones de Java y de Spring Boot, y la extensión **Thunder Client**.

### 2. Clonar el repositorio

```bash
git clone <URL-DE-ESTE-REPOSITORIO>
cd Aglome-tienda/aglome-tienda
```

Todos los comandos de esta guía se ejecutan dentro de `aglome-tienda`, la carpeta que contiene el `pom.xml`.

### 3. Crear tu base de datos

Cada persona usa **su propia** base local. En pgAdmin (clic derecho en *Databases › Create › Database*) o con `psql`:

```sql
CREATE DATABASE aglome_spring_db;
```

No hay que crear tablas: Hibernate las crea al arrancar (`ddl-auto=update`).

### 4. Configurar tu base de datos y tu puerto

La aplicación lee su configuración de **variables de entorno**. Si no defines ninguna, usa estos valores por defecto:

| Variable | Qué es | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de tu base | `jdbc:postgresql://localhost:5432/aglome_spring_db` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |
| `SERVER_PORT` | Puerto donde corre la API | `8080` |
| `JWT_SECRET` | Clave que firma los tokens (mínimo 32 caracteres) | clave de desarrollo |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `36000000` (10 horas) |

Si tu PostgreSQL tiene otra contraseña, otro nombre de base o otro puerto, define las variables **antes** de arrancar, en la misma terminal.

**Windows (PowerShell):**

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/mi_base"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "mi_clave"
$env:SERVER_PORT = "9090"
```

**Linux / macOS:**

```bash
export DB_URL="jdbc:postgresql://localhost:5432/mi_base"
export DB_USER="postgres"
export DB_PASSWORD="mi_clave"
export SERVER_PORT="9090"
```

Las variables duran mientras esa terminal esté abierta. Si cambias `SERVER_PORT`, reemplaza `8080` en todas las URL de esta guía.

> **Importante:** el valor por defecto de `JWT_SECRET` es solo para desarrollo. Si publicas la aplicación, define tu propio secreto.

### 5. Arrancar la API

Hay tres formas.

**Desde la terminal con Maven** (la que usamos en el proyecto):

```powershell
.\mvnw.cmd clean spring-boot:run
```

En Linux o macOS: `chmod +x mvnw` y luego `./mvnw clean spring-boot:run`. `clean` recompila todo; sin `clean` el arranque es más rápido.

**Desde VS Code:** abre `AglomeTiendaApplication.java` y pulsa el botón **Run** que aparece sobre el método `main`, o usa el panel *Spring Boot Dashboard*. Define las variables de entorno antes de abrir VS Code.

**Como archivo jar:**

```powershell
.\mvnw.cmd clean package "-DskipTests"
java -jar target\aglome-tienda-0.0.1-SNAPSHOT.jar
```

**Cómo saber que arrancó bien:** la consola muestra al final `Tomcat started on port 8080` y `Started AglomeTiendaApplication in ... seconds`. Para detenerla se pulsa `Ctrl + C`, y cada vez que cambies el código hay que detenerla y arrancarla de nuevo. Si aparece `Port 8080 was already in use`, ya hay otra copia corriendo: detenla o usa otro `SERVER_PORT`.

### 6. Administrador automático

Al arrancar, `DataInitializer` **crea un administrador si no existe**, para poder entrar al panel sin tocar la base de datos:

| Email | Contraseña | Rol |
|---|---|---|
| `admin@aglome.com` | `Admin123` | `ADMIN` |

Cualquier persona que se registre por la API recibe el rol `CLIENTE`. Un cliente nunca puede crearse como admin. Son credenciales de demostración: cámbialas en `DataInitializer` si vas a publicar la aplicación.

---

## Probar en el navegador (y por qué `/` sale en blanco)

Al abrir `http://localhost:8080/` el navegador muestra una pantalla en blanco, y **es lo esperado**. Este proyecto es solo una API REST que responde JSON: no tiene página web ni dashboard, y no existen las carpetas `static/` ni `templates/`. Además, la ruta raíz no es pública, así que Spring Security responde 401 sin contenido. El frontend en Angular consumirá esta API en la siguiente etapa.

Lo que sí se puede abrir en el navegador son las rutas **GET públicas**, que no necesitan token:

| URL | Qué muestra |
|---|---|
| `http://localhost:8080/api/productos` | Catálogo de productos publicados con sus variantes |
| `http://localhost:8080/api/categorias` | Lista de categorías |
| `http://localhost:8080/api/productos/buscar?texto=melamina` | Búsqueda por nombre (consulta JPQL) |

Si aparece JSON, la API está viva y conectada a la base. Si aparece `[]`, funciona pero aún no hay datos. Si el navegador dice que no puede acceder al sitio, la aplicación no está corriendo o usa otro puerto.

Todo lo demás (carrito, pedidos, `/api/admin/**` y cualquier POST, PUT, PATCH o DELETE) no se puede probar desde la barra de direcciones, porque el navegador no envía el header `Authorization`. Para eso se usa **Thunder Client**.

---

## Pruebas automáticas

```powershell
.\mvnw.cmd clean test
```

Resultado esperado: **49 tests, 0 fallos, `BUILD SUCCESS`**. Usan **H2 en memoria** (`src/test/resources/application.properties`), así que **no tocan tu PostgreSQL**.

| Grupo | Tests | Qué comprueban |
|---|---|---|
| Arranque | 1 | La aplicación completa arranca |
| Modelo | 8 | Comportamiento de las entidades |
| Servicio | 22 | Lógica de negocio con Mockito, y `PedidoTransaccionTest` (compra y rollback con base real H2) |
| JPQL | 3 | `ConsultasJpqlTest`: las 3 consultas contra una base real |
| Controlador | 15 | Rutas, validaciones y seguridad: 401, 403 y 200 |

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
  "marca": "Aglome",
  "categoriaId": ID_CATEGORIA,
  "publicado": true
}
```
Esperado: **201** con el `id` (UUID) del producto. *Se guarda una fila en `producto_base`.*

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
Esperado: **201** con el `id` de la variante. *Se guarda en `variante`, `tipo_atributo` y `variante_atributo`, en una sola transacción.* Si repites el SKU: **400 "Ya existe una variante con ese SKU"**.

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
Esperado: **200** y `"estado": "CANCELADO"`. Repite D11: el stock vuelve a **100**, porque la cancelación devuelve el stock dentro de una transacción. Si repites el PATCH, responde **400 "El pedido ya está cancelado"**.

### Seguridad

| Petición | Token | Resultado | Por qué |
|---|---|---|---|
| `GET /api/carrito` | ninguno | **401** | No se identificó |
| `GET /api/admin/pedidos` | cliente | **403** | Está identificado, pero su rol no es ADMIN |
| `GET /api/admin/pedidos` | admin | **200** | Tiene el rol requerido |

### Ensayar y presentar

- El **SKU** y el **email** son únicos y repetirlos da error 400. Si ensayas con `BISAG-ACE-3PUL` y `demo1@test.com`, para presentar usa otros (`BISAG-ACE-3PUL-B`, `demo2@test.com`) o borra antes el producto del ensayo con `DELETE /api/admin/productos/{id}` (solo es posible si no tiene pedidos).
- Para una demostración corta en vivo bastan D2, D3, D4, D6, D9, D10, D11 y D12. Los logins se dejan hechos antes.
- La **marca** es un campo de texto libre: usa siempre el mismo valor, con las mismas mayúsculas.

---

## Qué se guarda y cómo verlo en la base de datos

Con PostgreSQL, las cuentas y todos los datos creados **se conservan al apagar la aplicación**. Lo único que no se guarda en la base son los **tokens**: el servidor solo comprueba su firma, y quien los guarda es Thunder Client.

| Qué | Dónde queda | ¿Se pierde al reiniciar? |
|---|---|---|
| Cuentas (admin y clientes) | Tabla `usuario` | No |
| Categorías, productos y variantes | PostgreSQL | No |
| Carritos y pedidos | PostgreSQL | No |
| Token JWT | Solo en Thunder Client | Deja de servir a las 10 horas o si cambia `JWT_SECRET` |

No hace falta registrar de nuevo al admin ni a un cliente: si un token deja de servir, se hace `POST /api/auth/login` con el mismo email y contraseña. (Con una base en memoria, como H2, todo se borraba al reiniciar; por eso H2 queda solo para las pruebas.)

### Ver las tablas en pgAdmin

1. Abre pgAdmin: *Servers › PostgreSQL › Databases › aglome_spring_db › Schemas › public › Tables*.
2. Clic derecho sobre una tabla › *View/Edit Data › All Rows*. O abre el *Query Tool* y ejecuta:

```sql
SELECT id, nombre, email, rol FROM usuario;
SELECT id, nombre FROM categoria;
SELECT id, nombre, publicado FROM producto_base;
SELECT sku, precio_venta, stock FROM variante;
SELECT id, estado, total, fecha FROM pedido;

SELECT c.nombre, SUM(d.cantidad)
FROM detalle_pedido d
JOIN variante v ON v.id = d.variante_id
JOIN producto_base p ON p.id = v.producto_base_id
JOIN categoria c ON c.id = p.categoria_id
GROUP BY c.nombre;
```

La última consulta es la versión SQL del reporte de ventas y debe coincidir con `GET /api/admin/reportes/ventas-por-categoria`.

| Petición | Tablas que cambian |
|---|---|
| `POST /api/auth/registro` | `usuario` |
| `POST /api/admin/categorias` | `categoria` |
| `POST /api/admin/productos` | `producto_base` |
| `POST /api/admin/variantes` | `variante`, `tipo_atributo`, `variante_atributo` |
| `POST /api/carrito/items` | `carrito`, `carrito_item` |
| `POST /api/pedidos/confirmar` | `pedido`, `detalle_pedido`, `variante` (baja el stock) y `carrito_item` (se vacía) |
| `PATCH /api/admin/pedidos/{id}/estado` | `pedido` (estado) y `variante` (sube el stock si se cancela) |

### Agregar más datos con el admin existente

Como el admin ya está guardado, no se crea otro: se hace login y se repiten D2 a D5 con otros datos. Para ver los ids existentes: `GET /api/categorias` y `GET /api/admin/productos`. Para una subcategoría, la categoría admite el campo opcional `padreId`, por ejemplo `{ "nombre": "Bisagras", "padreId": 2 }`.

### Crear otro administrador

La API **no permite** registrarse como administrador (el registro siempre crea clientes). Para tener otro admin:

- **Opción A, sin código:** registra la cuenta con `POST /api/auth/registro` y en pgAdmin ejecuta `UPDATE usuario SET rol = 'ADMIN' WHERE email = 'otro.admin@test.com';`. Luego esa persona inicia sesión de nuevo para que su token lleve el rol ADMIN.
- **Opción B, por código:** agrega en `DataInitializer` otro bloque con otro email y contraseña.

---

## Lista de endpoints

**Acceso público**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crea una cuenta de cliente y devuelve un token |
| POST | `/api/auth/login` | Inicia sesión y devuelve un token |
| GET | `/api/productos` | Lista los productos publicados |
| GET | `/api/productos/{id}` | Detalle de un producto |
| GET | `/api/productos/buscar?texto=` | Busca por nombre (JPQL) |
| GET | `/api/productos/categoria/{categoriaId}` | Productos de una categoría |
| GET | `/api/categorias` · `/api/categorias/{id}` | Categorías |

**Requieren token (cualquier usuario autenticado)**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/perfil` | Datos del usuario autenticado |
| GET | `/api/carrito` | Ver el carrito |
| POST | `/api/carrito/items` | Agregar una variante al carrito |
| PUT | `/api/carrito/items/{itemId}?cantidad=` | Cambiar la cantidad de un ítem |
| DELETE | `/api/carrito/items/{itemId}` | Quitar un ítem |
| POST | `/api/pedidos/confirmar` | Convierte el carrito en un pedido |
| GET | `/api/pedidos` | Mis pedidos (admite `?estado=`, JPQL) |
| GET | `/api/pedidos/{id}` | Detalle de uno de mis pedidos |

**Solo rol `ADMIN`**

| Método | Ruta | Descripción |
|---|---|---|
| POST · PUT · DELETE | `/api/admin/categorias` · `/{id}` | Gestión de categorías |
| GET · POST · PUT · DELETE | `/api/admin/productos` · `/{id}` | Gestión de productos |
| POST · PUT · DELETE | `/api/admin/variantes` · `/{id}` | Gestión de variantes |
| GET | `/api/admin/pedidos` | Todos los pedidos |
| PATCH | `/api/admin/pedidos/{id}/estado?estado=` | Cambia el estado de un pedido |
| GET | `/api/admin/reportes/ventas-por-categoria` | Unidades vendidas por categoría (JPQL) |

---

## Unidad 1: resumen

La Unidad 1 dejó construida la base de la API: arquitectura en capas, 27 endpoints REST y pruebas automáticas.

| Tema | Qué se hizo |
|---|---|
| Estructura de Spring Boot | Proyecto Maven con los starters web, JPA, security y validation; configuración en `application.properties` |
| Controladores y endpoints | 11 controladores `@RestController` con 27 rutas |
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

JPQL consulta las **clases Java** y no las tablas. Se usa cuando el nombre del método ya no alcanza. Hay tres consultas con `@Query`:

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
| `/api/admin/**` | Solo el rol ADMIN |
| Cualquier otra ruta | Cualquier usuario con token válido |

`AdminPedidoController` usa además `@PreAuthorize("hasRole('ADMIN')")` como segundo candado, activado con `@EnableMethodSecurity`. Las respuestas de seguridad son:

- **401 Unauthorized:** no hay token o el token no es válido (no te identificaste).
- **403 Forbidden:** el token es válido, pero tu rol no tiene permiso.

Otros detalles: las contraseñas se guardan con BCrypt, la sesión es sin estado (`STATELESS`) y la ruta interna `/error` está permitida para que Spring no reemplace un 403 por un 401 al armar el mensaje de error.

| Acción | Sin token | Cliente | Administrador |
|---|---|---|---|
| Ver catálogo y categorías | 200 | 200 | 200 |
| Registrarse e iniciar sesión | 200 | 200 | 200 |
| Usar carrito y pedidos | 401 | 200 | 200 |
| Cualquier ruta `/api/admin/**` | 401 | 403 | 200 |

### Autenticación con JWT

Un JWT es un texto firmado, con tres partes (cabecera, contenido y firma), que contiene el email del usuario y su rol. Flujo completo:

1. El usuario hace `POST /api/auth/login` con su email y contraseña.
2. `AuthService` valida la contraseña y `JwtService` genera el token firmado.
3. En cada petición posterior, el cliente envía `Authorization: Bearer <token>`.
4. `JwtAuthFilter` lee el token, comprueba la firma y la vigencia, e identifica al usuario y su rol.
5. `SecurityConfig` decide si ese rol puede acceder a la ruta pedida.

La clave de firma **no está en el código**: se lee de `jwt.secret` (variable `JWT_SECRET`). Si se cambia, los tokens emitidos antes dejan de ser válidos y hay que volver a iniciar sesión.

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
| **400 "Ya existe una variante con ese SKU"** | El SKU es único. Usa otro. |
| El registro dice que el email ya existe | Esa cuenta ya fue creada. Usa `POST /api/auth/login`. |
| Productos duplicados en el catálogo | El nombre puede repetirse; solo el SKU es único. Bórralos con `DELETE /api/admin/productos/{id}`. |
