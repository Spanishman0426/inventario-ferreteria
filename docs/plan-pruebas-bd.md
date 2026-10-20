# Plan y casos de prueba — Base de datos del Sistema de Inventario de Ferretería

Oct 20, 2026 · @Someone

Este entregable prueba la base de datos con un escenario de tres meses de operación, define las consultas base para los reportes y verifica los dos documentos anteriores. Cada caso tiene un resultado esperado fijado de antemano, y los valores numéricos los calcula un programa independiente de la base (el oráculo), de modo que un error en SQL no pueda confirmarse a sí mismo.

## 1. Alcance y estrategia

| Nivel | Qué se prueba | Técnica | Archivo |
| --- | --- | --- | --- |
| Integridad | Que la base rechace lo que las reglas prohíben. | Pruebas negativas: se ejecuta la operación prohibida y se espera un código de error concreto (SQLSTATE). | `03_pruebas_base_datos.sql` |
| Consistencia | Que lotes, kardex, documentos y pagos cuadren entre sí. | Invariantes: consultas que deben devolver cero discrepancias. | `03_pruebas_base_datos.sql` |
| Cálculo | Que precios, IVA, redondeos, PEPS/FEFO y costos sean correctos. | Comparación con valores del oráculo. | `03_pruebas_base_datos.sql` |
| Reportes | Que las consultas base devuelvan las cifras correctas. | Comparación con valores del oráculo. | `04_consultas_reportes.sql` + `03` |
| Documentos | Que especificación y modelo sean coherentes entre sí. | Inspección con lista de chequeo y conteos cruzados. | Sección 7 |

Los archivos se ejecutan en orden: `01` (esquema, ya aplicado), `02_datos_prueba.sql`, `04_consultas_reportes.sql` y `03_pruebas_base_datos.sql`. Cada prueba corre dentro de una subtransacción que se revierte, así que la base queda igual después de probar y las pruebas pueden repetirse.

**Criterio de éxito:** el resumen final de `03` informa 0 fallas. Cualquier falla se reporta con su identificador, el resultado obtenido y el esperado.

## 2. Escenario de datos de prueba

`02_datos_prueba.sql` carga tres meses de operación de una ferretería en Neiva: 15 documentos, 27 líneas, 11 lotes, 29 movimientos de kardex, 7 pagos, 4 cambios de estado y 17 precios. Agosto queda cerrado con su conteo físico; septiembre y octubre quedan abiertos. Cada documento está elegido para ejercitar una regla concreta.

### 2.1 Maestros

| Elemento | Datos |
| --- | --- |
| Usuarios | Un usuario por rol: admin, gerente, jefe, comprador, vendedor, auditor (más Sistema). |
| Empresa | Ferretería Surcolombiana S.A.S., NIT 901234567-7. |
| Productos | 9 productos de 8 marcas y 7 categorías, con 13 presentaciones; 4 unidades base distintas (UND, MTR, KG, GAL). |
| Control | 2 con garantía (martillo, bombillo), 3 con vencimiento (silicona, soldadura PVC, pintura). |
| Códigos | 3 presentaciones con código interno EAN-13 prefijo 20 (metro de cable, unidad de tornillo, kilo de cemento). |
| Terceros | 2 proveedores, 2 clientes, 1 cliente-proveedor y Consumidor final. |

### 2.2 Operaciones

