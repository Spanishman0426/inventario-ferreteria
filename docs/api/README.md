<!--
  Nombre: README.md
  Descripción: Guía de la API REST con ejemplos JSON de petición y respuesta del catálogo de productos, de
               la administración de marcas y categorías y de clientes y proveedores.
  Fecha de creación: 2026-10-20
  Última actualización: 2026-10-22
  Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
-->

# API REST — Sistema de Inventario de Ferretería

El contrato completo está en [`openapi.yaml`](openapi.yaml) (OpenAPI 3.1), generado por springdoc desde el
código. La prueba `ContratoOpenApiTests` falla si el código y este archivo dejan de coincidir; tras un cambio
intencional de la API se regenera y se revisa el diff:

```bash
cd backend
./mvnw test -Dtest=ContratoOpenApiTests -Dopenapi.actualizar=true     # Windows: mvnw.cmd ...
```

| Entorno | Especificación | Swagger UI |
| --- | --- | --- |
| dev (`https://localhost:8443`) | `/v3/api-docs`, `/v3/api-docs.yaml` | `/swagger-ui.html` |
| test | `/v3/api-docs`, `/v3/api-docs.yaml` | desactivado |
| prod | según `OPENAPI_HABILITADO` (activa por defecto) | desactivado |

## Convenciones

- **Autenticación:** `POST /api/v1/auth/login` devuelve `accessToken`; las demás rutas exigen
  `Authorization: Bearer <token>`. El token vence a los 30 minutos (RNF-SEG-006).
- **Idioma:** `Accept-Language: es` (por defecto) o `en` cambia los títulos y mensajes de error.
- **Paginación:** `page` (desde 0), `size` (1 a 100, por defecto 20) y `sort=campo,asc|desc` (repetible).
  La respuesta usa el formato `Pagina`: `contenido`, `pagina`, `tamano`, `totalElementos`, `totalPaginas`.
- **Errores:** siempre `application/problem+json` (RFC 9457) con `codigo` estable y `errores` por campo.

## Productos — `/api/v1/productos` (RF-PRO-001, RF-PRO-003)

| Método y ruta | Permiso | Respuestas |
| --- | --- | --- |
| `GET /api/v1/productos` | `CATALOGO_CONSULTAR` | 200, 400, 401, 403 |
| `GET /api/v1/productos/{id}` | `CATALOGO_CONSULTAR` | 200, 401, 403, 404 |
| `POST /api/v1/productos` | `CATALOGO_EDITAR` | 201 + `Location`, 400, 401, 403, 409, 422 |
| `PUT /api/v1/productos/{id}` | `CATALOGO_EDITAR` | 200, 400, 401, 403, 404, 409, 422 |
| `DELETE /api/v1/productos/{id}` | `CATALOGO_EDITAR` | 204, 401, 403, 404 |

`CATALOGO_CONSULTAR` lo tienen todos los roles; `CATALOGO_EDITAR`, administrador, gerente y jefe de inventario.

**DELETE es un borrado lógico:** el producto pasa a `INACTIVO` y se conserva, porque un elemento de catálogo en
uso no se elimina, solo se desactiva (RF-CFG-001). Sigue disponible en `GET`, se reactiva con `PUT` y
`"estado": "ACTIVO"`, y repetir el DELETE también responde 204.

**Costos según permiso:** `porcentajeIncremento` y `ultimoCostoBase` solo aparecen si el usuario tiene
`COSTOS_VER` (un vendedor no ve costos, matriz 2.2).

### Crear

```http
POST /api/v1/productos
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "nombre": "Martillo de uña 16 oz",
  "descripcion": "Mango de fibra de vidrio",
  "categoriaId": 1,
  "marcaId": 1,
  "unidadBase": "UND",
  "tarifaIvaId": 3,
  "porcentajeIncremento": 30.00,
  "control": "GARANTIA",
  "plazoGarantiaMeses": 12
}
```

