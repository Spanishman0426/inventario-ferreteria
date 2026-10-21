<!--
  Nombre: casos-de-prueba.md
  Descripción: Casos de prueba por criterio de aceptación, generados desde docs/criterios-aceptacion.yaml
               con los archivos de prueba que los implementan. No editar a mano: regenerar con
               node scripts/casos-de-prueba.mjs.
  Fecha de creación: 2026-10-21
  Última actualización: 2026-10-21
  Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
-->

# Casos de prueba por criterio de aceptación

33 criterios y 52 casos: 48 con prueba automatizada y 4 sin ella. Este documento se genera desde [`criterios-aceptacion.yaml`](../criterios-aceptacion.yaml); para cambiar un caso se edita el catálogo y se ejecuta `node scripts/casos-de-prueba.mjs`.

**Cómo leerlo.** El id del caso es `CP-<criterio>-<nn>`. "Implementado en" lista los archivos que citan el id del criterio (por ejemplo `[CA-RF-PRO-001-2]`) en el nombre visible de sus pruebas y que son del mismo nivel que el caso; el número de menciones aproxima cuántas pruebas lo verifican (una prueba parametrizada cuenta una vez aquí, pero ejecuta varias combinaciones). Niveles: clases `*IntegracionTests`, `*BaseDatosTests` y `*WebTests` del backend son de integración; las demás clases JUnit y las pruebas de Vitest, unitarias; Playwright y Selenium, E2E; k6, carga. El resultado de cada ejecución está en `reportes/criterios/` (`node scripts/criterios.mjs`).

## Resumen

