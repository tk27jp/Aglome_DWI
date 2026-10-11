# Aglome-tienda · API REST con Spring Boot

API REST para una tienda en línea (catálogo de productos con variantes, carrito, pedidos y panel de administración).
Proyecto académico del curso de **Back-end con Spring Boot**, que cubre dos unidades de aprendizaje:

| Unidad | Tema | Qué se implementó |
|---|---|---|
| **1** | API REST con TDD | Controladores, servicios, inyección de dependencias, métodos HTTP y pruebas automatizadas |
| **2** | Back-end con bases de datos | JPA/Hibernate sobre PostgreSQL, JPQL, transacciones, Spring Security, roles y JWT |

---

## Tabla de contenido

1. [Tecnologías](#tecnologías)
2. [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
3. [Ejecutar las pruebas automáticas](#ejecutar-las-pruebas-automáticas)
4. [Guía de pruebas manuales con Thunder Client](#guía-de-pruebas-manuales-con-thunder-client)
5. [Lista de endpoints](#lista-de-endpoints)
6. [Unidad 1: API REST y TDD](#unidad-1-api-rest-y-tdd)
7. [Unidad 2: base de datos y seguridad](#unidad-2-base-de-datos-y-seguridad)
8. [Problemas frecuentes](#problemas-frecuentes)

---

## Tecnologías

- Java 21
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- PostgreSQL (aplicación) y H2 en memoria (pruebas automáticas)
- JWT con la librería `jjwt`
- Lombok
- JUnit 5, Mockito y Spring Security Test
- Maven (incluye el wrapper `mvnw`, no necesitas instalar Maven)

---

## Cómo ejecutar el proyecto

### 1. Requisitos

- **JDK 21** instalado. Comprueba con `java -version`.
- **PostgreSQL** instalado y encendido (puerto 5432 por defecto).
- Un cliente HTTP, por ejemplo la extensión **Thunder Client** de VS Code.

### 2. Clonar el repositorio

```bash
git clone <URL-DE-ESTE-REPOSITORIO>
cd Aglome-tienda/aglome-tienda
```

> El proyecto Maven está dentro de la carpeta `aglome-tienda`. Todos los comandos de esta guía se ejecutan desde ahí.

### 3. Crear tu base de datos

Cada persona usa **su propia** base de datos local. En pgAdmin (clic derecho en *Databases › Create › Database*) o con `psql`:

```sql
CREATE DATABASE aglome_spring_db;
```

No hace falta crear tablas: Hibernate las crea solas al arrancar (`ddl-auto=update`).

### 4. Configurar tu conexión (base de datos y puerto)

La aplicación lee su configuración de **variables de entorno**. Si no defines ninguna, usa estos valores por defecto:

| Variable | Qué es | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de tu base | `jdbc:postgresql://localhost:5432/aglome_spring_db` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |
| `SERVER_PORT` | Puerto donde corre la API | `8080` |
| `JWT_SECRET` | Clave que firma los tokens (mínimo 32 caracteres) | clave de desarrollo |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `36000000` (10 horas) |

Si tu PostgreSQL tiene otra contraseña, otro nombre de base o otro puerto, define las variables **antes** de arrancar.

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

Las variables solo duran mientras esa terminal esté abierta.

> **Importante:** el valor por defecto de `JWT_SECRET` es solo para desarrollo. Si publicas la aplicación, define tu propio secreto.

### 5. Arrancar la API

**Windows:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Cuando veas en la consola `Started AglomeTiendaApplication`, la API está lista en `http://localhost:8080` (o en el puerto que definiste en `SERVER_PORT`).

### 6. Usuario administrador automático

Al arrancar, la aplicación **crea un administrador si no existe**, para poder entrar al panel de administración sin tocar la base de datos:

| Email | Contraseña | Rol |
|---|---|---|
| `admin@aglome.com` | `Admin123` | `ADMIN` |

Cualquier persona que se registre por la API recibe el rol `CLIENTE`. Un cliente nunca puede crearse como admin.

---

## Ejecutar las pruebas automáticas

```powershell
.\mvnw.cmd clean test
```

(en Linux/macOS: `./mvnw clean test`). Resultado esperado: **49 tests, 0 fallos, `BUILD SUCCESS`**.

Las pruebas usan **H2 en memoria** (configurado en `src/test/resources/application.properties`), así que **no tocan tu PostgreSQL** ni necesitan que esté encendido.

---

## Guía de pruebas manuales con Thunder Client

Esta secuencia recorre toda la API y demuestra cada tema de las dos unidades. **Sigue el orden**: cada paso usa datos del anterior.

### Antes de empezar

1. Con la API corriendo, abre Thunder Client en VS Code.
2. Si cambiaste el puerto, reemplaza `8080` en todas las URL.
3. Cómo enviar el token: en la pestaña **Headers** agrega

   ```
   Authorization: Bearer <tu-token>
   ```

   Con un espacio después de `Bearer`, sin comillas y copiando solo el valor del campo `token`. No lo pongas también en la pestaña *Auth*, o se duplicará.
4. Los tokens son por usuario: usa el token de **admin** para las rutas de administración y el de **cliente** para el carrito y los pedidos.
5. Thunder Client guarda los *headers* **por pestaña**. Si cambias de token, actualízalo en cada pestaña.

### Paso 1. Login como administrador

```
POST http://localhost:8080/api/auth/login
```

```json
{ "email": "admin@aglome.com", "password": "Admin123" }
```

Respuesta esperada: **200** con `token`, `nombre`, `email` y `rol: "ADMIN"`. Copia el `token`.

### Paso 2. Crear una categoría (token de admin)

```
POST http://localhost:8080/api/admin/categorias
```

```json
{ "nombre": "Melaminas" }
```

Respuesta: **201** con el `id` de la categoría (por ejemplo `1`). Anótalo.

### Paso 3. Crear un producto (token de admin)

```
POST http://localhost:8080/api/admin/productos
```

```json
{
  "nombre": "Melamina Azul Marino",
  "descripcion": "Aporta elegancia ejecutiva",
  "marca": "Aglome",
  "categoriaId": 1,
  "publicado": true
}
```

Usa en `categoriaId` el id real que obtuviste. Respuesta: **201** con el `id` (UUID) del producto. Cópialo.

### Paso 4. Crear una variante (token de admin)

```
POST http://localhost:8080/api/admin/variantes
```

```json
{
  "productoBaseId": "UUID_DEL_PRODUCTO",
  "sku": "MELM-AZM-18MM",
  "precioVenta": 45.90,
  "stock": 30,
  "atributos": { "Color": "Azul Marino", "Medida": "2.14m x 2.50m" }
}
```

Respuesta: **201** con el `id` de la variante. Cópialo.
El `sku` es único: si repites la petición recibirás **400 "Ya existe una variante con ese SKU"**.

### Paso 5. Ver el catálogo (sin token)

```
GET http://localhost:8080/api/productos
```

Respuesta: **200** con el producto y su variante (stock 30).

### Paso 6. Registrar un cliente

```
POST http://localhost:8080/api/auth/registro
```

```json
{ "nombre": "Jonathan Navarro", "email": "jonathan@test.com", "password": "123456" }
```

Respuesta: **200** con el `token` del cliente. Cópialo (es distinto al del admin).
La contraseña debe tener al menos 6 caracteres. Si ese email ya existe, recibirás un error: en ese caso usa `POST /api/auth/login` para obtener un token nuevo.

### Paso 7. Comprar (token del cliente)

Agregar al carrito:

```
POST http://localhost:8080/api/carrito/items
```

```json
{ "varianteId": "UUID_DE_LA_VARIANTE", "cantidad": 2 }
```

Confirmar la compra (sin cuerpo):

```
POST http://localhost:8080/api/pedidos/confirmar
```

Respuesta: **200** con el pedido en estado `CONFIRMADO`.

### Paso 8. Comprobar el descuento de stock

```
GET http://localhost:8080/api/productos
```

El stock de la variante debe ser **28** (se compraron 2 de 30). Esto ocurre dentro de una transacción junto con la creación del pedido.

### Paso 9. Consultas JPQL

| Qué prueba | Petición | Token | Resultado esperado |
|---|---|---|---|
| Búsqueda por nombre (sin distinguir mayúsculas) | `GET /api/productos/buscar?texto=melamina` | ninguno | El producto publicado |
| Pedidos por estado | `GET /api/pedidos?estado=CONFIRMADO` | cliente | Su pedido |
| Pedidos por estado (sin coincidencias) | `GET /api/pedidos?estado=CANCELADO` | cliente | Lista vacía `[]` |
| Reporte de ventas por categoría | `GET /api/admin/reportes/ventas-por-categoria` | admin | `Melaminas` con las unidades vendidas |

### Paso 10. Seguridad: 401 y 403

| Petición | Token | Resultado | Por qué |
|---|---|---|---|
| `GET /api/carrito` | ninguno | **401** | No te identificaste |
| `GET /api/admin/pedidos` | cliente | **403** | Te identificaste, pero tu rol no es ADMIN |
| `GET /api/admin/pedidos` | admin | **200** | Tienes el rol requerido |

### Paso 11. El admin cancela un pedido y el stock vuelve

1. Con el token de **admin**: `GET /api/admin/pedidos` y copia el `id` de un pedido `CONFIRMADO`.
2. Cancelarlo:

   ```
   PATCH http://localhost:8080/api/admin/pedidos/ID_DEL_PEDIDO/estado?estado=CANCELADO
   ```

   Respuesta: **200** con el pedido en estado `CANCELADO`.
3. `GET /api/productos`: el stock vuelve a **30**.
4. Repite el `PATCH` sobre el mismo pedido: **400 "El pedido ya está cancelado"**.

Reemplaza `ID_DEL_PEDIDO` por el UUID real, **sin** `<` ni `>`.

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
| PUT | `/api/carrito/items/{itemId}` | Cambiar la cantidad de un ítem |
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

Estados de un pedido: `PENDIENTE`, `CONFIRMADO`, `CANCELADO`.

---

## Unidad 1: API REST y TDD

### Estructura del proyecto

```
src/main/java/pe/edu/utp/aglome_tienda/
├── controller/   Reciben las peticiones HTTP y devuelven respuestas
├── service/      Lógica de negocio
├── repository/   Acceso a la base de datos (interfaces de Spring Data)
├── model/        Entidades JPA (tablas)
├── dto/          Objetos que viajan en las peticiones y respuestas
└── security/     Configuración de Spring Security y JWT
```

El flujo de una petición es: **controller → service → repository → base de datos**, y la respuesta vuelve por el mismo camino.

### Conceptos aplicados

- **Controladores y endpoints:** cada clase `@RestController` agrupa rutas con `@RequestMapping` y métodos con `@GetMapping`, `@PostMapping`, etc.
- **Inyección de dependencias:** las clases reciben sus dependencias por constructor (`@RequiredArgsConstructor` de Lombok). Spring crea los objetos y los entrega, sin usar `new`.
- **Validación:** los DTO de entrada usan `@NotBlank`, `@Email`, `@Min`, etc. y los controladores `@Valid`. Si los datos no son válidos, la API responde 400.
- **Métodos HTTP y su uso en la práctica:**

| Método | Uso | Ejemplo en el proyecto |
|---|---|---|
| GET | Leer datos | `GET /api/productos` |
| POST | Crear | `POST /api/admin/productos` |
| PUT | Reemplazar un recurso completo | `PUT /api/admin/productos/{id}` |
| PATCH | Modificar solo una parte | `PATCH /api/admin/pedidos/{id}/estado` |
| DELETE | Eliminar | `DELETE /api/carrito/items/{itemId}` |

### TDD (desarrollo guiado por pruebas)

Hay pruebas en tres niveles, en `src/test/java`:

- **Modelo:** comprueban las entidades.
- **Servicio:** usan **Mockito** para simular los repositorios y probar la lógica de negocio de forma aislada.
- **Controlador:** usan `@WebMvcTest` y `MockMvc` para comprobar rutas, códigos HTTP, validaciones y seguridad, sin levantar toda la aplicación.

---

## Unidad 2: base de datos y seguridad

### JPA e Hibernate (operaciones CRUD)

Las entidades de `model/` (`Usuario`, `Categoria`, `ProductoBase`, `Variante`, `Pedido`, `DetallePedido`, `Carrito`...) se convierten en tablas de PostgreSQL con relaciones `@ManyToOne` y una clave compuesta (`VarianteAtributoId`). Los repositorios extienden `JpaRepository`, que ya trae crear, leer, actualizar y borrar. Con `ddl-auto=update`, Hibernate crea o ajusta las tablas al arrancar sin borrar datos.

### JPQL

JPQL escribe consultas sobre las **clases Java** (no sobre las tablas). Hay tres consultas con `@Query`:

| Consulta | Dónde está | Qué hace |
|---|---|---|
| `buscarPublicadosPorNombre` | `ProductoBaseRepository` | Busca productos publicados por nombre, sin distinguir mayúsculas |
| `buscarPorUsuarioYEstado` | `PedidoRepository` | Pedidos de un usuario filtrados por estado |
| `unidadesVendidasPorCategoria` | `DetallePedidoRepository` | `JOIN`, `SUM` y `GROUP BY`: unidades vendidas por categoría |

Se prueban contra una base real (H2) en `ConsultasJpqlTest`, con `@DataJpaTest`.

### Transacciones

`@Transactional` hace que un grupo de operaciones se guarde **completo o no se guarde**:

- **Confirmar una compra** (`PedidoService.confirmarCompra`): crea el pedido, descuenta el stock y vacía el carrito. Si algo falla, todo se deshace. `PedidoTransaccionTest` lo demuestra forzando un fallo al final.
- **Cancelar un pedido** (`PedidoService.cambiarEstado`): cambia el estado y devuelve el stock de cada producto.
- **Variantes y carrito:** `VarianteService` y `CarritoService` también son transaccionales.

### Spring Security, roles y permisos

Reglas definidas en `SecurityConfig`:

| Ruta | Acceso |
|---|---|
| `/api/auth/**` | Público |
| `GET /api/productos/**` y `GET /api/categorias/**` | Público |
| `/api/admin/**` | Solo rol `ADMIN` |
| Cualquier otra | Usuario autenticado |

Además, `AdminPedidoController` usa `@PreAuthorize("hasRole('ADMIN')")` como segunda protección a nivel de clase (activada con `@EnableMethodSecurity`).

Respuestas de seguridad:

- **401 Unauthorized:** no hay token o no es válido.
- **403 Forbidden:** el token es válido, pero el rol no tiene permiso.

La sesión es *stateless*: el servidor no guarda nada entre peticiones, todo viaja en el token. Las contraseñas se guardan con **BCrypt**.

### Autenticación con JWT

1. El usuario hace `POST /api/auth/login` con email y contraseña.
2. `AuthService` valida la contraseña y `JwtService` genera un token firmado con el email y el rol.
3. En cada petición, el cliente envía `Authorization: Bearer <token>`.
4. `JwtAuthFilter` lee el token, comprueba su firma y su vigencia y, si es válido, identifica al usuario y su rol para Spring Security.
5. `SecurityConfig` decide si ese rol puede acceder a la ruta.

La clave de firma (`JWT_SECRET`) **no está en el código**: se lee de la configuración. Si se cambia, los tokens emitidos antes dejan de ser válidos y hay que volver a iniciar sesión.

---

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Connection refused` o `password authentication failed` al arrancar | PostgreSQL apagado, base inexistente o credenciales incorrectas. Revisa `DB_URL`, `DB_USER`, `DB_PASSWORD` y que la base exista. |
| `Port 8080 was already in use` | Otro programa usa el puerto. Define `SERVER_PORT` con otro valor. |
| `class file version 69.0 ... only recognizes up to 65.0` | Se compiló con un JDK más nuevo que el que usa Maven. Usa JDK 21 y ejecuta `mvnw clean test`. |
| **401** con un token que antes funcionaba | El token venció o se cambió `JWT_SECRET`. Haz login otra vez y usa el token nuevo. |
| **403** en una ruta `/api/admin/**` | Tu usuario es cliente. Usa el token del administrador. |
| **401** en todas las pestañas tras cambiar de token | Thunder Client guarda los headers por pestaña. Actualiza el token en cada una. |
| **400** al usar `/api/admin/pedidos/<id>/estado` | Dejaste el texto de ejemplo en la URL. Pon el UUID real, sin `<` ni `>`. |
| **400 "Ya existe una variante con ese SKU"** | El `sku` es único. Usa otro. |
| El registro responde que el email ya existe | Esa cuenta ya fue creada. Usa `/api/auth/login`. |