| Fecha | Documento | Qué ocurre | Regla que ejercita |
| --- | --- | --- | --- |
| 03-ago | AP-1 | Migración inicial: 6 lotes de apertura con costo. | RN-29 |
| 05-ago | OC-1 | Orden a Pavco: 100 tubos y 24 soldaduras. | RN-21 |
| 08-ago | DI-1 | Recepción parcial: 60 tubos y 24 soldaduras; la OC queda Recibida parcial. | RN-21 |
| 10-ago | DI-2 | Compra directa de pintura, lote A a $45.000 que vence en jun-2027. | RN-18 |
| 12-ago | FV-1 | Consumidor final, efectivo: total $104.670 se cobra $104.650; fracción de 2,5 kg de cemento. | RN-05, RN-07 |
| 20-ago | FV-2 | Constructora Andina, crédito, descuento 5 % en cemento, cuñete de 5 galones. | RN-09, RN-05 |
| 25-ago | Cambio | 1 martillo pasa a dañada por rotura. | RN-14, RN-16 |
| 31-ago | Conteo + AJ-1 | Faltan 5 tornillos; el ajuste lo propone la jefe y lo aprueba el gerente. | RN-24, RN-27 |
| 01-sep | Cierre | Agosto se cierra con saldos congelados. | RN-25 |
| 01-sep | Precio | El gerente fija a mano el bombillo en $7.900. | RN-20 |
| 02-sep | DI-3 | Resto de la OC: 40 tubos a $4.200; el precio sube a $5.670; la OC queda Recibida. | RN-20, RN-21 |
| 03-sep | DI-4 | Pintura lote B a $47.000 que vence en mar-2027, antes que el lote A. | RN-19 |
| 05-sep | FV-3 | 8 galones con pago mixto débito + efectivo; FEFO consume el lote B. | RN-07, RN-19 |
| 10-sep | NC-1 | Devolución de 1 martillo de FV-1 al lote y costo original. | RN-22 |
| 12-sep | Reserva 1 | 10 tubos para Constructora Andina. | RN-15 |
| 15-sep | DP-1 | 2 soldaduras defectuosas vuelven a Pavco desde el lote de DI-1. | RN-23 |
| 16-sep | FV-4 | Andina compra los 10 tubos reservados; la reserva queda consumida. | RN-04, RN-15 |
| 20-sep | AB-1 | Baja del martillo dañado con destino reciclaje, aprobada por el gerente. | RN-17 |
| 01-oct | Cambio | Tarea del Sistema: la silicona vencida el 30-sep pasa a dañada. | RN-18 |
| 05-oct | FV-5 | 3 bombillos al precio manual, garantía hasta oct-2027. | RN-18, RN-20 |
| 15-oct | Reserva 2 | 2 bombillos para Ferreconstrucciones, activa. | RN-15 |

### 2.3 El oráculo

Los datos los genera `oraculo.py`, un programa en Python que simula cada operación aplicando las reglas de negocio (redondeos, IVA, PEPS/FEFO, costos por lote, precios) sin consultar la base, y que verifica sus propias invariantes mientras simula: por ejemplo, que ningún saldo quede negativo o que los pagos sumen el total. Al escribir el escenario, el oráculo detectó un error en los datos de entrada: el efectivo recibido en FV-3 era menor que lo que se debía pagar en efectivo, algo que el CHECK `ck_pago_cambio` habría rechazado. Se corrigió antes de generar el script.

## 3. Casos de prueba de integridad

Los 30 casos PB intentan una operación prohibida y pasan solo si la base la rechaza con el código de error esperado: 23514 (CHECK), 23505 (único), 23503 (clave foránea) o P0001 (trigger de negocio). Un rechazo por una causa distinta cuenta como falla, porque indicaría que la regla probada no es la que actuó.

| ID | Regla | Operación intentada | Esperado |
| --- | --- | --- | --- |
| PB-01 | RN-03 | Restar 1.000 unidades al saldo de un lote con 18 | 23514 |
| PB-02 | RN-01 | Presentación con un código de barras ya usado | 23505 |
| PB-03 | RN-26 | NIT con dígito de verificación incorrecto | 23514 |
| PB-04 | RN-26 | Persona jurídica sin razón social | 23514 |
| PB-05 | RN-26 | Un segundo consumidor final | 23505 |
| PB-06 | RN-07 | Pago en efectivo de $104.670 (no múltiplo de 50) | 23514 |
| PB-07 | RN-14 | Cambio de estado dañada → reservada | 23514 |
| PB-08 | RN-14 | Retorno de dañada a disponible sin autorizador | 23514 |
| PB-09 | RN-27 | Retorno autorizado por el mismo usuario que lo pide | 23514 |
| PB-10 | RN-25 | Movimiento con fecha en agosto, ya cerrado | P0001 |
| PB-11 | RN-25 | Factura con fecha en agosto | P0001 |
| PB-12 | RN-12 | Modificar una factura confirmada | P0001 |
| PB-13 | RN-12 | Borrar una factura confirmada | P0001 |
| PB-14 | RN-12 | Modificar una línea de factura confirmada | P0001 |
| PB-15 | RN-12 | Cambiar el proveedor de una orden ya recibida | P0001 |
| PB-16 | RN-12 | Anular una factura de otro día | P0001 |
| PB-17 | RN-02 | Borrar un movimiento del kardex | P0001 |
| PB-18 | RN-28 | Modificar un registro de auditoría | P0001 |
| PB-19 | RN-02 | Borrar un lote | P0001 |
| PB-20 | RN-02 | Movimiento sin documento ni cambio de estado | 23514 |
| PB-21 | RN-27 | Ajuste aprobado por quien lo propuso | 23514 |
| PB-22 | RN-24 | Cerrar septiembre sin conteo físico | P0001 |
| PB-23 | RN-25 | Cerrar octubre con septiembre abierto | P0001 |
| PB-24 | RN-25 | Reapertura con motivo de menos de 20 caracteres | 23514 |
| PB-25 | RN-15 | Reserva con plazo de 10 días | 23514 |
| PB-26 | RN-20 | Segundo precio vigente para la misma presentación | 23505 |
| PB-27 | RN-09 | Descuento de 60 % | 23514 |
| PB-28 | RN-06 | Factura cuyo total no cuadra con subtotal e IVA | 23514 |
| PB-29 | RN-05 | Línea con cantidad cero | 23514 |
| PB-30 | — | Línea con un producto inexistente | 23503 |

