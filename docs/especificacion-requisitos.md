# Especificación de Requisitos — Sistema de Inventario de Ferretería

Oct 19, 2026 · @Someone

## 1. Introducción

Esta es la versión 4 y queda **aprobada para diseño**: el interesado respondió las 19 preguntas abiertas, aprobó las prioridades, las reglas propuestas y los valores de calidad, y delegó en el analista las decisiones marcadas "a conveniencia". Ya no quedan requisitos **Por definir**.

### 1.1 Propósito

Documentar actores, requisitos funcionales, requisitos de calidad y reglas de negocio de un sistema web de inventario y facturación para una ferretería, como base para el análisis, el diseño y el modelo de datos.

### 1.2 Alcance

El sistema opera sobre **una sola bodega** y cubre el ciclo completo de la mercancía: compra, recepción, venta, devolución, cambio de estado, baja, conteo, ajuste y cierre mensual.

| Dentro del alcance | Fuera del alcance |
| --- | --- |
| Catálogo con marcas, presentaciones, conversiones y códigos de barras | Operación sin internet |
| Terceros (clientes y proveedores) y datos de la empresa | Múltiples bodegas y traslados |
| Órdenes de compra, recepciones parciales y documentos de ingreso | Facturación electrónica ante la DIAN |
| Ventas de contado con factura, IVA, descuentos y varios medios de pago | Ventas a crédito y cartera |
| Devoluciones de clientes y a proveedores | Contabilidad general |
| Reservas, dañados, bajas, conteos físicos y ajustes | Alertas automáticas por correo |
| Costeo por lotes (PEPS con prioridad por vencimiento) y precios con historial |  |
| Cierres mensuales con reapertura controlada |  |
| Usuarios, roles, auditoría, migración desde Excel, lector de códigos, impresión, exportación y API |  |

### 1.3 Referencias normativas

- **ISO/IEC/IEEE 29148:2018**: estructura de la especificación y características de calidad de cada requisito.
- **ISO/IEC 25010:2023**: clasificación de los requisitos no funcionales.

### 1.4 Definiciones

| Término | Significado en este documento |
| --- | --- |
| Producto | Artículo genérico del catálogo, asociado a una marca y a una unidad base. |
| Presentación | Forma comercial de un producto (unidad, caja x12, bulto 50 kg, rollo 100 m) con su código de barras y factor de conversión. |
| Unidad base | Unidad en la que se guarda la existencia de un producto (UND, KG, LTR, MTR, etc.). |
| Lote | Capa de inventario creada por una recepción, un ajuste positivo o la migración, con costo unitario, fecha de ingreso y vencimiento opcional. |
| PEPS / FEFO | Primero en entrar, primero en salir / primero en vencer, primero en salir. Definen qué lote consume cada salida. |
| Movimiento | Cambio en la existencia total: recepción, venta, devolución, baja o ajuste. |
| Cambio de estado | Traslado de cantidad entre disponible, reservada y dañada; no altera la existencia total. |
| Tercero | Cliente, proveedor o ambos; persona natural o jurídica. |
| Consumidor final | Tercero predefinido para ventas sin cliente identificado. |

## 2. Actores y permisos

El sistema tiene seis roles humanos, un usuario de sistema para tareas programadas y dos actores externos; la matriz 2.2 define qué puede hacer cada rol (RN-27).

### 2.1 Actores

| Actor | Tipo | Responsabilidad |
| --- | --- | --- |
| Administrador | Humano | Usuarios, roles, configuración, migración y reapertura de períodos; acceso total. |
| Gerente | Humano | Precios manuales, descuentos mayores, aprobación de ajustes y bajas, cierres y reportes con costos. |
| Jefe de inventario | Humano | Catálogo, recepciones, dañados, conteos físicos y propuesta de ajustes y bajas. |
| Comprador | Humano | Proveedores, órdenes de compra, recepciones y devoluciones a proveedor. |
| Vendedor | Humano | Clientes, ventas, reservas y devoluciones de clientes. |
| Auditor | Humano | Consulta de auditoría, historial y reportes, solo lectura. |
| Sistema (tareas programadas) | Sistema | Vence reservas, traslada lotes vencidos a dañada y depura auditoría mayor a 24 meses. |
| Lector de código de barras | Dispositivo | Entrega el código leído como entrada de teclado. |
| Sistema externo | Sistema | Consume la API REST (por ejemplo, una tienda en línea que consulta existencias). |

### 2.2 Matriz de permisos

| Operación | Admin | Gerente | Jefe inv. | Comprador | Vendedor | Auditor |
| --- | --- | --- | --- | --- | --- | --- |
| Usuarios, roles y configuración | Sí | — | — | — | — | — |
| Crear y editar productos y presentaciones | Sí | Sí | Sí | — | — | — |
| Consultar productos y existencias | Sí | Sí | Sí | Sí | Sí | Sí |
| Ver costos y utilidad | Sí | Sí | Sí | Sí | — | Sí |
| Fijar precio manual | Sí | Sí | — | — | — | — |
| Registrar terceros | Sí | Sí | — | Proveedores | Clientes | — |
| Órdenes de compra y recepciones | Sí | Sí | Recepciones | Sí | — | — |
| Devoluciones a proveedor | Sí | Sí | — | Sí | — | — |
| Vender y reservar | Sí | Sí | — | — | Sí | — |
| Descuento mayor a 10 % | Sí | Sí | — | — | — | — |
| Anular factura del día | Sí | Sí | — | — | — | — |
| Devoluciones de clientes | Sí | Sí | — | — | Sí | — |
| Marcar dañado | Sí | Sí | Sí | — | — | — |
| Retornar dañado a disponible | Sí | Sí | — | — | — | — |
| Proponer baja o ajuste; registrar conteo | Sí | Sí | Sí | — | — | — |
| Aprobar baja o ajuste | Sí | Sí | — | — | — | — |
| Cerrar mes | Sí | Sí | — | — | — | — |
| Reabrir mes | Sí | — | — | — | — | — |
| Reportes e historial de movimientos | Sí | Sí | Sí | Sí | Sin costos | Sí |
| Consultar auditoría | Sí | Sí | — | — | — | Sí |
| Migración desde Excel | Sí | — | — | — | — | — |

Quien propone una baja o un ajuste no puede aprobarlo (separación de funciones).

## 3. Requisitos funcionales

Se especifican 27 requisitos funcionales en 15 módulos con la plantilla del curso; las prioridades están aprobadas por el interesado.

| ID | Nombre | Prioridad |
| --- | --- | --- |
| RF-PRO-001 | Registrar producto | Crítica |
| RF-PRO-002 | Gestionar presentaciones y códigos de barras | Crítica |
| RF-PRO-003 | Buscar y consultar productos | Alta |
| RF-TER-001 | Registrar clientes y proveedores | Crítica |
| RF-CFG-001 | Configurar empresa, impuestos y catálogos | Alta |
| RF-PRE-001 | Gestionar costos, precios e historial | Alta |
| RF-COM-001 | Registrar orden de compra | Alta |
| RF-COM-002 | Recibir mercancía y generar documento de ingreso | Crítica |
| RF-VEN-001 | Registrar venta y generar factura | Crítica |
| RF-VEN-002 | Anular factura | Media |
| RF-DEV-001 | Registrar devolución de cliente | Alta |
| RF-DEV-002 | Registrar devolución a proveedor | Media |
| RF-EST-001 | Reservar y liberar existencias | Media |
| RF-EST-002 | Clasificar existencias dañadas | Media |
| RF-EST-003 | Dar de baja existencias dañadas | Media |
| RF-CON-001 | Registrar conteo físico | Alta |
| RF-AJU-001 | Aprobar y aplicar ajuste de inventario | Alta |
| RF-CIE-001 | Ejecutar cierre mensual | Alta |
| RF-CIE-002 | Reabrir período cerrado | Media |
| RF-MOV-001 | Consultar kardex e historial de movimientos | Alta |
| RF-REP-001 | Consultar reportes | Alta |
| RF-SEG-001 | Gestionar usuarios, roles y sesión | Crítica |
| RF-SEG-002 | Registrar y consultar auditoría | Crítica |
| RF-MIG-001 | Migrar datos desde Excel | Alta |
| RF-LEC-001 | Leer código de barras | Alta |
| RF-DOC-001 | Imprimir, enviar y exportar documentos | Alta |
| RF-INT-001 | Exponer API para sistemas externos | Baja |

### 3.1 Catálogo, terceros, configuración y precios

#### RF-PRO-001 — Registrar producto