| Campo | Regla |
| --- | --- |
| `nombre` | Obligatorio, 3 a 150 caracteres; único por marca sin distinguir mayúsculas (409). |
| `descripcion` | Opcional, hasta 500 caracteres. |
| `categoriaId`, `marcaId`, `tarifaIvaId` | Obligatorios; deben existir y estar activos (400 en el campo). |
| `unidadBase` | Obligatorio; código de `GET /api/v1/unidades-medida`. |
| `porcentajeIncremento` | Obligatorio, 0 a 500, hasta 2 decimales (RN-20). |
| `control` | `NINGUNO`, `VENCIMIENTO` o `GARANTIA` (RN-18). |
| `plazoGarantiaMeses` | Obligatorio si y solo si `control` es `GARANTIA`. |
| `estado` | Opcional: `ACTIVO` (por defecto al crear) o `INACTIVO`. |

Respuesta `201 Created` con `Location: https://localhost:8443/api/v1/productos/10`:

```json
{
  "id": 10,
  "nombre": "Martillo de uña 16 oz",
  "descripcion": "Mango de fibra de vidrio",
  "estado": "ACTIVO",
  "categoria": { "id": 1, "nombre": "Herramientas manuales" },
  "marca": { "id": 1, "nombre": "Stanley" },
  "unidadBase": { "codigo": "UND", "nombre": "Unidad", "esDiscreta": true },
  "tarifaIva": { "id": 3, "nombre": "Tarifa general", "porcentaje": 19.00 },
  "porcentajeIncremento": 30.00,
  "control": "GARANTIA",
  "plazoGarantiaMeses": 12,
  "creadoEn": "2026-10-20T20:30:00Z"
}
```

`ultimoCostoBase` aparece después de la primera recepción (RF-COM-002) para quien tiene `COSTOS_VER`.

### Buscar

```http
GET /api/v1/productos?q=martillo&categoriaId=1&estado=ACTIVO&page=0&size=20&sort=nombre,asc
Authorization: Bearer eyJ...
```

`q` busca dentro del nombre sin distinguir mayúsculas (`%` y `_` se toman literalmente). `sort` admite `id`,
`nombre`, `creadoEn`, `marca` y `categoria`. Respuesta `200 OK` (vista de un vendedor, sin costos):

```json
{
  "contenido": [
    {
      "id": 1,
      "nombre": "Martillo de uña 16 oz",
      "descripcion": null,
      "estado": "ACTIVO",
      "categoria": { "id": 1, "nombre": "Herramientas" },
      "marca": { "id": 1, "nombre": "Stanley" },
      "unidadBase": { "codigo": "UND", "nombre": "Unidad", "esDiscreta": true },
      "tarifaIva": { "id": 3, "nombre": "Tarifa general", "porcentaje": 19.00 },
      "control": "GARANTIA",
      "plazoGarantiaMeses": 6,
      "creadoEn": "2026-10-20T20:30:00Z"
    }
  ],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 1,
  "totalPaginas": 1
}
```

### Editar

`PUT /api/v1/productos/{id}` recibe el mismo cuerpo que `POST` y reemplaza todos los campos (si `estado` se
omite, no cambia). Responde `200 OK` con el producto. La unidad base no puede cambiar si el producto ya tiene
presentaciones o lotes (RN-05), ni el control si tiene lotes (RN-18): ambos casos responden 409
`OPERACION_NO_PERMITIDA`.

### Inactivar (borrado lógico)

```http
DELETE /api/v1/productos/10
Authorization: Bearer eyJ...
```

Respuesta `204 No Content`, sin cuerpo.

## Marcas y categorías — `/api/v1/marcas` y `/api/v1/categorias` (RF-CFG-001)

Las dos rutas tienen las mismas operaciones. No hay DELETE (405): un elemento en uso no se elimina, solo se
desactiva; los productos que ya lo tienen lo conservan y al editarlos pueden mantenerlo.

| Operación | Permiso | Respuestas |
| --- | --- | --- |
| `GET /api/v1/marcas?q=&estado=&page=&size=&sort=` | `CATALOGO_CONSULTAR` | 200 página de `{ id, nombre, estado }`, 400, 401, 403 |
| `GET /api/v1/marcas/activas` | `CATALOGO_CONSULTAR` | 200 `[{ "id": 1, "nombre": "Stanley" }, ...]` sin paginar, por nombre |
| `POST /api/v1/marcas` | `CONFIG_GESTIONAR` | 201 + `Location`, 400, 401, 403, 409 |
| `PUT /api/v1/marcas/{id}` | `CONFIG_GESTIONAR` | 200, 400, 401, 403, 404, 409 |
| `PATCH /api/v1/marcas/{id}/estado` | `CONFIG_GESTIONAR` | 200, 400, 401, 403, 404 |