## 4. Casos de prueba de consistencia y cálculo

Los 14 casos PC comprueban que las piezas del modelo cuadren entre sí sobre todos los datos cargados, no sobre un ejemplo; los 18 casos PK comparan cálculos concretos con el oráculo.

### 4.1 Consistencia

| ID | Comprobación | Esperado |
| --- | --- | --- |
| PC-01 | Saldo de cada lote, por estado, igual a la suma de su kardex | 0 discrepancias |
| PC-02 | Existencia de cada producto igual al saldo final de su kardex | 0 discrepancias |
| PC-03 | Subtotal, descuento e IVA del encabezado iguales a la suma de las líneas | 0 discrepancias |
| PC-04 | Pagos de cada factura y nota crédito iguales a su total | 0 discrepancias |
| PC-05 | Costo de cada línea de salida igual al costo de sus movimientos | 0 discrepancias |
| PC-06 | Cantidad base igual a cantidad × factor en todas las líneas | 0 discrepancias |
| PC-07 | Numeración de 1 a N sin saltos por tipo de documento | 0 tipos con saltos |
| PC-08 | Siguiente consecutivo igual al último número + 1 | 0 discrepancias |
| PC-09 | La auditoría registró la creación de FV-1 | sí |
| PC-10 | Ninguna contraseña aparece en la auditoría | 0 registros |
| PC-11 | El rol vendedor no tiene el permiso de ver costos | 0 permisos |
| PC-12 | Una factura del mismo día se anula sin error | sin error |
| PC-13 | Agosto se reabre y se vuelve a cerrar sin error | sin error |
| PC-14 | Un tercero se marca como cliente y proveedor a la vez | sin error |

### 4.2 Cálculo

| ID | Caso | Esperado (oráculo) |
| --- | --- | --- |
| PK-01 | FV-1: subtotal, IVA, redondeo y total | 87.958 · 16.712 · −20 · 104.650 |
| PK-02 | FV-1: cambio de $110.000 | 5.350 |
| PK-03 | FV-2: subtotal, descuento, IVA y total | 517.000 · 7.130 · 96.875 · 606.745 |
| PK-04 | FV-3 con pago mixto: subtotal, IVA, redondeo y total | 526.400 · 100.016 · −16 · 626.400 |
| PK-05 | FV-3 consumió el lote B, que vence antes aunque entró después | lote 11: 8 unidades |
| PK-06 | FV-3: costo de venta al costo del lote B | 376.000 |
| PK-07 | Primer lote vendible de pintura | lote 11 |
| PK-08 | La silicona vencida no aparece como vendible | 0 filas |
| PK-09 | La silicona vencida pasó a dañada | 8 dañadas, 0 disponibles |
| PK-10 | NC-1: subtotal, IVA, redondeo y total | 23.400 · 4.446 · +4 · 27.850 |
| PK-11 | La devolución reingresó al lote original con su costo | lote 1 a $18.000 |
| PK-12 | Precio del tubo tras subir el costo: base y con IVA | 5.670 · 6.747 |
| PK-13 | El precio manual del bombillo no cambió | MANUAL · 7.900 |
| PK-14 | Cuñete de 5 galones: costo × factor × 1,40 | 329.000 |
| PK-15 | Código interno del metro de cable | 2000000000022 |
| PK-16 | Fin de garantía de los bombillos de FV-5 | 2027-10-05 |
| PK-17 | Estado final de la orden de compra | RECIBIDA |
| PK-18 | La reserva 1 quedó consumida por FV-4 | CONSUMIDA · FV id 13 |

## 5. Consultas base para reportes

`04_consultas_reportes.sql` define 13 reportes como funciones del esquema `reportes`, con parámetros de fecha o identificador. Así el backend no reescribe SQL de reportes: llama la función y pagina o exporta el resultado. Todos los rangos de fechas se interpretan en hora de Colombia, del inicio del primer día al final del último.