**Descripción.** El sistema deberá permitir registrar un producto con marca, categoría, unidad base, tarifa de IVA, porcentaje de incremento y tipo de control (vencimiento o garantía).

**Actor.** Jefe de inventario, gerente o administrador.

**Prioridad.** Crítica.

**Precondiciones.** Usuario autenticado con permiso de catálogo; marca y categoría registradas.

**Flujo principal**

1. El actor solicita registrar un producto.
2. El sistema presenta el formulario.
3. El actor introduce los datos.
4. El sistema valida obligatoriedad y que no exista el mismo nombre para la misma marca.
5. El sistema guarda el producto y lo audita.
6. El sistema confirma y ofrece agregar presentaciones (RF-PRO-002).

**Reglas de negocio.** RN-05, RN-08, RN-18, RN-20.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| nombre | string | Sí | Denominación del producto. |
| marca | referencia | Sí | Marca del producto. |
| categoria | referencia | Sí | Herramientas, eléctricos, plomería, pinturas, etc. |
| unidad\_base | catálogo | Sí | UND, PAR, JGO, KG, G, LB, LTR, ML, GAL, MTR, CM, M2, M3. |
| tarifa\_iva | catálogo | Sí | 0 %, 5 % o 19 %. |
| porcentaje\_incremento | decimal | Sí | Margen sobre el costo, entre 0 y 500. |
| control | enumeración | Sí | Ninguno, vencimiento o garantía. |
| plazo\_garantia\_meses | entero | Condicional | Obligatorio si control = garantía. |
| descripcion | string | No | Detalle adicional. |

**Criterios de aceptación**

- Un producto válido queda registrado y puede consultarse.
- Se rechaza un producto sin marca, sin unidad base, sin tarifa de IVA o con garantía sin plazo.
- Se rechaza un duplicado de nombre y marca, indicando la causa.

#### RF-PRO-002 — Gestionar presentaciones y códigos de barras

**Descripción.** El sistema deberá asociar a un producto varias presentaciones, cada una con un único código de barras y un factor de conversión a la unidad base, y generar un código interno cuando no exista uno de fabricante utilizable.

**Actor.** Jefe de inventario, gerente o administrador.

**Prioridad.** Crítica.

**Precondiciones.** Producto registrado.

**Flujo principal**

1. El actor selecciona un producto y solicita agregar una presentación.
2. El sistema presenta los campos.
3. El actor introduce nombre, factor y, si existe, el código del fabricante.
4. El sistema valida factor y unicidad del código; si el código está vacío o ya pertenece a otra presentación, genera un código interno.
5. El sistema guarda la presentación.
6. El sistema muestra la equivalencia (1 caja = 12 UND) y permite imprimir la etiqueta del código.

**Reglas de negocio.** RN-01, RN-05.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| nombre\_presentacion | string | Sí | Unidad, caja x12, bulto 50 kg, rollo 100 m. |
| codigo\_barras | string | No | Código de fabricante; vacío genera código interno. |
| factor\_conversion | decimal(12,3) | Sí | Cantidad de unidad base por presentación; mayor que 0. |
| admite\_fraccion | booleano | Sí | Si se vende en cantidades no enteras; falso si la unidad base es discreta. |

**Criterios de aceptación**

- Una presentación válida queda asociada al producto con su factor.
- Un código de fabricante repetido genera un código interno EAN-13 con prefijo 20 y dígito de control válido.
- 3 cajas x12 se convierten en 36 UND.

#### RF-PRO-003 — Buscar y consultar productos

**Descripción.** El sistema deberá buscar productos por código de barras, nombre, marca o categoría y mostrar existencias por estado y por lote.

**Actor.** Todos los roles.

**Prioridad.** Alta.

**Precondiciones.** Usuario autenticado.

**Flujo principal**

1. El actor introduce o escanea un criterio.
2. El sistema busca coincidencias.
3. El sistema muestra presentaciones, precio vigente y existencias disponible, reservada y dañada.
4. El actor abre el detalle con lotes, costos (según permiso) y vencimientos.

**Reglas de negocio.** RN-13, RN-27.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| criterio | string | Sí | Código, nombre, marca o categoría. |
| estado | enumeración | No | Disponible, reservada o dañada. |

**Criterios de aceptación**

- Un código registrado devuelve exactamente su presentación.
- Las existencias mostradas coinciden con la suma de movimientos y cambios de estado.
- Un vendedor no ve costos.

#### RF-TER-001 — Registrar clientes y proveedores

**Descripción.** El sistema deberá registrar terceros como personas naturales o jurídicas, con rol de cliente, proveedor o ambos.

**Actor.** Vendedor (clientes), comprador (proveedores), gerente o administrador.

**Prioridad.** Crítica.

**Precondiciones.** Usuario con permiso de terceros.

**Flujo principal**

1. El actor selecciona el tipo de persona.
2. El sistema presenta los campos que corresponden.
3. El actor introduce identificación, nombres o razón social y contacto.
4. El sistema valida obligatoriedad, formato, dígito de verificación del NIT y unicidad.
5. El sistema guarda el tercero y lo audita.
6. El sistema confirma el registro.

**Reglas de negocio.** RN-26.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| tipo\_persona | enumeración | Sí | Natural o jurídica. |
| tipo\_identificacion | enumeración | Sí | CC (cédula) o NIT. |
| numero\_identificacion | string | Sí | Único; NIT con dígito de verificación. |
| nombre / apellido | string | Condicional | Obligatorios para persona natural. |
| razon\_social | string | Condicional | Obligatoria para persona jurídica. |
| direccion | string | Sí | Dirección de contacto. |
| telefono | string | Sí | 7 a 10 dígitos. |
| correo | string | No | Formato válido; necesario para enviar facturas por correo. |
| es\_cliente / es\_proveedor | booleano | Sí | Al menos uno verdadero. |

**Criterios de aceptación**

- Se registra una persona natural con cédula, nombre y apellido, y una jurídica con NIT y razón social.
- Un mismo tercero puede marcarse como cliente y proveedor.
- Se rechaza una identificación duplicada o un NIT con dígito de verificación incorrecto.
- El tercero Consumidor final existe desde la instalación y no puede editarse ni eliminarse.

#### RF-CFG-001 — Configurar empresa, impuestos y catálogos

**Descripción.** El sistema deberá permitir configurar los datos de la empresa que aparecen en los documentos, las tarifas de IVA, los prefijos de numeración y los catálogos de marcas, categorías, unidades y motivos.

**Actor.** Administrador.

**Prioridad.** Alta.

**Precondiciones.** Administrador autenticado.

**Flujo principal**

1. El administrador abre la configuración.
2. El sistema muestra los valores vigentes.
3. El administrador modifica un valor o agrega un elemento a un catálogo.
4. El sistema valida el dato.
5. El sistema guarda el cambio y lo audita.
6. El sistema aplica el cambio a los documentos nuevos, nunca a los ya emitidos.

**Reglas de negocio.** RN-08, RN-11, RN-28.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| nit\_empresa / razon\_social | string | Sí | Datos del emisor de facturas. |
| direccion / telefono / correo | string | Sí | Contacto del emisor. |
| tarifas\_iva | lista | Sí | Valores vigentes: 0, 5 y 19 %. |
| prefijos | lista | Sí | OC, DI, FV, NC, DP, AB, AJ. |
| motivos | lista | Sí | Motivos de daño, ajuste, baja y devolución. |

**Criterios de aceptación**

- Una factura nueva muestra los datos vigentes de la empresa.
- Un cambio de tarifa no altera facturas ya emitidas.
- Un elemento de catálogo en uso no se elimina; solo se desactiva.

#### RF-PRE-001 — Gestionar costos, precios e historial

**Descripción.** El sistema deberá calcular el precio de venta de cada presentación como el costo de la última compra incrementado por el porcentaje del producto, permitir fijarlo a mano y conservar el historial de costos y precios.

**Actor.** Sistema (cálculo automático); gerente o administrador (precio manual).

**Prioridad.** Alta.

**Precondiciones.** Producto con porcentaje de incremento.

**Flujo principal**

1. Al confirmarse una recepción, el sistema registra el costo unitario en el historial de costos.
2. El sistema calcula el precio base sin IVA y lo redondea al peso.
3. Si la presentación no tiene precio manual fijado, el sistema publica el nuevo precio con vigencia inmediata.
4. El gerente puede fijar un precio manual con motivo.
5. El sistema guarda cada cambio en el historial con origen (calculado o manual), usuario y fecha.

**Reglas de negocio.** RN-06, RN-08, RN-20.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| presentacion | referencia | Sí | Presentación a la que aplica el precio. |
| precio\_manual | entero (COP) | Condicional | Solo para precio manual; mayor que 0. |
| motivo | string | Condicional | Obligatorio para precio manual. |
| liberar\_manual | booleano | No | Vuelve al precio calculado. |