| Criterio | Requisito | Casos | Niveles | Estado |
| --- | --- | --- | --- | --- |
| [CA-RF-PRO-001-1](#ca-rf-pro-001-1) | RF-PRO-001 | 3 | Unitaria, Integración, E2E | Implementado |
| [CA-RF-PRO-001-2](#ca-rf-pro-001-2) | RF-PRO-001 | 3 | Unitaria, Integración, E2E | Implementado |
| [CA-RF-PRO-001-3](#ca-rf-pro-001-3) | RF-PRO-001 | 3 | Unitaria, Integración, E2E | Implementado |
| [CA-RF-PRO-001-D1](#ca-rf-pro-001-d1) | RF-PRO-001 | 1 | E2E | Implementado |
| [CA-RF-PRO-003-1](#ca-rf-pro-003-1) | RF-PRO-003 | 1 | Integración | Pendiente |
| [CA-RF-PRO-003-2](#ca-rf-pro-003-2) | RF-PRO-003 | 1 | Integración | Pendiente |
| [CA-RF-PRO-003-3](#ca-rf-pro-003-3) | RF-PRO-003 | 3 | Unitaria, Integración, E2E | Implementado |
| [CA-RF-PRO-003-D1](#ca-rf-pro-003-d1) | RF-PRO-003 | 1 | E2E | Implementado |
| [CA-RF-CFG-001-3](#ca-rf-cfg-001-3) | RF-CFG-001 | 2 | Integración, E2E | Implementado |
| [CA-RF-SEG-001-1](#ca-rf-seg-001-1) | RF-SEG-001 | 2 | Unitaria, Integración | Implementado |
| [CA-RF-SEG-001-2](#ca-rf-seg-001-2) | RF-SEG-001 | 3 | Unitaria, Integración, E2E | Implementado |
| [CA-RF-SEG-001-3](#ca-rf-seg-001-3) | RF-SEG-001 | 2 | Unitaria, Integración | Implementado |
| [CA-RF-SEG-001-D1](#ca-rf-seg-001-d1) | RF-SEG-001 | 1 | E2E | Implementado |
| [CA-RF-SEG-002-1](#ca-rf-seg-002-1) | RF-SEG-002 | 1 | Integración | Implementado |
| [CA-RF-SEG-002-2](#ca-rf-seg-002-2) | RF-SEG-002 | 1 | Integración | Implementado |
| [CA-RF-SEG-002-3](#ca-rf-seg-002-3) | RF-SEG-002 | 1 | Integración | Implementado |
| [CA-RNF-SEG-001-1](#ca-rnf-seg-001-1) | RNF-SEG-001 | 2 | Integración, E2E | Implementado |
| [CA-RNF-SEG-001-D1](#ca-rnf-seg-001-d1) | RNF-SEG-001 | 1 | E2E | Implementado |
| [CA-RNF-SEG-001-2](#ca-rnf-seg-001-2) | RNF-SEG-001 | 1 | Integración | Implementado |
| [CA-RNF-SEG-002-1](#ca-rnf-seg-002-1) | RNF-SEG-002 | 1 | Integración | Implementado |
| [CA-RNF-SEG-003-1](#ca-rnf-seg-003-1) | RNF-SEG-003 | 1 | Integración | Implementado |
| [CA-RNF-SEG-003-2](#ca-rnf-seg-003-2) | RNF-SEG-003 | 1 | Integración | Implementado |
| [CA-RNF-SEG-004-1](#ca-rnf-seg-004-1) | RNF-SEG-004 | 1 | Integración | Implementado |
| [CA-RNF-SEG-005-1](#ca-rnf-seg-005-1) | RNF-SEG-005 | 1 | E2E | Pendiente |
| [CA-RNF-SEG-005-2](#ca-rnf-seg-005-2) | RNF-SEG-005 | 2 | Unitaria, Integración | Implementado |
| [CA-RNF-SEG-006-1](#ca-rnf-seg-006-1) | RNF-SEG-006 | 1 | Integración | Implementado |
| [CA-RNF-SEG-006-2](#ca-rnf-seg-006-2) | RNF-SEG-006 | 2 | Unitaria, Integración | Implementado |
| [CA-RNF-REN-001-1](#ca-rnf-ren-001-1) | RNF-REN-001 | 1 | Carga | Implementado |
| [CA-RNF-REN-003-1](#ca-rnf-ren-003-1) | RNF-REN-003 | 1 | Carga | Implementado |
| [CA-RNF-INT-002-1](#ca-rnf-int-002-1) | RNF-INT-002 | 3 | Unitaria, Integración, E2E | Implementado |
| [CA-RNF-INT-003-1](#ca-rnf-int-003-1) | RNF-INT-003 | 2 | Unitaria, E2E | Implementado |
| [CA-RNF-INT-003-D1](#ca-rnf-int-003-d1) | RNF-INT-003 | 1 | E2E | Implementado |
| [CA-RNF-MAN-001-1](#ca-rnf-man-001-1) | RNF-MAN-001 | 1 | Integración | Medición (jacoco) |

## RF-PRO-001

### CA-RF-PRO-001-1

**Criterio.** Un producto válido queda registrado y puede consultarse.

**Escenarios E2E.** E2E-02

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-001-1-01 | Unitaria | 1. Crear una petición completa con marca, categoría, unidad, tarifa y control (backend) o llenar el formulario (frontend).<br>2. Llamar a ProductoService.crear con los repositorios simulados, o enviar el formulario con el servicio simulado. | Nombre "  Martillo de uña 16 oz  ", marca 1, categoría 1, UND, IVA 19 %, margen 30, garantía 12 meses. | Se guarda con nombre recortado, descripción en blanco como nula y estado ACTIVO; el formulario llama al servicio una vez. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java) (JUnit, 3 menciones)<br>[frontend/src/components/productos/ProductoFormDialog.test.jsx](../../frontend/src/components/productos/ProductoFormDialog.test.jsx) (Vitest, 2 menciones)<br>[frontend/src/pages/ProductosPage.test.jsx](../../frontend/src/pages/ProductosPage.test.jsx) (Vitest, 1 mención)<br>[frontend/src/validation/productoSchema.test.js](../../frontend/src/validation/productoSchema.test.js) (Vitest, 1 mención) |
| CP-RF-PRO-001-1-02 | Integración | 1. Iniciar sesión como jefe de inventario.<br>2. POST /api/v1/productos con un producto válido.<br>3. GET /api/v1/productos/{id} y buscar por nombre. | Producto con nombre único (UUID), marca 1, categoría 1, UND, tarifa 3, margen 30, control NINGUNO. | 201 con Location; el GET devuelve los mismos datos y la búsqueda lo encuentra; se audita un CREAR. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 2 menciones) |
| CP-RF-PRO-001-1-03 | E2E | 1. Entrar como jefe de inventario y abrir Productos.<br>2. Crear un producto con garantía y plazo desde el formulario.<br>3. Buscarlo por nombre. | Nombre "E2E PW/SE <sello>", garantía 12 meses. | Notificación de éxito; la tabla lo muestra con marca, categoría, unidad, IVA y estado Activo. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

### CA-RF-PRO-001-2

**Criterio.** Se rechaza un producto sin marca, sin unidad base, sin tarifa de IVA o con garantía sin plazo.

**Escenarios E2E.** E2E-03

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-001-2-01 | Unitaria | 1. Validar la petición con Bean Validation / GarantiaConPlazoValidator (backend) o con productoSchema de Yup (frontend).<br>2. Llamar al servicio con referencias inexistentes o inactivas. | Petición sin marcaId, sin unidadBase y sin tarifaIvaId; control GARANTIA sin plazo; marca inactiva. | Un error por campo (marcaId, unidadBase, tarifaIvaId, plazoGarantiaMeses) y nada se guarda. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java) (JUnit, 6 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/validacion/GarantiaConPlazoValidatorTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/validacion/GarantiaConPlazoValidatorTests.java) (JUnit, 1 mención)<br>[frontend/src/components/productos/ProductoFormDialog.test.jsx](../../frontend/src/components/productos/ProductoFormDialog.test.jsx) (Vitest, 2 menciones)<br>[frontend/src/validation/productoSchema.test.js](../../frontend/src/validation/productoSchema.test.js) (Vitest, 6 menciones) |
| CP-RF-PRO-001-2-02 | Integración | 1. POST /api/v1/productos con los campos obligatorios vacíos o con garantía sin plazo.<br>2. Insertar por SQL un producto sin marca, sin unidad o sin tarifa, y otro con garantía sin plazo. | Token de jefe; JSON sin marcaId, unidadBase ni tarifaIvaId; control GARANTIA sin plazoGarantiaMeses. | 400 con un error en cada campo; por SQL 23502 (NOT NULL) y 23514 (ck_producto_garantia). | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoBaseDatosTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoBaseDatosTests.java) (JUnit, 2 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 3 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 3 menciones) |
| CP-RF-PRO-001-2-03 | E2E | 1. Abrir el formulario de nuevo producto y guardar vacío.<br>2. Escribir valores fuera de rango y elegir garantía sin plazo. | Nombre de 2 caracteres, porcentaje 501, garantía sin plazo. | Cada campo muestra su etiqueta y la causa; no se envía nada al servidor. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

### CA-RF-PRO-001-3

**Criterio.** Se rechaza un duplicado de nombre y marca, indicando la causa.

**Escenarios E2E.** E2E-07

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-001-3-01 | Unitaria | 1. Simular que el repositorio ya tiene el nombre para la marca.<br>2. Llamar a ProductoService.crear o actualizar. | Nombre existente con otras mayúsculas, misma marca. | ConflictoException con nombre y marca; no se llama a save. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java) (JUnit, 2 menciones)<br>[frontend/src/components/productos/ProductoFormDialog.test.jsx](../../frontend/src/components/productos/ProductoFormDialog.test.jsx) (Vitest, 1 mención) |
| CP-RF-PRO-001-3-02 | Integración | 1. Crear un producto por la API y repetir la creación con el mismo nombre en otras mayúsculas.<br>2. Insertar el duplicado por SQL y con el repositorio JPA, sin pasar por el servicio. | Mismo nombre y marca; Accept-Language es y en. | 409 CONFLICTO_UNICIDAD con la causa traducida; por SQL o JPA, 23505 (ux_producto_marca_nombre). | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoBaseDatosTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoBaseDatosTests.java) (JUnit, 2 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención) |
| CP-RF-PRO-001-3-03 | E2E | 1. Crear un producto por API y luego otro igual desde el formulario.<br>2. Cambiar solo la marca y guardar de nuevo. | Mismo nombre y marca; después otra marca. | El campo nombre muestra la causa del servidor; con otra marca sí se registra. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

### CA-RF-PRO-001-D1

**Criterio.** Un producto registrado puede editarse y los cambios quedan visibles en la consulta.

**Origen (criterio derivado).** Matriz 2.2, operación "Crear y editar productos y presentaciones".

**Escenarios E2E.** E2E-04

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-001-D1-01 | E2E | 1. Crear un producto por API.<br>2. Editar nombre, marca y porcentaje desde la interfaz.<br>3. Buscar por el nombre anterior y por el nuevo. | Producto "E2E ... <sello>"; nueva marca y margen 45. | El nombre anterior ya no aparece; el nuevo se muestra con la marca y el margen editados. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

## RF-PRO-003

### CA-RF-PRO-003-1

**Criterio.** Un código registrado devuelve exactamente su presentación.

**Observación.** Pendiente; depende de las presentaciones y códigos de barras (RF-PRO-002), aún no implementados.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-003-1-01 | Integración | 1. Consultar por la API el código de barras de una presentación del escenario. | Código 7702001000012 (presentación 1 del escenario de prueba). | Una sola presentación, la 1, con su producto. (Prueba prevista.) | _Sin prueba automatizada_ |

### CA-RF-PRO-003-2

**Criterio.** Las existencias mostradas coinciden con la suma de movimientos y cambios de estado.

**Observación.** Pendiente; la consulta de existencias por estado y lote aún no está implementada.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-003-2-01 | Integración | 1. Consultar las existencias de cada producto del escenario por la API.<br>2. Compararlas con la suma del kardex (v_kardex) por estado. | Escenario de database/02_datos_prueba.sql. | 0 discrepancias, como PC-02 del plan de pruebas de la base. (Prueba prevista.) | _Sin prueba automatizada_ |

### CA-RF-PRO-003-3

**Criterio.** Un vendedor no ve costos.

**Escenarios E2E.** E2E-08

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-003-3-01 | Unitaria | 1. Convertir un producto a respuesta con y sin el permiso COSTOS_VER (backend).<br>2. Renderizar la página de productos con un usuario sin COSTOS_VER (frontend). | Producto con margen 30 y último costo 18.000. | Sin el permiso no se incluyen porcentajeIncremento ni ultimoCostoBase, y la tabla no muestra la columna. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java) (JUnit, 3 menciones)<br>[frontend/src/pages/ProductosPage.test.jsx](../../frontend/src/pages/ProductosPage.test.jsx) (Vitest, 2 menciones) |
| CP-RF-PRO-003-3-02 | Integración | 1. Consultar el mismo producto con el token del vendedor y con el del gerente. | Usuarios vendedor y gerente del escenario. | El JSON del vendedor omite margen y costo; el del gerente los trae. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención) |
| CP-RF-PRO-003-3-03 | E2E | 1. Entrar como vendedor y abrir Productos. | Usuario vendedor. | No hay columna de costos ni botones de edición. | [e2e/playwright/tests/permisos-idioma.spec.js](../../e2e/playwright/tests/permisos-idioma.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/permisos-idioma.spec.js](../../e2e/selenium/tests/permisos-idioma.spec.js) (Selenium, 1 mención) |

### CA-RF-PRO-003-D1

**Criterio.** La búsqueda por nombre, sin distinguir mayúsculas, y los filtros por categoría y estado devuelven solo los productos que cumplen todos los criterios; sin coincidencias se informa que no hay resultados.

**Origen (criterio derivado).** Descripción y pasos 1 y 2 del flujo principal de RF-PRO-003.

**Escenarios E2E.** E2E-05

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-PRO-003-D1-01 | E2E | 1. Crear por API tres productos propios, uno inactivo.<br>2. Buscar con otras mayúsculas y combinar filtros de categoría y estado.<br>3. Buscar un texto sin coincidencias. | Tres productos "E2E ... <sello>" en dos categorías. | Solo aparecen los que cumplen todos los filtros; sin coincidencias se muestra el aviso de sin resultados. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

## RF-CFG-001

### CA-RF-CFG-001-3

**Criterio.** Un elemento de catálogo en uso no se elimina; solo se desactiva.

**Escenarios E2E.** E2E-06

**Observación.** Por ahora cubre el producto (DELETE /api/v1/productos/{id} lo deja INACTIVO, previa confirmación en la interfaz); marcas, categorías y motivos se agregarán con la administración de RF-CFG-001.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-CFG-001-3-01 | Integración | 1. DELETE /api/v1/productos/{id} con el token del jefe.<br>2. Consultar el producto y repetir el DELETE. | Producto creado para la prueba. | 204; el producto sigue existiendo como INACTIVO y consultable; repetirlo no audita de nuevo. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención) |
| CP-RF-CFG-001-3-02 | E2E | 1. Pulsar Desactivar y cancelar el diálogo.<br>2. Pulsar Desactivar y confirmar. | Producto creado por API. | Cancelar no cambia nada; confirmar lo deja Inactivo, sin botón de desactivar y aún editable. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

## RF-SEG-001

### CA-RF-SEG-001-1

**Criterio.** Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.

**Observación.** La página de inicio de sesión muestra el aviso (Vitest, nivel unitario); falta el escenario E2E.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-001-1-01 | Unitaria | 1. Simular 5 inicios de sesión con contraseña incorrecta en ServicioAutenticacion.<br>2. Intentar con la contraseña correcta antes y después de 15 minutos (reloj fijo).<br>3. Mostrar en LoginPage la respuesta CUENTA_BLOQUEADA. | Usuario con hash BCrypt simulado; reloj en 2026-10-21 10:00 Bogotá. | Al quinto fallo bloqueado_hasta = ahora + 15 min; con la correcta sigue rechazado hasta que vence el plazo. | [backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java) (JUnit, 3 menciones)<br>[frontend/src/pages/LoginPage.test.jsx](../../frontend/src/pages/LoginPage.test.jsx) (Vitest, 1 mención) |
| CP-RF-SEG-001-1-02 | Integración | 1. POST /api/v1/auth/login 5 veces con contraseña incorrecta.<br>2. Repetir con la contraseña correcta. | Usuario gerente del escenario. | 423 CUENTA_BLOQUEADA con la hora de desbloqueo en es y en, aun con la contraseña correcta. | [backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/auth/AuthControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AuthControllerWebTests.java) (JUnit, 1 mención) |

### CA-RF-SEG-001-2

**Criterio.** Un usuario sin permiso no ejecuta la operación, ni por interfaz ni por API.

**Escenarios E2E.** E2E-08

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-001-2-01 | Unitaria | 1. Renderizar ProtectedRoute y ProductosPage con un usuario sin el permiso. | Usuario sin CATALOGO_EDITAR o sin la ruta permitida. | No se muestra la acción ni la página; se redirige. | [frontend/src/components/ProtectedRoute.test.jsx](../../frontend/src/components/ProtectedRoute.test.jsx) (Vitest, 1 mención)<br>[frontend/src/pages/ProductosPage.test.jsx](../../frontend/src/pages/ProductosPage.test.jsx) (Vitest, 1 mención) |
| CP-RF-SEG-001-2-02 | Integración | 1. Recorrer cada endpoint de /api con el token de cada uno de los 6 roles (MatrizPermisosIntegracionTests).<br>2. Comprobar en la base que una operación denegada no deja rastro. | 20 endpoints × 6 roles con cuerpos válidos; productos y usuarios creados para cada caso. | 2xx si la matriz 2.2 da el permiso; si no, 403 ACCESO_DENEGADO sin registro de auditoría. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 2 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/AutorizacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/AutorizacionIntegracionTests.java) (JUnit, 3 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/security/MatrizPermisosIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/MatrizPermisosIntegracionTests.java) (JUnit, 1 mención) |
| CP-RF-SEG-001-2-03 | E2E | 1. Entrar como vendedor; revisar la tabla de productos.<br>2. Llamar a la API de crear y desactivar con su token. | Usuario vendedor. | Sin botones de crear, editar ni desactivar; la API responde 403. | [e2e/playwright/tests/permisos-idioma.spec.js](../../e2e/playwright/tests/permisos-idioma.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/permisos-idioma.spec.js](../../e2e/selenium/tests/permisos-idioma.spec.js) (Selenium, 1 mención) |

### CA-RF-SEG-001-3

**Criterio.** Los usuarios no se eliminan, solo se desactivan; una cuenta desactivada no inicia sesión.

**Observación.** La página de inicio de sesión muestra el aviso (Vitest, nivel unitario); falta el escenario E2E.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-001-3-01 | Unitaria | 1. Iniciar sesión en ServicioAutenticacion con un usuario inactivo y la contraseña correcta.<br>2. Mostrar en LoginPage la respuesta de cuenta inactiva. | Usuario con activo = false. | Se rechaza sin emitir token. | [backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java) (JUnit, 1 mención)<br>[frontend/src/pages/LoginPage.test.jsx](../../frontend/src/pages/LoginPage.test.jsx) (Vitest, 1 mención) |
| CP-RF-SEG-001-3-02 | Integración | 1. PATCH /api/v1/usuarios/{id}/estado con activo false.<br>2. Iniciar sesión con ese usuario; intentar DELETE /api/v1/usuarios/{id}. | Usuario creado por el administrador. | El login se rechaza; DELETE no existe (405) y el usuario sigue en la tabla. | [backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/usuario/UsuarioIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/usuario/UsuarioIntegracionTests.java) (JUnit, 2 menciones) |

### CA-RF-SEG-001-D1

**Criterio.** Con usuario y contraseña válidos el sistema inicia la sesión y muestra el nombre del usuario; con credenciales inválidas la rechaza con un aviso y no da acceso.

**Origen (criterio derivado).** Pasos 1 y 2 del flujo principal de RF-SEG-001.

**Escenarios E2E.** E2E-01

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-001-D1-01 | E2E | 1. Abrir /login, escribir una contraseña errada y enviar.<br>2. Escribir la contraseña correcta y enviar. | Usuario jefe con E2E_JEFE_PASSWORD. | Con la errada, aviso y sigue en /login; con la correcta entra a productos y muestra "Julia Jefe Inventario". | [e2e/playwright/tests/autenticacion.spec.js](../../e2e/playwright/tests/autenticacion.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/autenticacion.spec.js](../../e2e/selenium/tests/autenticacion.spec.js) (Selenium, 1 mención) |

## RF-SEG-002

### CA-RF-SEG-002-1

**Criterio.** Cada operación auditable genera exactamente un registro en la misma transacción.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-002-1-01 | Integración | 1. Anotar el último id de auditoría.<br>2. Iniciar sesión (correcto y fallido), crear, editar e inactivar un producto y desactivar un usuario por la API.<br>3. Insertar y modificar un producto por SQL en una transacción que se revierte. | Usuarios del escenario; productos con nombre único. | Un registro por operación (INICIAR_SESION, SESION_FALLIDA, CREAR, MODIFICAR) con usuario e IP; un UPDATE sin cambios no se audita; al revertir no queda nada. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaBaseDatosTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaBaseDatosTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java) (JUnit, 3 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java) (JUnit, 2 menciones) |

### CA-RF-SEG-002-2

**Criterio.** Nadie puede modificar ni borrar un registro de menos de 24 meses.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-002-2-01 | Integración | 1. PUT y DELETE sobre /api/v1/auditoria/{id} con el token del administrador.<br>2. UPDATE y DELETE sobre auditoria por SQL con el usuario de la aplicación. | Último registro de auditoría. | 405 por la API y P0001 por SQL; el registro queda igual. | [backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java) (JUnit, 2 menciones) |

### CA-RF-SEG-002-3

**Criterio.** Un registro de 25 meses desaparece en la siguiente depuración mensual.

**Observación.** Se prueba la función fn_depurar_auditoria de la base; la tarea programada mensual que la invoca aún no está implementada.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RF-SEG-002-3-01 | Integración | 1. Insertar un registro con fecha de hace 25 meses y otro de hace 23.<br>2. Ejecutar fn_depurar_auditoria(). | Particiones mensuales de auditoria. | Desaparece el de 25 meses, queda el de 23 y se registra una constancia DEPURAR. | [backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java) (JUnit, 1 mención) |

## RNF-SEG-001

### CA-RNF-SEG-001-1

**Criterio.** Toda ruta protegida responde 401 sin token.

**Escenarios E2E.** E2E-09

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-001-1-01 | Integración | 1. Llamar a cada endpoint de /api sin la cabecera Authorization (MatrizPermisosIntegracionTests).<br>2. Llamar con un token alterado y con uno vencido. | Los 20 endpoints protegidos con cuerpos válidos. | 401 NO_AUTENTICADO en Problem Details en todos. | [backend/src/test/java/co/ferreteria/inventario/catalogo/CatalogoReferenciaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/CatalogoReferenciaIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/MatrizPermisosIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/MatrizPermisosIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java) (JUnit, 2 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/security/auth/AuthControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AuthControllerWebTests.java) (JUnit, 1 mención) |
| CP-RNF-SEG-001-1-02 | E2E | 1. GET /api/v1/productos sin token desde la prueba. | Sin sesión. | 401 | [e2e/playwright/tests/autenticacion.spec.js](../../e2e/playwright/tests/autenticacion.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/autenticacion.spec.js](../../e2e/selenium/tests/autenticacion.spec.js) (Selenium, 1 mención) |

### CA-RNF-SEG-001-D1

**Criterio.** Una página protegida abierta sin sesión lleva al inicio de sesión y no muestra su contenido.

**Origen (criterio derivado).** Requisito de RNF-SEG-001 ("Todo acceso exige sesión autenticada"), aplicado a la interfaz.

**Escenarios E2E.** E2E-09

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-001-D1-01 | E2E | 1. Abrir /productos en un navegador sin sesión. | Sin sesión. | Redirige a /login y no muestra la tabla de productos. | [e2e/playwright/tests/autenticacion.spec.js](../../e2e/playwright/tests/autenticacion.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/autenticacion.spec.js](../../e2e/selenium/tests/autenticacion.spec.js) (Selenium, 1 mención) |

### CA-RNF-SEG-001-2

**Criterio.** Ninguna contraseña se guarda en texto plano.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-001-2-01 | Integración | 1. Crear un usuario por la API y cambiarle la contraseña.<br>2. Crear dos usuarios con la misma contraseña.<br>3. Buscar las contraseñas en la tabla usuario y en la auditoría. | Contraseñas aleatorias que cumplen la política (letras y números). | password_hash es BCrypt de costo 12 ($2a$12$...), verificable con BCryptPasswordEncoder; hashes distintos con la misma contraseña; ni la contraseña ni el hash aparecen en la auditoría ni en la respuesta. | [backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java) (JUnit, 2 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/usuario/UsuarioIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/usuario/UsuarioIntegracionTests.java) (JUnit, 1 mención) |

## RNF-SEG-002

### CA-RNF-SEG-002-1

**Criterio.** 100 % de las pruebas de la matriz pasan por interfaz y por API.

**Observación.** Por API se recorren todos los endpoints publicados; por interfaz, E2E-08 cubre el vendedor.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-002-1-01 | Integración | 1. Recorrer cada endpoint contra cada rol y comprobar 2xx o 403.<br>2. Verificar que la matriz probada cubra todos los endpoints publicados en /api.<br>3. Verificar los permisos que lleva el token de cada rol. | 20 endpoints × 6 roles; tabla rol_permiso de V1. | 120 de 120 combinaciones según la matriz 2.2; ningún endpoint sin probar; admin 26, gerente 22, jefe 8, comprador 7, vendedor 6 y auditor 4 permisos. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/AutorizacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/AutorizacionIntegracionTests.java) (JUnit, 5 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/security/MatrizPermisosIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/MatrizPermisosIntegracionTests.java) (JUnit, 2 menciones) |

## RNF-SEG-003

### CA-RNF-SEG-003-1

**Criterio.** Cada operación de prueba genera su registro (usuario, fecha, IP, acción y valores antes y después).

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-003-1-01 | Integración | 1. Crear y modificar un producto por la API con el token del jefe.<br>2. Leer los registros de auditoría de ese producto. | Producto con margen 30 que se cambia a 45. | CREAR (sin antes) y MODIFICAR (antes 30.00, después 45.00), ambos con usuario_id del jefe e IP 127.0.0.1. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java) (JUnit, 1 mención) |

### CA-RNF-SEG-003-2

**Criterio.** Retención de 24 meses.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-003-2-01 | Integración | 1. Ejecutar la depuración con registros de 23 y 25 meses. | Igual que CA-RF-SEG-002-3. | Se conserva el de 23 meses y se elimina el de 25. | [backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java) (JUnit, 1 mención) |

## RNF-SEG-004

### CA-RNF-SEG-004-1

**Criterio.** Los intentos de editar o borrar la auditoría y los documentos confirmados, por API y por SQL con el usuario de la aplicación, son rechazados.

**Observación.** Por ahora cubre la auditoría; los documentos confirmados se agregarán con compras y ventas.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-004-1-01 | Integración | 1. Igual que CA-RF-SEG-002-2 (API y SQL sobre auditoria). | Último registro de auditoría. | 405 por la API y P0001 por SQL. | [backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/common/auditoria/AuditoriaIntegracionTests.java) (JUnit, 2 menciones) |

## RNF-SEG-005

### CA-RNF-SEG-005-1

**Criterio.** Sin hallazgos altos en pruebas OWASP Top 10.

**Observación.** Requiere un escaneo OWASP sobre la aplicación desplegada. El backend ya prueba partes (búsqueda sin inyección de comodines, CORS, cabeceras de seguridad), pero no reemplazan el escaneo.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-005-1-01 | E2E | 1. Levantar docker-compose.e2e.yml y ejecutar un escaneo OWASP ZAP (baseline y API con openapi.yaml). | URL del frontend y especificación docs/api/openapi.yaml. | 0 alertas de riesgo alto. (Prueba prevista.) | _Sin prueba automatizada_ |

### CA-RNF-SEG-005-2

**Criterio.** Bloqueo tras 5 intentos fallidos.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-005-2-01 | Unitaria | 1. Igual que el caso unitario de CA-RF-SEG-001-1. | Usuario con 4 intentos fallidos. | El quinto fallo bloquea la cuenta. | [backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java) (JUnit, 1 mención) |
| CP-RNF-SEG-005-2-02 | Integración | 1. Igual que el caso de integración de CA-RF-SEG-001-1. | Usuario gerente. | 423 CUENTA_BLOQUEADA. | [backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java) (JUnit, 1 mención) |

## RNF-SEG-006

### CA-RNF-SEG-006-1

**Criterio.** HTTPS obligatorio.

**Observación.** El backend verifica la cabecera HSTS; la redirección a HTTPS depende del despliegue.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-006-1-01 | Integración | 1. Hacer solicitudes HTTPS (secure) con respuestas 200, 401 y 403, al login y al actuator.<br>2. Hacer la misma solicitud por HTTP plano. | Tokens de admin y vendedor; sin token. | Toda respuesta HTTPS lleva Strict-Transport-Security max-age=31536000 ; includeSubDomains, además de nosniff y X-Frame-Options DENY; por HTTP no se envía HSTS. | [backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/SeguridadNoFuncionalIntegracionTests.java) (JUnit, 2 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java) (JUnit, 1 mención) |

### CA-RNF-SEG-006-2

**Criterio.** La sesión expira tras 30 minutos de inactividad.

**Observación.** El backend emite tokens que vencen 30 minutos después de emitidos y rechaza los vencidos; la renovación mientras el usuario está activo se verificará en e2e.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-SEG-006-2-01 | Unitaria | 1. Emitir un token con ServicioToken y reloj fijo. | Reloj en 2026-10-21 10:00. | exp = iat + 30 minutos; expiresIn = 1800. | [backend/src/test/java/co/ferreteria/inventario/security/ServicioTokenTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/ServicioTokenTests.java) (JUnit, 1 mención) |
| CP-RNF-SEG-006-2-02 | Integración | 1. Llamar a /api/v1/auth/me con un token vencido. | Token firmado con la clave de la aplicación y exp en el pasado. | 401 | [backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AutenticacionIntegracionTests.java) (JUnit, 1 mención) |

## RNF-REN-001

### CA-RNF-REN-001-1

**Criterio.** Búsqueda por código y confirmación de venta rápidas con el volumen de diseño: percentil 95 menor a 1 s.

**Medición.** umbral del resumen de k6 (`load-tests/results/k6-resumen-*.json`).

**Observación.** k6 mide búsqueda por nombre, consulta por id y creación de productos (código de barras y ventas aún no existen). Solo el modo carga sobre ferreteria_carga usa el volumen de diseño; el modo humo de la CI corre con 1 usuario sobre el escenario de prueba y es una verificación débil.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-REN-001-1-01 | Carga | 1. Cargar load-tests/datos-volumen.sql en ferreteria_carga y levantar el entorno contra esa base.<br>2. Ejecutar k6 run load-tests/productos.js (50 usuarios, 5 minutos; 70 % buscar, 20 % consultar, 10 % crear). | 100.000 presentaciones y 50.000 productos; usuario jefe. | p95 de http_req_duration < 1000 ms (umbral de k6). | [load-tests/productos.js](../../load-tests/productos.js) (k6, 1 mención) |

## RNF-REN-003

### CA-RNF-REN-003-1

**Criterio.** 50 usuarios concurrentes en prueba de carga sin errores ni saldos negativos.

**Medición.** umbral del resumen de k6 (`load-tests/results/k6-resumen-*.json`).

**Observación.** Solo cuenta un resumen de k6 en modo carga (50 usuarios virtuales o más); el modo humo no lo demuestra. Los saldos negativos se verificarán cuando existan ventas.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-REN-003-1-01 | Carga | 1. Igual que CA-RNF-REN-001-1, con 50 usuarios virtuales constantes. | 100.000 presentaciones; 50 VU durante 5 minutos. | http_req_failed < 1 % (umbral de k6) y todas las comprobaciones de respuesta aprobadas. | [load-tests/productos.js](../../load-tests/productos.js) (k6, 1 mención) |

## RNF-INT-002

### CA-RNF-INT-002-1

**Criterio.** Cada regla de validación muestra campo y causa.

**Escenarios E2E.** E2E-03

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-INT-002-1-01 | Unitaria | 1. Validar con productoSchema cada regla (obligatorios, longitudes, rango del margen, garantía).<br>2. Renderizar ProductoFormDialog y enviarlo con errores. | Un valor inválido por regla; idioma es y en. | Un mensaje por campo con su causa, traducido. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoServiceTests.java) (JUnit, 1 mención)<br>[frontend/src/components/productos/ProductoFormDialog.test.jsx](../../frontend/src/components/productos/ProductoFormDialog.test.jsx) (Vitest, 3 menciones)<br>[frontend/src/validation/productoSchema.test.js](../../frontend/src/validation/productoSchema.test.js) (Vitest, 16 menciones) |
| CP-RNF-INT-002-1-02 | Integración | 1. Enviar a la API peticiones inválidas de producto y de login, con Accept-Language es y en. | Margen 501, nombre corto, campos vacíos, orden desconocido. | 400 en Problem Details con un error por campo (campo y mensaje) en el idioma pedido. | [backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoControllerWebTests.java) (JUnit, 4 menciones)<br>[backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java](../../backend/src/test/java/co/ferreteria/inventario/catalogo/ProductoIntegracionTests.java) (JUnit, 1 mención)<br>[backend/src/test/java/co/ferreteria/inventario/security/auth/AuthControllerWebTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/AuthControllerWebTests.java) (JUnit, 1 mención) |
| CP-RNF-INT-002-1-03 | E2E | 1. Igual que el caso e2e de CA-RF-PRO-001-2. | Formulario vacío y valores fuera de rango. | Cada campo muestra su etiqueta y "Este campo es obligatorio" u otra causa. | [e2e/playwright/tests/productos.spec.js](../../e2e/playwright/tests/productos.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/productos.spec.js](../../e2e/selenium/tests/productos.spec.js) (Selenium, 1 mención) |

## RNF-INT-003

### CA-RNF-INT-003-1

**Criterio.** Moneda como $ 12.350, fechas dd/mm/aaaa, zona horaria America/Bogota.

**Escenarios E2E.** E2E-10

**Observación.** El backend formatea fechas en mensajes (bloqueo de cuenta); E2E-10 verifica la moneda en pantalla ($ 18.000). Las fechas en pantalla se verificarán cuando una página las muestre.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-INT-003-1-01 | Unitaria | 1. Bloquear una cuenta con el reloj fijo y leer el mensaje. | Reloj en UTC; desbloqueo 15 minutos después. | La hora aparece en Bogotá con formato dd/MM/yyyy HH:mm. | [backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java](../../backend/src/test/java/co/ferreteria/inventario/security/auth/ServicioAutenticacionTests.java) (JUnit, 1 mención) |
| CP-RNF-INT-003-1-02 | E2E | 1. Abrir Productos como jefe. | Producto del escenario con último costo 18.000. | La moneda se muestra como $ 18.000. | [e2e/playwright/tests/permisos-idioma.spec.js](../../e2e/playwright/tests/permisos-idioma.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/permisos-idioma.spec.js](../../e2e/selenium/tests/permisos-idioma.spec.js) (Selenium, 1 mención) |

### CA-RNF-INT-003-D1

**Criterio.** La interfaz se muestra en español por defecto y puede cambiarse a inglés; la elección se conserva al recargar la página y al cerrar la sesión.

**Origen (criterio derivado).** Requisito de RNF-INT-003 (interfaz en español) y regla de internacionalización del proyecto.

**Escenarios E2E.** E2E-10

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-INT-003-D1-01 | E2E | 1. Entrar y comprobar el idioma; cambiar a inglés.<br>2. Recargar y cerrar la sesión. | Usuario jefe. | Arranca en español (lang="es"); en inglés cambian títulos, botones, menú y estados; la elección sobrevive a recargar y a cerrar sesión. | [e2e/playwright/tests/permisos-idioma.spec.js](../../e2e/playwright/tests/permisos-idioma.spec.js) (Playwright, 1 mención)<br>[e2e/selenium/tests/permisos-idioma.spec.js](../../e2e/selenium/tests/permisos-idioma.spec.js) (Selenium, 1 mención) |

## RNF-MAN-001

### CA-RNF-MAN-001-1

**Criterio.** Cobertura del backend mayor o igual a 70 %.

**Medición.** cobertura de líneas de JaCoCo (`backend/target/site/jacoco/jacoco.csv`).

**Observación.** Lo verifica la regla jacoco:check del pom (cobertura de líneas >= 0.70) en ./mvnw verify, no una prueba con nombre; scripts/criterios.mjs lee backend/target/site/jacoco/jacoco.csv.

| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |
| --- | --- | --- | --- | --- | --- |
| CP-RNF-MAN-001-1-01 | Integración | 1. Ejecutar ./mvnw verify (pruebas unitarias y de integración con el agente de JaCoCo). | Todas las pruebas del backend. | Cobertura de líneas >= 70 %; si no, jacoco:check hace fallar el build. | _Sin prueba automatizada_ |