- `nombre`: obligatorio, 2 a 100 caracteres, se recorta; único sin distinguir mayúsculas (409 `CONFLICTO_UNICIDAD`).
- `estado` (filtro y cuerpo del PATCH): `ACTIVO` o `INACTIVO`. Sin filtro, la búsqueda devuelve ambos.
- `sort` admite `nombre` (por defecto) e `id`.

```http
POST /api/v1/categorias
Authorization: Bearer eyJ...
Content-Type: application/json

{ "nombre": "Ferretería eléctrica" }
```

Respuesta `201 Created` con `Location: /api/v1/categorias/12`:

```json
{ "id": 12, "nombre": "Ferretería eléctrica", "estado": "ACTIVO" }
```

```http
PATCH /api/v1/categorias/12/estado
Content-Type: application/json

{ "estado": "INACTIVO" }
```

## Clientes y proveedores — `/api/v1/terceros` (RF-TER-001, RN-26)

Todas las operaciones exigen `TERCERO_CLIENTE_EDITAR` o `TERCERO_PROVEEDOR_EDITAR`. Además, crear o editar
un tercero cliente exige `TERCERO_CLIENTE_EDITAR` y uno proveedor `TERCERO_PROVEEDOR_EDITAR` (los dos si es
ambos). Al editar o desactivar se miran los roles que el tercero tiene y los que tendrá: un vendedor no
edita un proveedor. Sin el permiso de un rol: 403 `ACCESO_DENEGADO`. No hay DELETE (405).

| Operación | Respuestas |
| --- | --- |
| `GET /api/v1/terceros?q=&rol=&tipoPersona=&estado=&page=&size=&sort=` | 200 página de terceros, 400, 401, 403 |
| `GET /api/v1/terceros/{id}` | 200, 401, 403, 404 |
| `POST /api/v1/terceros` | 201 + `Location`, 400, 401, 403, 409 |
| `PUT /api/v1/terceros/{id}` | 200, 400, 401, 403, 404, 409, 422 |
| `PATCH /api/v1/terceros/{id}/estado` | 200, 400, 401, 403, 404, 422 |

- Persona `NATURAL`: `CC` o `NIT`, `nombre` y `apellido` obligatorios. Persona `JURIDICA`: solo `NIT` y
  `razonSocial` obligatoria. Los campos que no aplican al tipo se descartan.
- `numeroIdentificacion`: 5 a 15 dígitos sin separadores; único por tipo de identificación (409).
- `digitoVerificacion`: obligatorio con NIT y debe coincidir con el algoritmo de la DIAN (`fn_dv_nit`);
  si no coincide, 400 en ese campo con el dígito correcto. Con CC no se admite.
- `direccion` y `telefono` (7 a 10 dígitos) obligatorios; `correo` opcional con formato válido.
- `esCliente` / `esProveedor`: al menos uno verdadero; un tercero puede ser ambos.
- `q` busca en el nombre o la razón social (sin distinguir mayúsculas) y al inicio de la identificación;
  `rol` es `CLIENTE` o `PROVEEDOR`; `sort` admite `nombre` (por defecto), `numeroIdentificacion`,
  `creadoEn` e `id`.
- El Consumidor final (CC 222222222222, `esConsumidorFinal: true`) aparece en la búsqueda, pero PUT y PATCH
  responden 422 `REGLA_NEGOCIO`: «El Consumidor final viene predefinido y no se puede editar ni desactivar».

```http
POST /api/v1/terceros
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "tipoPersona": "JURIDICA",
  "tipoIdentificacion": "NIT",
  "numeroIdentificacion": "890903938",
  "digitoVerificacion": 8,
  "razonSocial": "Bancolombia S.A.",
  "direccion": "Carrera 48 # 26-85, Medellín",
  "telefono": "6045101000",
  "correo": "proveedores@banco.test",
  "esCliente": true,
  "esProveedor": true
}
```