**Criterios de aceptación**

- Con costo 10.000 y 30 %, el precio base es 13.000 y el precio con IVA 19 % es 15.470.
- Una nueva compra no cambia un precio manual fijado.
- El historial conserva valor anterior, valor nuevo, origen, fecha y usuario.

### 3.2 Compras, ventas y devoluciones

#### RF-COM-001 — Registrar orden de compra

**Descripción.** El sistema deberá registrar órdenes de compra a proveedores y seguir su estado hasta la recepción total o la cancelación.

**Actor.** Comprador, gerente o administrador.

**Prioridad.** Alta.

**Precondiciones.** Proveedor y presentaciones registrados.

**Flujo principal**

1. El actor selecciona el proveedor.
2. El actor agrega líneas con presentación, cantidad y costo pactado.
3. El sistema valida cantidades y costos y calcula totales con IVA.
4. El actor guarda la orden como borrador o la emite.
5. El sistema asigna el consecutivo OC y la deja en estado Emitida.
6. El sistema actualiza el estado a Recibida parcial o Recibida según las recepciones (RF-COM-002).

**Reglas de negocio.** RN-06, RN-08, RN-11, RN-21.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| proveedor | referencia | Sí | Tercero con rol proveedor. |
| fecha\_esperada | fecha | No | Entrega prevista. |
| presentacion | referencia | Sí | Por línea. |
| cantidad | decimal(12,3) | Sí | Mayor que 0. |
| costo\_unitario | entero (COP) | Sí | Sin IVA, por presentación. |

**Criterios de aceptación**

- Una orden emitida tiene consecutivo OC único y no modifica existencias.
- Una orden emitida solo se cancela si no tiene recepciones.
- El estado cambia a Recibida cuando todas las líneas están completas.

#### RF-COM-002 — Recibir mercancía y generar documento de ingreso

**Descripción.** El sistema deberá registrar la recepción total o parcial de una orden de compra, o una compra directa sin orden, crear un lote por línea, aumentar la existencia disponible y generar un documento de ingreso numerado.

**Actor.** Comprador o jefe de inventario.

**Prioridad.** Crítica.

**Precondiciones.** Proveedor registrado; orden emitida (si aplica); período abierto.

**Flujo principal**

1. El actor selecciona una orden pendiente o elige compra directa con proveedor.
2. El sistema carga las cantidades pendientes de la orden, si existe.
3. El actor confirma cantidades recibidas, costo, número de factura del proveedor y fecha de vencimiento por línea cuando el producto la controla.
4. El sistema valida que lo recibido no supere lo pendiente y convierte a unidad base.
5. En una sola transacción, el sistema crea los lotes, aumenta la existencia disponible, actualiza costo y precio (RF-PRE-001) y audita la operación.
6. El sistema genera el documento de ingreso DI y lo muestra para imprimir.

**Reglas de negocio.** RN-02, RN-05, RN-06, RN-11, RN-18, RN-20, RN-21, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| orden\_compra | referencia | No | Vacío en compra directa. |
| proveedor | referencia | Sí | Tomado de la orden o elegido. |
| factura\_proveedor | string | Sí | Número del soporte del proveedor. |
| presentacion | referencia | Sí | Por línea. |
| cantidad\_recibida | decimal(12,3) | Sí | Mayor que 0 y menor o igual a lo pendiente. |
| costo\_unitario | entero (COP) | Sí | Sin IVA, por presentación. |
| fecha\_vencimiento | fecha | Condicional | Obligatoria si el producto controla vencimiento; posterior a hoy. |

**Criterios de aceptación**

- Recibir 60 de una orden de 100 deja la orden en Recibida parcial con 40 pendientes.
- La existencia disponible aumenta exactamente en la cantidad convertida, una sola vez.
- Si falla cualquier línea, no se guarda el documento, ningún lote ni ningún cambio de existencia.

#### RF-VEN-001 — Registrar venta y generar factura

**Descripción.** El sistema deberá registrar una venta de contado, calcular descuentos, IVA y redondeo, registrar los pagos, consumir lotes según RN-19 y generar una factura numerada.

**Actor.** Vendedor, gerente o administrador.

**Prioridad.** Crítica.

**Precondiciones.** Existencia disponible o reservada para el cliente; período abierto.

**Flujo principal**

1. El actor selecciona el cliente o usa Consumidor final.
2. El actor agrega líneas escaneando o buscando presentaciones e indica cantidades y descuentos.
3. El sistema muestra precios vigentes y calcula subtotal, descuentos, IVA por tarifa y total.
4. El sistema verifica existencia vendible sin generar negativos ni usar lotes vencidos o dañados.
5. El actor registra uno o varios pagos; si hay efectivo, el sistema aplica el redondeo a múltiplo de 50 y calcula el cambio.
6. En una sola transacción, el sistema consume los lotes, registra el costo de venta, calcula fechas de garantía, genera la factura FV y la muestra para imprimir o enviar.

**Reglas de negocio.** RN-02 a RN-11, RN-15, RN-18, RN-19, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| cliente | referencia | Sí | Cliente o Consumidor final. |
| presentacion | referencia | Sí | Por línea. |
| cantidad | decimal(12,3) | Sí | Mayor que 0; entera si la presentación no admite fracción. |
| descuento\_pct | decimal(5,2) | No | 0 a 10 % para vendedor; mayor requiere gerente. |
| reserva | referencia | No | Reserva del mismo cliente que se consume. |
| medio\_pago / valor | enumeración / entero | Sí | Efectivo, débito, crédito o transferencia; la suma cubre el total. |
| valor\_recibido | entero (COP) | Condicional | Para efectivo, para calcular el cambio. |

**Criterios de aceptación**

- Una venta confirmada produce una factura FV con número único y descuenta la existencia una sola vez.
- Con un lote de 5 UND a 1.000 que vence en junio y otro de 5 a 1.200 que vence en abril, vender 7 consume primero el de abril y registra costo 8.000.
- Un total de 12.374 pagado en efectivo se cobra como 12.350 y registra un ajuste de redondeo de −24.
- Se impide vender más de lo disponible y vender lotes vencidos.

#### RF-VEN-002 — Anular factura

**Descripción.** El sistema deberá permitir anular una factura emitida el mismo día y sin devoluciones, revirtiendo existencias y lotes.

**Actor.** Gerente o administrador.

**Prioridad.** Media.

**Precondiciones.** Factura del día actual, sin devoluciones; período abierto.

**Flujo principal**

1. El actor busca la factura.
2. El sistema verifica fecha y ausencia de devoluciones.
3. El actor indica el motivo de anulación.
4. El sistema devuelve las cantidades a los mismos lotes y estados de origen.
5. El sistema marca la factura como Anulada, conservando su número, y audita la operación.

**Reglas de negocio.** RN-11, RN-12.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| factura | referencia | Sí | Factura a anular. |
| motivo | string | Sí | Razón de la anulación. |

**Criterios de aceptación**

- Tras anular, las existencias y los lotes quedan como antes de la venta.
- Una factura de un día anterior no puede anularse; debe usarse RF-DEV-001.

#### RF-DEV-001 — Registrar devolución de cliente

**Descripción.** El sistema deberá registrar la devolución total o parcial de una factura, reingresar la mercancía al lote de origen con su costo original y generar una nota crédito.

**Actor.** Vendedor, gerente o administrador.

**Prioridad.** Alta.

**Precondiciones.** Factura emitida hace 30 días o menos, o línea dentro de su garantía; período abierto.

**Flujo principal**

1. El actor busca la factura.
2. El sistema muestra las líneas con la cantidad aún devolvible.
3. El actor indica cantidades, motivo y si la mercancía vuelve en buen estado o defectuosa.
4. El sistema reingresa la cantidad a los lotes consumidos por esa línea: a disponible si está en buen estado, a dañada si está defectuosa.
5. El sistema genera la nota crédito NC con el valor a reintegrar y el medio de reintegro.

**Reglas de negocio.** RN-02, RN-03, RN-11, RN-22, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| factura | referencia | Sí | Factura de origen. |
| linea / cantidad | referencia / decimal | Sí | Menor o igual a vendido menos devuelto. |
| estado\_mercancia | enumeración | Sí | Buen estado o defectuosa. |
| motivo | catálogo | Sí | Cambio de opinión, defecto, garantía, error de despacho. |
| medio\_reintegro | enumeración | Sí | Efectivo, reversión a tarjeta o transferencia. |