| ID | Función | Devuelve | Requisito |
| --- | --- | --- | --- |
| R-01 | `existencias_valorizadas(corte)` | Disponible, reservada, dañada, total y valor por producto a una fecha, reconstruidos desde el kardex | RF-REP-001 |
| R-02 | `kardex(producto, desde, hasta)` | Movimientos con documento, lote, cantidad neta y saldo acumulado | RF-MOV-001 |
| R-03 | `ventas_resumen(desde, hasta)` | Facturas, venta neta sin IVA, costo, utilidad bruta, IVA y total facturado, netos de notas crédito | RF-REP-001 |
| R-04 | `ventas_por_producto(desde, hasta)` | Cantidad, venta neta, costo, utilidad y margen por producto | RF-REP-001 |
| R-05 | `ventas_por_vendedor(desde, hasta)` | Facturas y total por vendedor | RF-REP-001 |
| R-06 | `compras_por_proveedor(desde, hasta)` | Ingresos, subtotal, IVA, total, devoluciones y neto por proveedor | RF-REP-001 |
| R-07 | `proximos_a_vencer(dias, hoy)` | Lotes con saldo que vencen en el horizonte, con días restantes y valor | RF-REP-001 |
| R-08 | `danados_y_bajas(desde, hasta)` | Pasos a dañada y bajas con cantidad, costo y motivo | RF-REP-001 |
| R-09 | `diferencias_conteo(conteo)` | Diferencias de un conteo con su valor | RF-CON-001 |
| R-10 | `reservas_activas(hoy)` | Reservas activas con días restantes | RF-EST-001 |
| R-11 | `historial_precios(presentacion)` | Cambios de precio con origen, costo base, usuario y motivo | RF-PRE-001 |
| R-12 | `iva_por_tarifa(desde, hasta)` | Base e IVA por tarifa, netos de notas crédito | RF-REP-001 |
| R-13 | `saldos_cierre(periodo)` | Saldos congelados de un cierre por producto | RF-CIE-001 |

Ejemplo de uso:

```sql
SELECT * FROM reportes.ventas_resumen('2026-09-01', '2026-09-30');
SELECT * FROM reportes.ventas_por_producto('2026-08-01', '2026-10-31');
SELECT * FROM reportes.proximos_a_vencer(180);
```

El reporte de existencias se calcula desde el kardex y no desde los saldos actuales de `lote`, para poder consultarlo a cualquier fecha pasada; la prueba PR-09 confirma que coincide con los saldos congelados del cierre.

## 6. Pruebas de los reportes

Los 17 casos PR ejecutan cada función de reportes sobre el escenario y comparan su resultado con el oráculo. Dos casos cruzan reportes entre sí (PR-04 con PR-01, PR-09 con PR-08), de modo que una agregación mal escrita no pase aunque coincida por casualidad con un valor.

| ID | Reporte | Caso | Esperado |
| --- | --- | --- | --- |
| PR-01 | R-03 | Ventas de agosto: facturas, neta, costo, utilidad, IVA, facturado | 2 · 597.828 · 460.050 · 137.778 · 113.587 · 711.395 |
| PR-02 | R-03 | Ventas de septiembre, netas de NC-1 | 2 · 559.700 · 398.000 · 161.700 · 106.343 · 693.873 |
| PR-03 | R-03 | Ventas de octubre | 1 · 23.700 · 16.500 · 7.200 · 4.503 · 28.203 |
| PR-04 | R-04 | La utilidad por producto suma la de PR-01 | 137.778 |
| PR-05 | R-05 | Ventas por vendedor en septiembre | Valeria Vendedora · 2 |
| PR-06 | R-06 | Compras de agosto: ingresos, subtotal, total, neto | 2 · 1.122.000 · 1.335.180 · 1.122.000 |
| PR-07 | R-06 | Compras de septiembre, netas de DP-1 | 2 · 638.000 · 759.220 · 602.000 |
| PR-08 | R-01 | Inventario valorizado al 31 de agosto | 2.774.650 |
| PR-09 | R-13 | Saldos del cierre de agosto (debe igualar PR-08) | 2.774.650 |
| PR-10 | R-01 | Inventario valorizado al 20 de octubre | 2.944.150 |
| PR-11 | R-02 | Saldo final del kardex del tornillo | 1.895 |
| PR-12 | R-07 | Lotes que vencen en 180 días desde el 20 de octubre | lotes 8 y 11 |
| PR-13 | R-08 | Daños y bajas de agosto a octubre: eventos y costo | 3 · 108.000 |
| PR-14 | R-09 | Diferencias del conteo de agosto: líneas y valor | 1 · −300 |
| PR-15 | R-10 | Reservas activas al 20 de octubre | 1 · Ferreconstrucciones Ltda. |
| PR-16 | R-11 | Historial del galón de pintura: cambios y último precio | 2 · 65.800 |
| PR-17 | R-12 | IVA de agosto por tarifa | 19 % · 113.587 |