Respuesta `201 Created` con `Location: /api/v1/terceros/7`:

```json
{
  "id": 7, "tipoPersona": "JURIDICA", "tipoIdentificacion": "NIT", "numeroIdentificacion": "890903938",
  "digitoVerificacion": 8, "nombre": null, "apellido": null, "razonSocial": "Bancolombia S.A.",
  "nombreMostrado": "Bancolombia S.A.", "direccion": "Carrera 48 # 26-85, Medellín", "telefono": "6045101000",
  "correo": "proveedores@banco.test", "esCliente": true, "esProveedor": true, "esConsumidorFinal": false,
  "estado": "ACTIVO", "creadoEn": "2026-10-22T15:00:00Z"
}
```

Un dígito incorrecto (`"digitoVerificacion": 3`) responde `400 VALIDACION` con
`"errores": [{ "campo": "digitoVerificacion", "mensaje": "no corresponde al NIT; el dígito correcto es 8" }]`.

## Catálogos de referencia (selectores)

Exigen `CATALOGO_CONSULTAR` y devuelven solo elementos activos. Las marcas y categorías activas salen de
`GET /api/v1/marcas/activas` y `GET /api/v1/categorias/activas` (sección anterior).

| Ruta | Respuesta (ejemplo) |
| --- | --- |
| `GET /api/v1/unidades-medida` | `[{ "codigo": "UND", "nombre": "Unidad", "esDiscreta": true }, ...]` (13, por código) |
| `GET /api/v1/tarifas-iva` | `[{ "id": 1, "nombre": "Excluido / exento", "porcentaje": 0.00 }, ...]` por porcentaje |

En el perfil dev, la migración repetible `db/devdata/R__datos_ejemplo_dev.sql` carga 5 marcas y 5 categorías
para probar desde Swagger UI; test y prod no la cargan.

## Errores (Problem Details)

### 400 — validación

```json
{
  "title": "Datos no válidos",
  "status": 400,
  "detail": "Uno o más campos no cumplen las reglas de validación.",
  "instance": "/api/v1/productos",
  "codigo": "VALIDACION",
  "errores": [
    { "campo": "plazoGarantiaMeses", "mensaje": "es obligatorio cuando el control es GARANTIA" },
    { "campo": "marcaId", "mensaje": "no existe o está inactivo (9999)" }
  ]
}
```

### 401 — sin token válido

```json
{
  "title": "No autenticado",
  "status": 401,
  "detail": "Debe iniciar sesión para realizar esta operación.",
  "instance": "/api/v1/productos",
  "codigo": "NO_AUTENTICADO",
  "errores": []
}
```

### 403 — sin permiso

```json
{
  "title": "Acceso denegado",
  "status": 403,
  "detail": "Su rol no tiene permiso para realizar esta operación.",
  "instance": "/api/v1/productos",
  "codigo": "ACCESO_DENEGADO",
  "errores": []
}
```

### 404 — no existe

```json
{
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "No existe el producto con identificador 999.",
  "instance": "/api/v1/productos/999",
  "codigo": "NO_ENCONTRADO",
  "errores": []
}
```

### 409 — duplicado u operación no permitida

```json
{
  "title": "Registro duplicado",
  "status": 409,
  "detail": "Ya existe el producto «Martillo de uña 16 oz» para la marca Stanley.",
  "instance": "/api/v1/productos",
  "codigo": "CONFLICTO_UNICIDAD",
  "errores": []
}
```

### 422 — regla de la base de datos

La aplicación valida antes de escribir, así que en el CRUD de productos un 422 solo aparece si la base rechaza
el dato por una restricción que la validación no cubrió (red de seguridad):

```json
{
  "title": "Regla de integridad incumplida",
  "status": 422,
  "detail": "Los datos no cumplen una regla de integridad de la base de datos (ck_producto_garantia).",
  "instance": "/api/v1/productos",
  "codigo": "REGLA_INTEGRIDAD",
  "errores": [],
  "restriccion": "ck_producto_garantia"
}
```