**Criterios de aceptación**

- No se puede devolver más de lo vendido en la línea, sumando devoluciones previas.
- La mercancía reingresa con el mismo costo con que salió.
- La nota crédito refleja el precio, descuento e IVA de la factura original.

#### RF-DEV-002 — Registrar devolución a proveedor

**Descripción.** El sistema deberá registrar la salida de mercancía hacia el proveedor que la vendió, descontando los lotes de la recepción de origen a su costo, y generar un documento de devolución.

**Actor.** Comprador, gerente o administrador.

**Prioridad.** Media.

**Precondiciones.** Recepción de origen con saldo en sus lotes; período abierto.

**Flujo principal**

1. El actor busca el documento de ingreso.
2. El sistema muestra sus lotes con saldo disponible y dañado.
3. El actor indica cantidades, estado de origen y motivo.
4. El sistema valida saldos y descuenta los lotes.
5. El sistema genera el documento DP y lo audita.

**Reglas de negocio.** RN-02, RN-03, RN-11, RN-23, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| documento\_ingreso | referencia | Sí | Recepción de origen. |
| lote / cantidad | referencia / decimal | Sí | Menor o igual al saldo del lote en el estado elegido. |
| estado\_origen | enumeración | Sí | Disponible o dañada. |
| motivo | catálogo | Sí | Defecto, error de pedido, vencimiento próximo. |

**Criterios de aceptación**

- La devolución descuenta solo los lotes de la recepción indicada.
- El valor del documento es la cantidad por el costo del lote.

### 3.3 Estados, bajas, conteos, ajustes y cierres

#### RF-EST-001 — Reservar y liberar existencias

**Descripción.** El sistema deberá permitir reservar cantidad disponible para un cliente hasta por 7 días y liberarla al cancelarse, al vencer el plazo o al consumirse en una venta.

**Actor.** Vendedor, gerente o administrador; Sistema (vencimiento).

**Prioridad.** Media.

**Precondiciones.** Cliente identificado (no Consumidor final); existencia disponible suficiente.

**Flujo principal**

1. El actor selecciona cliente, presentación y cantidad.
2. El sistema verifica la existencia disponible.
3. El actor indica la fecha límite, de 1 a 7 días.
4. El sistema traslada la cantidad de disponible a reservada y audita el cambio.
5. Al cancelarse o vencer, el sistema la devuelve a disponible; al venderse al mismo cliente, la descuenta de reservada.
6. El sistema muestra los saldos actualizados.

**Reglas de negocio.** RN-03, RN-14, RN-15.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| cliente | referencia | Sí | Cliente identificado. |
| presentacion | referencia | Sí | Producto reservado. |
| cantidad | decimal(12,3) | Sí | Mayor que 0 y menor o igual al disponible. |
| fecha\_limite | fecha | Sí | Máximo 7 días desde hoy. |

**Criterios de aceptación**

- Una reserva reduce disponible y aumenta reservada en la misma cantidad; la existencia total no cambia.
- Una reserva vencida vuelve a disponible en la siguiente ejecución diaria del Sistema.
- Se rechaza una fecha límite mayor a 7 días.

#### RF-EST-002 — Clasificar existencias dañadas

**Descripción.** El sistema deberá trasladar cantidad de un lote de disponible a dañada con un motivo, devolverla a disponible con autorización, y trasladar automáticamente los lotes vencidos a dañada.

**Actor.** Jefe de inventario; gerente o administrador (retorno); Sistema (vencidos).

**Prioridad.** Media.

**Precondiciones.** Lote con saldo disponible; motivo en el catálogo.

**Flujo principal**

1. El actor selecciona presentación, lote y cantidad.
2. El actor selecciona el motivo.
3. El sistema valida saldo y permisos.
4. El sistema traslada la cantidad a dañada y audita el cambio con saldos antes y después.
5. Cada día, el Sistema traslada a dañada, con motivo Vencido, el saldo disponible y reservado de los lotes vencidos, y cancela las reservas afectadas.

**Reglas de negocio.** RN-03, RN-14, RN-16, RN-18.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| lote | referencia | Sí | Lote afectado. |
| cantidad | decimal(12,3) | Sí | Menor o igual al saldo del lote en el estado de origen. |
| direccion | enumeración | Sí | Disponible → dañada o dañada → disponible. |
| motivo | catálogo | Sí | Rotura, defecto de fábrica, humedad, vencido, otro. |
| observacion | string | No | Detalle. |

**Criterios de aceptación**

- La cantidad dañada deja de estar disponible para venta.
- Un jefe de inventario no puede devolver dañados a disponible.
- Un lote vencido ayer aparece en dañada hoy, con motivo Vencido y usuario Sistema.

#### RF-EST-003 — Dar de baja existencias dañadas

**Descripción.** El sistema deberá registrar la salida definitiva de existencias dañadas, con su destino (reciclaje o disposición final), mediante un acta de baja aprobada.

**Actor.** Jefe de inventario (propone); gerente o administrador (aprueba).

**Prioridad.** Media.

**Precondiciones.** Saldo dañado en los lotes; período abierto.

**Flujo principal**

1. El proponente selecciona lotes dañados, cantidades y destino de cada línea.
2. El sistema guarda el acta en estado Propuesta.
3. Un aprobador distinto revisa el acta y la aprueba o rechaza.
4. Al aprobarse, el sistema descuenta las cantidades de dañada y registra la pérdida al costo de cada lote.
5. El sistema asigna el consecutivo AB y permite imprimir el acta.

**Reglas de negocio.** RN-02, RN-03, RN-11, RN-17, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| lote / cantidad | referencia / decimal | Sí | Menor o igual al saldo dañado del lote. |
| destino | enumeración | Sí | Reciclaje o disposición final. |
| gestor | string | No | Empresa o punto que recibe el residuo. |
| observacion | string | No | Detalle de la disposición. |

**Criterios de aceptación**

- Un acta propuesta no altera existencias.
- El proponente no puede aprobar su propia acta.
- El valor de la pérdida es la suma de cantidad por costo de cada lote.

#### RF-CON-001 — Registrar conteo físico

**Descripción.** El sistema deberá registrar conteos físicos totales o parciales, comparar lo contado con lo registrado y proponer los ajustes de las diferencias.

**Actor.** Jefe de inventario.

**Prioridad.** Alta.

**Precondiciones.** Período abierto.

**Flujo principal**

1. El actor crea un conteo total o parcial (por categoría o productos).
2. El sistema genera la lista de productos, y de lotes cuando controlan vencimiento, sin mostrar la cantidad registrada (conteo a ciegas).
3. El actor captura, escaneando o escribiendo, la cantidad en buen estado y la dañada.
4. Al cerrar el conteo, el sistema calcula la diferencia contra la existencia registrada en el momento de cada captura.
5. El sistema genera una propuesta de ajuste por cada diferencia distinta de cero (RF-AJU-001).

**Reglas de negocio.** RN-24.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| tipo | enumeración | Sí | Total o parcial. |
| alcance | lista | Condicional | Categorías o productos en conteo parcial. |
| producto / lote | referencia | Sí | Elemento contado. |
| cantidad\_buen\_estado | decimal(12,3) | Sí | Mayor o igual a 0, en unidad base. |
| cantidad\_danada | decimal(12,3) | Sí | Mayor o igual a 0. |

**Criterios de aceptación**

- El contador no ve la existencia registrada mientras cuenta.
- Un conteo cerrado no altera existencias hasta aprobar sus ajustes.
- Cada diferencia distinta de cero genera exactamente una propuesta de ajuste.

#### RF-AJU-001 — Aprobar y aplicar ajuste de inventario

**Descripción.** El sistema deberá aplicar ajustes de existencias, originados en un conteo o registrados manualmente con motivo, después de la aprobación de un usuario distinto al proponente.

**Actor.** Jefe de inventario (propone); gerente o administrador (aprueba).

**Prioridad.** Alta.

**Precondiciones.** Propuesta de ajuste registrada; período abierto.

**Flujo principal**

1. El aprobador revisa las propuestas pendientes con su diferencia y su valor.
2. El aprobador aprueba o rechaza cada propuesta con observación.
3. En un ajuste positivo, el sistema crea un lote de ajuste al costo de la última compra.
4. En un ajuste negativo, el sistema consume lotes según RN-19 en el estado afectado.
5. El sistema asigna el consecutivo AJ y audita la operación.

**Reglas de negocio.** RN-02, RN-03, RN-11, RN-19, RN-24, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| producto / lote | referencia | Sí | Elemento ajustado. |
| estado | enumeración | Sí | Disponible o dañada. |
| diferencia | decimal(12,3) | Sí | Positiva o negativa, distinta de 0. |
| motivo | catálogo | Sí | Diferencia de conteo, error de registro, pérdida, hallazgo. |
| decision | enumeración | Sí | Aprobado o rechazado. |