Un límite que conviene declarar: el costo de cada línea vendida lo calcula el oráculo al generar los datos, así que estas pruebas verifican que la base guarde, agregue y reporte bien esos costos, no que la aplicación los calcule bien. Esa segunda parte la cubrirán las pruebas del backend con los mismos valores esperados.

## 7. Verificación de los documentos generados

La especificación de requisitos y el modelo de datos se revisaron con 9 comprobaciones; aparecieron 7 defectos, todos corregidos antes de este entregable. Ninguno queda abierto.

| # | Comprobación | Resultado | Hallazgo y acción |
| --- | --- | --- | --- |
| V-01 | Cada RF tiene al menos una tabla que guarda sus datos | 27 de 27 | Sin hallazgos (modelo, sección 7). |
| V-02 | Cada regla de negocio tiene un mecanismo que la hace cumplir | 29 de 29 | 24 en la base y 5 en la aplicación (modelo, sección 4). |
| V-03 | Actores y permisos coinciden entre RF y matriz 2.2 | Defecto corregido | RF-MOV-001 excluía al vendedor y la matriz lo incluía sin costos; se ajustó RF-MOV-001 a la matriz. |
| V-04 | Orden y numeración de la especificación | Defecto corregido | En la versión 3 la mitad de los RF quedó al final del documento; se reescribió como versión 4. |
| V-05 | Cifras declaradas frente a cifras reales | 3 defectos corregidos | Preguntas bloqueantes (7 → 9), reglas garantizadas por la base (20 → 24) y objetos del script (8 → 12 funciones, 30 → 31 triggers, 27 → 32 índices). |
| V-06 | Compatibilidad del script con PostgreSQL 16 | Defecto corregido antes de ejecutar | Una columna IDENTITY en la tabla particionada de auditoría requería PostgreSQL 17; se cambió a `bigserial`. |
| V-07 | Coherencia de triggers con las reglas | Defecto corregido antes de ejecutar | Un trigger impedía aprobar en septiembre un acta propuesta en agosto; se eliminó porque RN-12 ya garantiza su objetivo. |
| V-08 | Algoritmos de dígito de verificación | Correcto | NIT contra 3 NIT públicos y EAN-13 contra 2 códigos reales. |
| V-09 | Ejecución del esquema | Correcto | `01` ejecutado por el interesado: terminó en COMMIT y la verificación 6.3 dio los valores esperados. |

## 8. Cómo ejecutar y registro de resultados

Sobre la base `ferreteria` que ya tiene el esquema `01`, se ejecutan los tres archivos en este orden. Los datos de prueba solo deben cargarse en una base de pruebas, nunca en la de producción.

```bash
psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f 02_datos_prueba.sql
psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f 04_consultas_reportes.sql
psql -U postgres -d ferreteria -f 03_pruebas_base_datos.sql
```

`02` y `04` deben terminar en `COMMIT`. `03` muestra la lista de 79 pruebas y al final una fila con el total, las correctas y las fallas; las pruebas pueden repetirse cuantas veces se quiera porque no cambian los datos. Para volver a cargar todo desde cero: `dropdb ferreteria`, `createdb ferreteria` y ejecutar `01`, `02`, `04`, `03`.

### 8.1 Registro de ejecución

| Fecha | Ejecutó | Archivo | Resultado | Observaciones |
| --- | --- | --- | --- | --- |
| 2026-10-20 | Juan Pablo | 01\_esquema\_inventario\_ferreteria.sql | COMMIT; verificación 6.3 correcta | — |
| Pendiente | Juan Pablo | 02\_datos\_prueba.sql | — | — |
| Pendiente | Juan Pablo | 04\_consultas\_reportes.sql | — | — |
| Pendiente | Juan Pablo | 03\_pruebas\_base\_datos.sql | Esperado: 79 pruebas, 0 fallas | — |

Si alguna prueba falla, el detalle muestra lo obtenido y lo esperado; con esa salida se determina si el error está en el esquema, en los datos o en la prueba, se corrige y se registra aquí.