**Criterios de aceptación**

- Un ajuste negativo mayor al saldo es rechazado.
- El proponente no puede aprobar su propio ajuste.
- Tras aplicar todos los ajustes de un conteo total, la existencia registrada coincide con la contada.

#### RF-CIE-001 — Ejecutar cierre mensual

**Descripción.** El sistema deberá cerrar un mes guardando saldos de cantidad y valor por lote y estado, después de verificar un conteo físico total del mes con todos sus ajustes resueltos.

**Actor.** Gerente o administrador.

**Prioridad.** Alta.

**Precondiciones.** El mes anterior está cerrado; el mes a cerrar terminó.

**Flujo principal**

1. El actor selecciona el mes a cerrar.
2. El sistema verifica que exista un conteo total con fecha en ese mes y sin ajustes pendientes.
3. El sistema muestra pendientes informativos: órdenes abiertas, reservas activas, actas sin aprobar.
4. El actor confirma el cierre.
5. El sistema guarda los saldos por lote y estado, bloquea el período y genera la constancia.

**Reglas de negocio.** RN-24, RN-25.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| periodo | año-mes | Sí | Mes a cerrar. |
| observaciones | string | No | Notas del cierre. |

**Criterios de aceptación**

- Sin conteo total del mes, o con ajustes pendientes, el cierre es rechazado.
- Cualquier operación con fecha en un mes cerrado es rechazada.
- Los saldos del cierre coinciden con el reporte de existencias valorizadas a esa fecha.

#### RF-CIE-002 — Reabrir período cerrado

**Descripción.** El sistema deberá permitir reabrir el último mes cerrado, con motivo, y exigir un nuevo cierre.

**Actor.** Administrador.

**Prioridad.** Media.

**Precondiciones.** El mes es el último cerrado.

**Flujo principal**

1. El administrador selecciona el último mes cerrado.
2. El administrador escribe el motivo de reapertura.
3. El sistema marca el período como Reabierto, conserva la constancia anterior y audita la operación.
4. Al cerrarse de nuevo, el sistema reemplaza los saldos y genera una nueva constancia.

**Reglas de negocio.** RN-25, RN-28.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| periodo | año-mes | Sí | Último mes cerrado. |
| motivo | string | Sí | Al menos 20 caracteres. |

**Criterios de aceptación**

- Solo puede reabrirse el último mes cerrado y solo por un administrador.
- El historial conserva todas las constancias de cierre y reapertura.

### 3.4 Consultas, seguridad, migración e integración

#### RF-MOV-001 — Consultar kardex e historial de movimientos

**Descripción.** El sistema deberá mostrar el kardex de un producto (entradas, salidas, cambios de estado, saldo y costo) y el historial de movimientos por período, con su documento de origen y responsable.

**Actor.** Todos los roles; el vendedor consulta sin costos (matriz 2.2).

**Prioridad.** Alta.

**Precondiciones.** Existen movimientos registrados.

**Flujo principal**

1. El actor selecciona filtros de producto, tipo de documento y rango de fechas.
2. El sistema presenta los movimientos paginados con saldo acumulado.
3. El actor abre el detalle de un movimiento.
4. El sistema muestra documento de origen, lotes afectados, cantidades, costos y responsable.

**Reglas de negocio.** RN-02, RN-12, RN-27.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| producto | referencia | No | Obligatorio para el kardex. |
| tipo\_documento | enumeración | No | DI, FV, NC, DP, AB, AJ, cambio de estado. |
| fecha\_desde / fecha\_hasta | fecha | No | Rango del filtro. |

**Criterios de aceptación**

- El saldo final del kardex es igual a la existencia registrada del producto.
- Las anulaciones aparecen enlazadas a la factura que revierten.
- Un vendedor ve cantidades y saldos, pero no costos unitarios ni totales.

#### RF-REP-001 — Consultar reportes

**Descripción.** El sistema deberá generar reportes de existencias valorizadas a una fecha, compras, ventas, utilidad bruta, productos próximos a vencer, dañados y bajas, diferencias de conteo, reservas activas e historial de precios.

**Actor.** Gerente, administrador, auditor; demás roles según matriz.

**Prioridad.** Alta.

**Precondiciones.** Existen datos para el período consultado.

**Flujo principal**

1. El actor selecciona el reporte y sus filtros.
2. El sistema calcula los resultados.
3. El sistema presenta resultados y totales.
4. El actor exporta a Excel o PDF (RF-DOC-001).

**Reglas de negocio.** RN-19, RN-25, RN-27.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| tipo\_reporte | catálogo | Sí | Reporte a generar. |
| fecha\_corte / rango | fecha | Sí | Corte o período. |
| categoria / marca / tercero / usuario | referencia | No | Filtros adicionales. |
| dias\_vencimiento | entero | No | Horizonte de próximos a vencer; 30 por defecto. |

**Criterios de aceptación**

- Cada reporte coincide con un conjunto de datos de referencia calculado a mano.
- Las existencias valorizadas a fin de un mes cerrado coinciden con los saldos de su cierre.
- Utilidad bruta = ventas netas sin IVA − costo de venta de los lotes consumidos.

#### RF-SEG-001 — Gestionar usuarios, roles y sesión

**Descripción.** El sistema deberá autenticar usuarios, permitir al administrador crearlos, asignarles roles y desactivarlos, y aplicar la matriz de permisos en cada operación.

**Actor.** Administrador; todos los usuarios (inicio de sesión).

**Prioridad.** Crítica.

**Precondiciones.** Existe al menos un administrador creado en la instalación.

**Flujo principal**

1. El usuario inicia sesión con usuario y contraseña.
2. El sistema valida credenciales y emite una sesión con sus permisos.
3. El administrador crea o selecciona un usuario y asigna roles.
4. El sistema valida y guarda los cambios.
5. El sistema aplica los permisos desde el siguiente acceso.

**Reglas de negocio.** RN-27, RN-28.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| usuario | string | Sí | Único. |
| nombre\_completo | string | Sí | Nombre de la persona. |
| correo | string | Sí | Único. |
| contrasena | string | Sí | Mínimo 8 caracteres con letras y números. |
| roles | lista | Sí | Al menos un rol. |
| activo | booleano | Sí | Estado de la cuenta. |

**Criterios de aceptación**

- Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.
- Un usuario sin permiso no ejecuta la operación, ni por interfaz ni por API.
- Los usuarios no se eliminan, solo se desactivan; una cuenta desactivada no inicia sesión.

#### RF-SEG-002 — Registrar y consultar auditoría

**Descripción.** El sistema deberá registrar cada creación, modificación, anulación, aprobación, cambio de estado e inicio de sesión, conservarlo 24 meses y permitir consultarlo.

**Actor.** Administrador, gerente o auditor; Sistema (depuración).

**Prioridad.** Crítica.

**Precondiciones.** Usuario con permiso de auditoría.

**Flujo principal**

1. El actor filtra por usuario, entidad, acción o fechas.
2. El sistema presenta los registros paginados.
3. El actor abre un registro.
4. El sistema muestra valores antes y después, dirección IP y fecha.
5. Cada mes, el Sistema elimina los registros con más de 24 meses y deja constancia de la depuración.

**Reglas de negocio.** RN-02, RN-28.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| usuario | referencia | No | Filtro por autor. |
| entidad | catálogo | No | Producto, documento, lote, precio, usuario, período. |
| accion | enumeración | No | Crear, modificar, anular, aprobar, cambiar estado, iniciar sesión. |
| fecha\_desde / fecha\_hasta | fecha | No | Rango del filtro. |

**Criterios de aceptación**

- Cada operación auditable genera exactamente un registro en la misma transacción.
- Nadie puede modificar ni borrar un registro de menos de 24 meses.
- Un registro de 25 meses desaparece en la siguiente depuración mensual.

#### RF-MIG-001 — Migrar datos desde Excel

**Descripción.** El sistema deberá importar marcas, categorías, productos, presentaciones, terceros y existencias iniciales con costo desde un libro de Excel con plantilla definida, validando cada fila.

**Actor.** Administrador.

**Prioridad.** Alta.

**Precondiciones.** Sistema sin movimientos de inventario; plantilla descargada desde el sistema.

**Flujo principal**

1. El actor carga el archivo .xlsx.
2. El sistema valida estructura y contenido de cada fila de cada hoja.
3. El sistema presenta el resumen de filas válidas e inválidas con hoja, fila y causa.
4. El actor confirma la importación solo si no hay errores.
5. El sistema carga todo en una sola transacción; cada existencia inicial crea un lote de apertura.
6. El sistema entrega el resumen de conciliación.

**Reglas de negocio.** RN-01, RN-26, RN-29.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| archivo | .xlsx | Sí | Hojas: Marcas, Categorías, Productos, Presentaciones, Terceros, Existencias. |
| fecha\_corte | fecha | Sí | Fecha de los saldos iniciales y de los lotes de apertura. |

**Criterios de aceptación**

- Un archivo con alguna fila inválida no carga nada y reporta cada error.
- El número de productos y la suma de cantidad y valor de existencias cargadas coinciden con el archivo.

#### RF-LEC-001 — Leer código de barras

**Descripción.** El sistema deberá aceptar la lectura de un escáner USB o Bluetooth en modo teclado en ventas, recepciones, conteos y consultas.

**Actor.** Vendedor, comprador y jefe de inventario.

**Prioridad.** Alta.

**Precondiciones.** Lector conectado y configurado para enviar Enter al final.

**Flujo principal**

1. El actor escanea el código con el foco en el campo de captura.
2. El sistema busca la presentación con ese código.
3. El sistema agrega la presentación o suma 1 a la cantidad si ya está en el documento.
4. Si el código no existe, el sistema emite un aviso visual y sonoro sin alterar el documento.

**Reglas de negocio.** RN-01.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| codigo\_leido | string | Sí | Código entregado por el lector. |

**Criterios de aceptación**

- Escanear dos veces el mismo código deja una línea con cantidad 2.
- Un código desconocido no modifica el documento en curso.

#### RF-DOC-001 — Imprimir, enviar y exportar documentos

**Descripción.** El sistema deberá generar en PDF las órdenes de compra, documentos de ingreso, facturas, notas crédito, devoluciones, actas de baja y constancias de cierre; imprimirlos en tamaño carta o tirilla de 80 mm; enviarlos por correo al tercero; y exportar reportes a Excel y CSV.

**Actor.** Todos los roles según el documento.

**Prioridad.** Alta.

**Precondiciones.** Documento confirmado; servidor de correo configurado para el envío.

**Flujo principal**

1. El actor abre un documento confirmado o un reporte.
2. El actor elige imprimir, descargar PDF, enviar por correo o exportar.
3. El sistema genera el archivo con los datos de la empresa vigentes al emitir el documento.
4. El sistema imprime, descarga o envía y registra el envío.

**Reglas de negocio.** RN-11, RN-12.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| documento | referencia | Sí | Documento o reporte. |
| formato | enumeración | Sí | PDF carta, PDF tirilla, Excel o CSV. |
| correo\_destino | string | Condicional | Por defecto el del tercero. |

**Criterios de aceptación**

- La factura impresa muestra emisor, cliente, líneas, IVA por tarifa, redondeo, total, pagos y cambio.
- Un documento anulado se imprime con la marca ANULADO.

#### RF-INT-001 — Exponer API para sistemas externos

**Descripción.** El sistema deberá exponer una API REST autenticada para consultar catálogo, precios y existencias, y registrar ventas de canales externos.

**Actor.** Sistema externo; administrador (credenciales).

**Prioridad.** Baja.

**Precondiciones.** Credencial de API emitida por el administrador.

**Flujo principal**

1. El sistema externo envía una solicitud con su token.
2. El sistema valida token, permisos y datos.
3. El sistema ejecuta la operación con las mismas reglas que la interfaz.
4. El sistema responde en JSON y audita la operación.

**Reglas de negocio.** RN-27, RN-28; una venta externa sigue todas las reglas de RF-VEN-001.

**Datos de entrada**

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| token | string | Sí | Credencial del sistema externo. |
| clave\_idempotencia | string | Condicional | Obligatoria en operaciones de escritura. |
| datos | JSON | Sí | Según la especificación OpenAPI. |

**Criterios de aceptación**

- Repetir una venta con la misma clave de idempotencia no crea una segunda factura.
- Una solicitud sin token válido recibe 401.

### 3.5 Funcionalidades excluidas

| ID | Funcionalidad | Motivo |
| --- | --- | --- |
| RF-OFF-001 | Operar sin internet | Decisión del interesado. |
| RF-UBI-001 | Trasladar entre bodegas | Existe una sola bodega. |
| RF-FE-001 | Facturación electrónica DIAN | No requerida (P-01). |
| RF-CAR-001 | Ventas a crédito y cartera | Simplificación de alcance (P-07). |
| RF-ALR-001 | Alertas automáticas por correo | Cubierto por el reporte de próximos a vencer. |

## 4. Ciclo de vida de las existencias

Cada unidad entra por un lote, cambia de estado sin salir de la bodega y sale solo por un documento aprobado (RN-02, RN-14).

&#91;embedded content: ciclo de vida de las existencias · 4 entradas, 3 estados, 4 salidas\]

La venta toma de disponible o de la reserva del mismo cliente; el ajuste negativo y la devolución a proveedor también pueden salir de dañada; la devolución de cliente defectuosa entra a dañada; los lotes vencidos pasan cada día a dañada, incluso desde reservada.

## 5. Requisitos no funcionales

Se definen 17 requisitos de calidad según ISO/IEC 25010:2023, con criterios medibles aprobados por el interesado. El volumen de diseño es 100.000 presentaciones, 5.000 líneas de movimiento por día y 5 años de histórico en línea (cerca de 9 millones de líneas).

| ID | Característica (subcaracterística) | Requisito | Criterio de aceptación | Prioridad |
| --- | --- | --- | --- | --- |
| RNF-SEG-001 | Seguridad (autenticidad) | Todo acceso exige sesión autenticada; contraseñas con hash bcrypt o Argon2. | Toda ruta protegida responde 401 sin token; ninguna contraseña se guarda en texto plano. | Crítica |
| RNF-SEG-002 | Seguridad (confidencialidad) | Cada operación se autoriza según la matriz 2.2, también en el servidor. | 100 % de las pruebas de la matriz pasan por interfaz y por API. | Crítica |
| RNF-SEG-003 | Seguridad (responsabilidad) | Toda operación auditable registra usuario, fecha, IP, acción y valores antes y después. | Cada operación de prueba genera su registro; retención de 24 meses. | Crítica |
| RNF-SEG-004 | Seguridad (integridad) | Auditoría y documentos confirmados no se editan ni se borran. | Intentos por API y por SQL con el usuario de la aplicación son rechazados. | Crítica |
| RNF-SEG-005 | Seguridad (resistencia) | Protección frente a inyección SQL, XSS, CSRF y fuerza bruta. | Sin hallazgos altos en pruebas OWASP Top 10; bloqueo tras 5 intentos fallidos. | Alta |
| RNF-SEG-006 | Seguridad (confidencialidad) | Comunicación cifrada y sesiones con expiración. | HTTPS obligatorio; sesión expira tras 30 min de inactividad. | Alta |
| RNF-REN-001 | Eficiencia (comportamiento temporal) | Búsqueda por código y confirmación de venta rápidas con el volumen de diseño. | Percentil 95 menor a 1 s. | Alta |
| RNF-REN-002 | Eficiencia (comportamiento temporal) | Reportes mensuales y kardex de un producto con el volumen de diseño. | Percentil 95 menor a 10 s. | Media |
| RNF-REN-003 | Eficiencia (capacidad) | Usuarios simultáneos sin errores ni bloqueos de existencias. | 50 usuarios concurrentes en prueba de carga sin errores ni saldos negativos. | Alta |
| RNF-FIA-001 | Fiabilidad (tolerancia a fallos) | Cada documento y su efecto en existencias son atómicos. | Una falla simulada a mitad de operación no deja documentos ni saldos parciales. | Crítica |
| RNF-FIA-002 | Fiabilidad (recuperabilidad) | Copias de seguridad automáticas. | Copia diaria; restauración probada; RPO 24 h y RTO 4 h. | Alta |
| RNF-FIA-003 | Fiabilidad (disponibilidad) | Servicio disponible en horario comercial. | Disponibilidad mensual de 99 %. | Media |
| RNF-INT-001 | Capacidad de interacción (operabilidad) | Venta habitual con lector y teclado, sin ratón. | Usuario entrenado registra una venta de 5 líneas en menos de 60 s. | Alta |
| RNF-INT-002 | Capacidad de interacción (protección contra errores) | Validación antes de enviar, con mensaje por campo. | Cada regla de validación muestra campo y causa. | Media |
| RNF-INT-003 | Capacidad de interacción (inclusividad) | Interfaz en español con formatos colombianos. | Moneda como $ 12.350, fechas dd/mm/aaaa, zona horaria America/Bogota. | Media |
| RNF-COM-001 | Compatibilidad (interoperabilidad) | Lectores en modo teclado, impresoras de tirilla de 80 mm y API REST documentada. | Lector e impresora probados en Chrome y Edge; especificación OpenAPI publicada. | Alta |
| RNF-MAN-001 | Mantenibilidad (capacidad de prueba) | Pruebas automatizadas de las reglas de negocio. | Cobertura del backend mayor o igual a 70 %. | Media |

## 6. Reglas de negocio

Las 29 reglas están aprobadas. Las tributarias (RN-06 a RN-10) son una simplificación académica razonable para Colombia, no asesoría contable.

| ID | Regla |
| --- | --- |
| RN-01 | Cada presentación tiene un único código de barras. Si no trae código de fabricante, o este ya pertenece a otra presentación, el sistema genera un EAN-13 interno con prefijo 20 (rango GS1 de uso interno) y dígito de control. |
| RN-02 | Todo cambio de existencias se vincula a un documento y a un usuario. |
| RN-03 | Ningún saldo puede ser negativo: ni disponible, ni reservada, ni dañada, ni el de cada lote en cada estado. |
| RN-04 | Solo se vende existencia disponible, o la reservada para el mismo cliente. Nunca se vende existencia dañada ni de lotes vencidos. |
| RN-05 | La existencia se guarda en la unidad base del producto (UND, PAR, JGO, KG, G, LB, LTR, ML, GAL, MTR, CM, M2, M3) con hasta 3 decimales; las unidades discretas (UND, PAR, JGO) solo admiten enteros. |
| RN-06 | Todo valor monetario es un entero en pesos colombianos (COP). Cada cálculo (subtotal de línea, descuento, IVA, total) se redondea al entero más cercano, con las mitades hacia arriba. |
| RN-07 | El valor pagado en efectivo se redondea al múltiplo de $50 más cercano (las mitades, $25, hacia arriba); la diferencia se registra en la factura como ajuste por redondeo. Los pagos electrónicos no se redondean. |
| RN-08 | Cada producto tiene una tarifa de IVA (0 %, 5 % o 19 %). Costos y precios se guardan sin IVA; el IVA se calcula por línea sobre el valor después del descuento. |
| RN-09 | El descuento es un porcentaje por línea: hasta 10 % lo aplica el vendedor; más de 10 % y hasta 50 % requiere gerente o administrador. |
| RN-10 | Las ventas son de contado. Medios de pago: efectivo, tarjeta débito, tarjeta crédito y transferencia (incluye billeteras digitales). Una factura admite varios pagos cuya suma cubre el total. |
| RN-11 | Cada tipo de documento tiene numeración consecutiva sin saltos y prefijo propio: OC, DI, FV, NC, DP, AB, AJ. Un documento anulado conserva su número. |
| RN-12 | Un documento confirmado no se edita ni se borra. Una factura solo se anula el mismo día y sin devoluciones; después se corrige con una devolución. |
| RN-13 | Existe una sola bodega; la existencia de cada lote se desglosa en disponible, reservada y dañada. |
| RN-14 | Cambios de estado permitidos: disponible → reservada; reservada → disponible; disponible → dañada; reservada → dañada (solo por vencimiento); dañada → disponible (solo con autorización). No alteran la existencia total. |
| RN-15 | Una reserva dura de 1 a 7 días, la crea un vendedor para un cliente identificado y vence automáticamente. |
| RN-16 | Todo paso a dañada exige lote y motivo. |
| RN-17 | Solo se da de baja existencia dañada, mediante acta aprobada por un usuario distinto al proponente. Cada línea indica destino: reciclaje o disposición final conforme a la normativa ambiental vigente. La pérdida se valora al costo del lote. |
| RN-18 | Un producto con control de vencimiento exige fecha de vencimiento en cada lote; los lotes vencidos pasan a dañada a diario con motivo Vencido. Un producto con garantía registra en cada línea de factura la fecha fin de garantía = fecha de venta + plazo. |
| RN-19 | Orden de consumo de lotes: si el producto controla vencimiento, primero el lote que vence antes (FEFO) y, en empate, el de ingreso más antiguo; si no, el de ingreso más antiguo (PEPS). El costo de una salida es la suma de cantidad por costo de cada lote consumido. |
| RN-20 | El costo unitario en unidad base de una presentación es su costo de compra dividido por su factor. El precio base sin IVA de cada presentación = costo unitario de la última compra × factor × (1 + porcentaje / 100). Se recalcula en cada recepción, salvo que tenga precio manual fijado por gerente o administrador. |
| RN-21 | Una recepción puede venir de una orden de compra (total o parcial) o ser compra directa. Lo recibido acumulado no supera lo ordenado. Estados de la orden: Borrador, Emitida, Recibida parcial, Recibida, Cancelada. |
| RN-22 | Una devolución de cliente procede dentro de 30 días de la factura o dentro de la garantía; no supera lo vendido menos lo ya devuelto; reingresa a los lotes de origen con su costo original, a disponible o a dañada según el estado de la mercancía, y genera nota crédito. |
| RN-23 | Una devolución a proveedor sale de los lotes de la recepción de origen, desde disponible o dañada, al costo de esos lotes. |
| RN-24 | El cierre exige un conteo físico total del mes con todos sus ajustes resueltos. Un ajuste lo aprueba un usuario distinto al proponente; si es positivo crea un lote al costo de la última compra, si es negativo consume lotes según RN-19. |
| RN-25 | Los cierres son mensuales y en orden. Ninguna operación puede tener fecha en un mes cerrado. Solo el administrador reabre, solo el último mes cerrado y con motivo. |
| RN-26 | Persona natural: cédula, nombre y apellido. Persona jurídica: NIT con dígito de verificación y razón social. Dirección y teléfono son obligatorios; correo opcional. La identificación es única. Un tercero puede ser cliente y proveedor. El tercero Consumidor final (identificación 222222222222) viene predefinido y no se edita. |
| RN-27 | Cada operación exige el permiso del rol según la matriz 2.2; quien propone un ajuste o una baja no lo aprueba. |
| RN-28 | La auditoría se escribe en la misma transacción que la operación, es inmutable y se conserva 24 meses; una depuración mensual elimina lo más antiguo y deja constancia. |
| RN-29 | La migración solo se ejecuta con el sistema sin movimientos, es todo o nada, y cada existencia inicial crea un lote de apertura con el costo del archivo. |

## 7. Decisiones y preguntas resueltas

Las 19 preguntas de la versión 3 están resueltas: 8 con decisión directa del interesado, 4 parcialmente delegadas y 7 delegadas por completo al analista ("a conveniencia").

### 7.1 Decisiones de alcance iniciales

| # | Decisión | Fuente |
| --- | --- | --- |
| D-01 | Inventario de compras y ventas, ampliado con devoluciones, bajas y ajustes (P-06, P-18, P-19). | Contexto + respuestas |
| D-02 | Una sola bodega. | Contexto |
| D-03 | Varias marcas y presentaciones; un código de barras por presentación. | Contexto |
| D-04 | Fracciones y conversiones. | Contexto |
| D-05 | Estados disponible, reservada y dañada; sin negativos. | Contexto |
| D-06 | Compra genera documento de ingreso; venta genera factura. | Contexto |
| D-07 | Cierres mensuales; auditoría de todos los movimientos. | Contexto |
| D-08 | Precio = costo de compra + porcentaje por producto, con historial. | Contexto |
| D-09 | Sin operación fuera de línea. | Respuesta |
| D-10 | Costeo PEPS. | Respuesta |
| D-11 | Cambios de estado definidos por el analista. | Delegación |

### 7.2 Preguntas resueltas

| ID | Respuesta del interesado | Decisión adoptada | Reflejada en |
| --- | --- | --- | --- |
| P-01 | Factura en este sistema; sin facturación electrónica. | Factura interna con prefijo FV y datos de la empresa. | RF-VEN-001, RF-CFG-001 |
| P-02 | COP sin decimales; unidades según producto; redondeo al entero y a $50 en efectivo. | Montos enteros con redondeo hacia arriba en mitades; 13 unidades base; cantidades con 3 decimales; redondeo a $50 solo en efectivo. | RN-05, RN-06, RN-07 |
| P-03 | A conveniencia. | EAN-13 interno con prefijo 20 cuando falta o se repite el código de fabricante. | RN-01, RF-PRO-002 |
| P-04 | A conveniencia. | Reserva de 1 a 7 días, creada por vendedor, con vencimiento automático. | RN-15, RF-EST-001 |
| P-05 | No vender vencidos; ideal por vencimiento más próximo. | FEFO para productos con vencimiento, PEPS para el resto; vencidos pasan a dañada a diario. | RN-04, RN-18, RN-19 |
| P-06 | Se dan de baja; reciclar lo posible y desechar según la ley. | Acta de baja aprobada, con destino reciclaje o disposición final. | RN-17, RF-EST-003 |
| P-07 | A conveniencia. | IVA 0/5/19 %, descuento por línea con tope por rol, 4 medios de pago, solo contado. | RN-08, RN-09, RN-10 |
| P-08 | Sí puede ser ambos; consumidor final con identificación de la empresa. | Tercero con doble rol. Consumidor final usa la identificación genérica 222222222222; la factura siempre muestra los datos de la empresa como emisor. | RN-26, RF-TER-001 |
| P-09 | A conveniencia; sí precio manual. | Base = costo de la última compra; precio manual por gerente o administrador. | RN-20, RF-PRE-001 |
| P-10 | Se admiten pedidos previos y recepciones parciales. | Orden de compra con recepciones parciales o compra directa. | RN-21, RF-COM-001, RF-COM-002 |
| P-11 | A conveniencia. | 6 roles con matriz de permisos y separación de funciones. | Sección 2.2, RN-27 |
| P-12 | A conveniencia. | Reabre solo el administrador, solo el último mes y con motivo. | RN-25, RF-CIE-002 |
| P-13 | Asumir sistemas esenciales. | Lector de códigos, impresoras carta y tirilla, correo, exportación Excel/CSV y API REST. | RF-LEC-001, RF-DOC-001, RF-INT-001 |
| P-14 | Lo define el analista con los datos de prueba. | Plantilla de 6 hojas con costos iniciales; filas definidas al generar datos de prueba. | RF-MIG-001, RN-29 |
| P-15 | Sí, verificar lo físico contra el sistema. | Conteo físico total obligatorio antes del cierre. | RF-CON-001, RN-24 |
| P-16 | Movimientos diarios, productos e histórico. | 100.000 presentaciones, 5.000 líneas por día, 5 años en línea. | RNF-REN-001 a 003 |
| P-17 | Máximo 2 años; consulta a conveniencia. | Retención de 24 meses; consultan administrador, gerente y auditor. | RN-28, RF-SEG-002 |
| P-18 | Sí se requieren ajustes. | Ajustes desde conteo o manuales, con aprobación. | RF-AJU-001, RN-24 |
| P-19 | Sí a ambos; costo a conveniencia. | Cliente: reingreso al costo original del lote. Proveedor: salida al costo del lote de origen. | RN-22, RN-23, RF-DEV-001, RF-DEV-002 |

## 8. Validación y verificación

La versión 4 cumple las 9 características de ISO/IEC/IEEE 29148:2018 para el conjunto de requisitos y queda validada por el interesado; es la línea base para el modelo de datos.

### 8.1 Verificación del conjunto de requisitos

| Característica | v3 | v4 | Evidencia |
| --- | --- | --- | --- |
| Necesario | Cumple | Cumple | Cada RF se origina en una decisión D-xx o en una respuesta P-xx (sección 7). |
| Apropiado | Cumple | Cumple | Los RF describen comportamiento; solo RNF-SEG-001, RNF-COM-001 y RNF-SEG-006 fijan tecnología (hash, REST, HTTPS) por exigencia de calidad. |
| Inequívoco | Parcial | Cumple | "Costo base", "volumen alto", unidades y redondeos ahora tienen definición numérica (RN-05 a RN-07, RN-20, sección 5). |
| Completo | Parcial | Cumple | Ningún campo queda Por definir; las 9 preguntas bloqueantes están resueltas. |
| Singular | Cumple | Cumple | Cada ficha expresa una capacidad; baja, ajuste, conteo y reapertura tienen ficha propia. |
| Factible | Cumple | Cumple | Todo el alcance cabe en una aplicación web con base de datos relacional transaccional. |
| Verificable | Parcial | Cumple | Cada RF tiene criterios con valores concretos (ejemplos de PEPS/FEFO, redondeo, precios); cada RNF tiene umbral numérico. |
| Consistente | Cumple | Cumple | Se revisaron cruces: FEFO y PEPS conviven en RN-19; vencidos (RN-18) no contradicen reservas porque RN-14 permite reservada → dañada; anulación (RN-12) y devolución (RN-22) no se solapan. |
| Trazable | Cumple | Cumple | Matriz 8.2: cada RF enlaza reglas, requisitos de calidad y origen. |

### 8.2 Matriz de trazabilidad

| Requisito | Reglas de negocio | Requisitos de calidad | Origen |
| --- | --- | --- | --- |
| RF-PRO-001 | RN-05, RN-08, RN-18, RN-20 | RNF-INT-002 | D-03, D-04 |
| RF-PRO-002 | RN-01, RN-05 | RNF-INT-002 | D-03, P-03 |
| RF-PRO-003 | RN-13, RN-27 | RNF-REN-001, RNF-SEG-002 | D-05 |
| RF-TER-001 | RN-26 | RNF-INT-002 | Contexto, P-08 |
| RF-CFG-001 | RN-08, RN-11, RN-28 | RNF-SEG-002 | P-01, P-07 |
| RF-PRE-001 | RN-06, RN-08, RN-20 | RNF-SEG-003 | D-08, P-09 |
| RF-COM-001 | RN-06, RN-08, RN-11, RN-21 | RNF-SEG-003 | P-10 |
| RF-COM-002 | RN-02, RN-05, RN-06, RN-11, RN-18, RN-20, RN-21, RN-25 | RNF-FIA-001, RNF-SEG-003 | D-06, P-10 |
| RF-VEN-001 | RN-02 a RN-11, RN-15, RN-18, RN-19, RN-25 | RNF-FIA-001, RNF-REN-001, RNF-REN-003, RNF-INT-001 | D-06, D-10, P-02, P-05, P-07 |
| RF-VEN-002 | RN-11, RN-12 | RNF-SEG-003 | Analista (corrección de errores) |
| RF-DEV-001 | RN-02, RN-03, RN-11, RN-22, RN-25 | RNF-FIA-001 | P-19 |
| RF-DEV-002 | RN-02, RN-03, RN-11, RN-23, RN-25 | RNF-FIA-001 | P-19 |
| RF-EST-001 | RN-03, RN-14, RN-15 | RNF-SEG-003 | D-05, P-04 |
| RF-EST-002 | RN-03, RN-14, RN-16, RN-18 | RNF-SEG-003 | D-05, P-05 |
| RF-EST-003 | RN-02, RN-03, RN-11, RN-17, RN-25 | RNF-SEG-003 | P-06 |
| RF-CON-001 | RN-24 | RNF-INT-001 | P-15 |
| RF-AJU-001 | RN-02, RN-03, RN-11, RN-19, RN-24, RN-25 | RNF-FIA-001, RNF-SEG-003 | P-18 |
| RF-CIE-001 | RN-24, RN-25 | RNF-FIA-001 | D-07, P-15 |
| RF-CIE-002 | RN-25, RN-28 | RNF-SEG-003 | P-12 |
| RF-MOV-001 | RN-02, RN-12, RN-27 | RNF-REN-002 | D-07 |
| RF-REP-001 | RN-19, RN-25, RN-27 | RNF-REN-002, RNF-SEG-002 | D-08, D-10 |
| RF-SEG-001 | RN-27, RN-28 | RNF-SEG-001, RNF-SEG-002, RNF-SEG-005, RNF-SEG-006 | Contexto, P-11 |
| RF-SEG-002 | RN-02, RN-28 | RNF-SEG-003, RNF-SEG-004 | D-07, P-17 |
| RF-MIG-001 | RN-01, RN-26, RN-29 | RNF-FIA-001 | Contexto, P-14 |
| RF-LEC-001 | RN-01 | RNF-COM-001, RNF-INT-001 | Contexto, P-13 |
| RF-DOC-001 | RN-11, RN-12 | RNF-COM-001, RNF-INT-003 | P-01, P-13 |
| RF-INT-001 | RN-27, RN-28 | RNF-COM-001, RNF-SEG-001 | Contexto, P-13 |

### 8.3 Validación con el interesado

- [x] Preguntas de la sección 7 respondidas.
- [x] Prioridades de los RF aprobadas.
- [x] Reglas propuestas y decididas por el analista aprobadas.
- [x] Valores numéricos de los RNF aprobados.
- [x] Revisión final de esta versión 4 por el interesado y el profesor.
