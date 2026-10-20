-- =====================================================================
-- 03_pruebas_base_datos.sql — Pruebas automatizadas de la base de datos
-- Requiere 01 (esquema), 02 (datos de prueba) y 04 (reportes) ya aplicados.
-- Cada prueba corre en una subtransacción que se revierte: la base queda igual.
-- Ejecución: psql -U postgres -d ferreteria -f 03_pruebas_base_datos.sql
-- Resultado: tabla de pruebas y un resumen final; el criterio de éxito es 0 fallas.
-- Valores esperados: calculados por oraculo.py, independiente de la base.
-- =====================================================================

SET app.usuario_id = '2';
SET client_min_messages = warning;

DROP SCHEMA IF EXISTS pruebas CASCADE;
CREATE SCHEMA pruebas;

CREATE TABLE pruebas.resultado (
  orden       serial PRIMARY KEY,
  id          text NOT NULL,
  descripcion text NOT NULL,
  resultado   text NOT NULL,
  obtenido    text,
  esperado    text
);

-- La operación debe fallar con un SQLSTATE concreto
CREATE FUNCTION pruebas.debe_fallar(p_id text, p_desc text, p_sql text, p_sqlstate text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE v_estado text; v_msg text;
BEGIN
  BEGIN
    EXECUTE p_sql;
    RAISE EXCEPTION USING ERRCODE = 'PB000', MESSAGE = 'la operación no produjo error';
  EXCEPTION WHEN OTHERS THEN
    v_estado := SQLSTATE; v_msg := SQLERRM;
  END;
  INSERT INTO pruebas.resultado (id, descripcion, resultado, obtenido, esperado)
  VALUES (p_id, p_desc, CASE WHEN v_estado = p_sqlstate THEN 'OK' ELSE 'FALLA' END,
          v_estado || ': ' || left(v_msg, 120), p_sqlstate);
END $$;

-- La operación debe completarse sin error (y luego se revierte)
CREATE FUNCTION pruebas.debe_funcionar(p_id text, p_desc text, p_sql text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE v_estado text; v_msg text;
BEGIN
  BEGIN
    EXECUTE p_sql;
    RAISE EXCEPTION USING ERRCODE = 'PB001', MESSAGE = 'revertir';
  EXCEPTION WHEN OTHERS THEN
    v_estado := SQLSTATE; v_msg := SQLERRM;
  END;
  INSERT INTO pruebas.resultado (id, descripcion, resultado, obtenido, esperado)
  VALUES (p_id, p_desc, CASE WHEN v_estado = 'PB001' THEN 'OK' ELSE 'FALLA' END,
          CASE WHEN v_estado = 'PB001' THEN 'sin error' ELSE v_estado || ': ' || left(v_msg, 120) END, 'sin error');
END $$;

-- La consulta debe devolver exactamente el valor esperado (como texto)
CREATE FUNCTION pruebas.debe_ser(p_id text, p_desc text, p_sql text, p_esperado text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE v text;
BEGIN
  BEGIN
    EXECUTE p_sql INTO v;
  EXCEPTION WHEN OTHERS THEN
    v := 'ERROR ' || SQLSTATE || ': ' || left(SQLERRM, 100);
  END;
  INSERT INTO pruebas.resultado (id, descripcion, resultado, obtenido, esperado)
  VALUES (p_id, p_desc, CASE WHEN v IS NOT DISTINCT FROM p_esperado THEN 'OK' ELSE 'FALLA' END, v, p_esperado);
END $$;

-- =====================================================================
-- A. INTEGRIDAD: la base debe rechazar lo que las reglas prohíben
--    23514 = CHECK, 23505 = UNIQUE, 23503 = FK, P0001 = trigger de negocio
-- =====================================================================
SELECT pruebas.debe_fallar('PB-01', 'RN-03 Un saldo de lote no puede quedar negativo',
  $q$UPDATE lote SET saldo_disponible = saldo_disponible - 1000 WHERE id = 1$q$, '23514');
SELECT pruebas.debe_fallar('PB-02', 'RN-01 Código de barras duplicado',
  $q$INSERT INTO presentacion (producto_id, nombre, codigo_barras, factor_conversion, admite_fraccion)
     VALUES (1, 'Unidad copia', '7702001000012', 1, false)$q$, '23505');
SELECT pruebas.debe_fallar('PB-03', 'RN-26 NIT con dígito de verificación incorrecto',
  $q$INSERT INTO tercero (tipo_persona, tipo_identificacion, numero_identificacion, digito_verificacion,
       razon_social, direccion, telefono, es_proveedor)
     VALUES ('JURIDICA', 'NIT', '900111222', 2, 'Proveedor errado S.A.S.', 'Neiva', '3001112222', true)$q$, '23514');
SELECT pruebas.debe_fallar('PB-04', 'RN-26 Persona jurídica sin razón social',
  $q$INSERT INTO tercero (tipo_persona, tipo_identificacion, numero_identificacion, digito_verificacion,
       direccion, telefono, es_cliente)
     VALUES ('JURIDICA', 'NIT', '900111222', 1, 'Neiva', '3001112222', true)$q$, '23514');
SELECT pruebas.debe_fallar('PB-05', 'RN-26 Un segundo consumidor final',
  $q$INSERT INTO tercero (tipo_persona, tipo_identificacion, numero_identificacion, nombre, apellido,
       direccion, telefono, es_cliente, es_consumidor_final)
     VALUES ('NATURAL', 'CC', '333333333', 'Otro', 'final', 'No aplica', '0000000', true, true)$q$, '23505');
SELECT pruebas.debe_fallar('PB-06', 'RN-07 Pago en efectivo que no es múltiplo de $50',
  $q$INSERT INTO pago (documento_id, medio, valor) VALUES (5, 'EFECTIVO', 104670)$q$, '23514');
SELECT pruebas.debe_fallar('PB-07', 'RN-14 Transición no permitida dañada → reservada',
  $q$INSERT INTO cambio_estado (lote_id, estado_origen, estado_destino, cantidad, reserva_id, usuario_id)
     VALUES (6, 'DANADA', 'RESERVADA', 1, 2, 4)$q$, '23514');
SELECT pruebas.debe_fallar('PB-08', 'RN-14 Retorno de dañado a disponible sin autorización',
  $q$INSERT INTO cambio_estado (lote_id, estado_origen, estado_destino, cantidad, usuario_id)
     VALUES (6, 'DANADA', 'DISPONIBLE', 1, 4)$q$, '23514');
SELECT pruebas.debe_fallar('PB-09', 'RN-27 Retorno autorizado por el mismo usuario',
  $q$INSERT INTO cambio_estado (lote_id, estado_origen, estado_destino, cantidad, usuario_id, autorizado_por)
     VALUES (6, 'DANADA', 'DISPONIBLE', 1, 3, 3)$q$, '23514');
SELECT pruebas.debe_fallar('PB-10', 'RN-25 Movimiento con fecha en agosto (cerrado)',
  $q$INSERT INTO movimiento_inventario (fecha, tipo, producto_id, lote_id, documento_id, documento_linea_id,
       estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id)
     VALUES ('2026-08-15 10:00-05', 'ENTRADA_COMPRA', 7, 7, 3, 9, 'DISPONIBLE', 1, 4000, 4000, 5)$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-11', 'RN-25 Factura con fecha en agosto (cerrado)',
  $q$INSERT INTO documento (tipo, numero, fecha, estado, tercero_id, creado_por)
     VALUES ('FV', 900, '2026-08-20 10:00-05', 'CONFIRMADO', 1, 6)$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-12', 'RN-12 Modificar una factura confirmada',
  $q$UPDATE documento SET observacion = 'cambio indebido' WHERE id = 6$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-13', 'RN-12 Borrar una factura confirmada',
  $q$DELETE FROM documento WHERE id = 6$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-14', 'RN-12 Modificar una línea de factura confirmada',
  $q$UPDATE documento_linea SET precio_unitario = 1 WHERE id = 12$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-15', 'RN-12 Cambiar el proveedor de una orden ya recibida',
  $q$UPDATE documento SET tercero_id = 3 WHERE id = 2$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-16', 'RN-12 Anular una factura de otro día',
  $q$UPDATE documento SET estado = 'ANULADO', anulado_por = 3, anulado_en = now(),
       motivo_anulacion = 'Prueba' WHERE id = 15$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-17', 'RN-02 Borrar un movimiento del kardex',
  $q$DELETE FROM movimiento_inventario WHERE id = 1$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-18', 'RN-28 Modificar un registro de auditoría',
  $q$UPDATE auditoria SET accion = 'CREAR' WHERE id = (SELECT min(id) FROM auditoria)$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-19', 'RN-02 Borrar un lote',
  $q$DELETE FROM lote WHERE id = 1$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-20', 'RN-02 Movimiento sin documento ni cambio de estado',
  $q$INSERT INTO movimiento_inventario (tipo, producto_id, lote_id, estado_destino, cantidad,
       costo_unitario_base, costo_total, usuario_id)
     VALUES ('ENTRADA_COMPRA', 7, 7, 'DISPONIBLE', 1, 4000, 4000, 5)$q$, '23514');
SELECT pruebas.debe_fallar('PB-21', 'RN-27 Ajuste aprobado por quien lo propuso',
  $q$INSERT INTO documento (tipo, numero, estado, creado_por, aprobado_por, aprobado_en)
     VALUES ('AJ', 900, 'APROBADO', 4, 4, now())$q$, '23514');
SELECT pruebas.debe_fallar('PB-22', 'RN-24 Cerrar septiembre sin conteo físico',
  $q$UPDATE periodo SET estado = 'CERRADO', cerrado_por = 3, cerrado_en = now() WHERE periodo = '2026-09-01'$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-23', 'RN-25 Cerrar octubre con septiembre abierto',
  $q$UPDATE periodo SET estado = 'CERRADO', cerrado_por = 3, cerrado_en = now() WHERE periodo = '2026-10-01'$q$, 'P0001');
SELECT pruebas.debe_fallar('PB-24', 'RN-25 Reapertura con motivo menor a 20 caracteres',
  $q$INSERT INTO periodo_evento (periodo, accion, usuario_id, motivo)
     VALUES ('2026-08-01', 'REAPERTURA', 2, 'corto')$q$, '23514');
SELECT pruebas.debe_fallar('PB-25', 'RN-15 Reserva con plazo mayor a 7 días',
  $q$INSERT INTO reserva (cliente_id, presentacion_id, producto_id, cantidad, cantidad_base, fecha_limite, creado_por)
     VALUES (4, 10, 7, 1, 1, (now() AT TIME ZONE 'America/Bogota')::date + 10, 6)$q$, '23514');
SELECT pruebas.debe_fallar('PB-26', 'RN-20 Dos precios vigentes para una presentación',
  $q$INSERT INTO precio_presentacion (presentacion_id, precio_base, origen, usuario_id)
     VALUES (10, 6000, 'CALCULADO', 2)$q$, '23505');
SELECT pruebas.debe_fallar('PB-27', 'RN-09 Descuento mayor a 50 %',
  $q$INSERT INTO documento_linea (documento_id, numero_linea, producto_id, cantidad, cantidad_base, descuento_pct)
     VALUES (6, 99, 1, 1, 1, 60)$q$, '23514');
SELECT pruebas.debe_fallar('PB-28', 'RN-06 Total de documento que no cuadra',
  $q$INSERT INTO documento (tipo, numero, estado, tercero_id, subtotal, iva, total, creado_por)
     VALUES ('FV', 900, 'CONFIRMADO', 1, 1000, 190, 1000, 6)$q$, '23514');
SELECT pruebas.debe_fallar('PB-29', 'RN-05 Línea con cantidad cero',
  $q$INSERT INTO documento_linea (documento_id, numero_linea, producto_id, cantidad, cantidad_base)
     VALUES (6, 99, 1, 0, 0)$q$, '23514');
SELECT pruebas.debe_fallar('PB-30', 'Integridad referencial: línea con producto inexistente',
  $q$INSERT INTO documento_linea (documento_id, numero_linea, producto_id, cantidad, cantidad_base)
     VALUES (6, 99, 99999, 1, 1)$q$, '23503');

-- =====================================================================
-- B. CONSISTENCIA: lotes, kardex, documentos y pagos deben cuadrar
-- =====================================================================
SELECT pruebas.debe_ser('PC-01', 'Saldo de cada lote por estado = suma de su kardex',
  $q$SELECT count(*)::text FROM lote l
     LEFT JOIN (SELECT lote_id,
                  sum(CASE WHEN estado_destino = 'DISPONIBLE' THEN cantidad ELSE 0 END)
                - sum(CASE WHEN estado_origen  = 'DISPONIBLE' THEN cantidad ELSE 0 END) AS d,
                  sum(CASE WHEN estado_destino = 'RESERVADA'  THEN cantidad ELSE 0 END)
                - sum(CASE WHEN estado_origen  = 'RESERVADA'  THEN cantidad ELSE 0 END) AS r,
                  sum(CASE WHEN estado_destino = 'DANADA'     THEN cantidad ELSE 0 END)
                - sum(CASE WHEN estado_origen  = 'DANADA'     THEN cantidad ELSE 0 END) AS x
                FROM movimiento_inventario GROUP BY lote_id) m ON m.lote_id = l.id
     WHERE l.saldo_disponible <> COALESCE(m.d, 0) OR l.saldo_reservado <> COALESCE(m.r, 0)
        OR l.saldo_danado <> COALESCE(m.x, 0)$q$, '0');
SELECT pruebas.debe_ser('PC-02', 'Existencia por producto = saldo final del kardex',
  $q$SELECT count(*)::text FROM v_existencia_producto e
     LEFT JOIN (SELECT DISTINCT ON (producto_id) producto_id, saldo FROM v_kardex
                ORDER BY producto_id, fecha DESC, id DESC) k ON k.producto_id = e.producto_id
     WHERE e.total <> COALESCE(k.saldo, 0)$q$, '0');
SELECT pruebas.debe_ser('PC-03', 'Encabezado de cada documento = suma de sus líneas',
  $q$SELECT count(*)::text FROM documento d
     LEFT JOIN (SELECT documento_id, sum(subtotal) s, sum(descuento_valor) dv, sum(iva_valor) i
                FROM documento_linea GROUP BY documento_id) l ON l.documento_id = d.id
     WHERE d.subtotal <> COALESCE(l.s, 0) OR d.descuento <> COALESCE(l.dv, 0) OR d.iva <> COALESCE(l.i, 0)$q$, '0');
SELECT pruebas.debe_ser('PC-04', 'RN-10 Pagos de cada factura y nota crédito = su total',
  $q$SELECT count(*)::text FROM documento d
     LEFT JOIN (SELECT documento_id, sum(valor) v FROM pago GROUP BY documento_id) p ON p.documento_id = d.id
     WHERE d.tipo IN ('FV','NC') AND d.estado = 'CONFIRMADO' AND d.total <> COALESCE(p.v, 0)$q$, '0');
SELECT pruebas.debe_ser('PC-05', 'RN-19 Costo de cada línea de salida = costo de sus movimientos',
  $q$SELECT count(*)::text FROM documento_linea l
     JOIN documento d ON d.id = l.documento_id AND d.tipo IN ('FV','NC','DP','AB','AJ')
     LEFT JOIN (SELECT documento_linea_id, sum(costo_total) c FROM movimiento_inventario GROUP BY 1) m
            ON m.documento_linea_id = l.id
     WHERE l.costo_total IS DISTINCT FROM m.c$q$, '0');
SELECT pruebas.debe_ser('PC-06', 'RN-05 cantidad_base = cantidad × factor en todas las líneas',
  $q$SELECT count(*)::text FROM documento_linea WHERE cantidad_base <> cantidad * factor_conversion$q$, '0');
SELECT pruebas.debe_ser('PC-07', 'RN-11 Numeración consecutiva sin saltos por tipo',
  $q$SELECT count(*)::text FROM (SELECT tipo FROM documento WHERE numero IS NOT NULL
     GROUP BY tipo HAVING max(numero) <> count(*) OR min(numero) <> 1) x$q$, '0');
SELECT pruebas.debe_ser('PC-08', 'RN-11 Siguiente consecutivo = último número + 1',
  $q$SELECT count(*)::text FROM consecutivo c
     LEFT JOIN (SELECT tipo, max(numero) m FROM documento GROUP BY tipo) d ON d.tipo = c.tipo_documento
     WHERE c.siguiente_numero <> COALESCE(d.m, 0) + 1$q$, '0');
SELECT pruebas.debe_ser('PC-09', 'RN-28 La auditoría registró la creación de FV-1',
  $q$SELECT (count(*) > 0)::text FROM auditoria
     WHERE entidad = 'documento' AND entidad_id = 5 AND accion = 'CREAR'$q$, 'true');
SELECT pruebas.debe_ser('PC-10', 'RNF-SEG-001 Ninguna contraseña llega a la auditoría',
  $q$SELECT count(*)::text FROM auditoria
     WHERE entidad = 'usuario' AND (valores_despues ? 'password_hash' OR valores_antes ? 'password_hash')$q$, '0');
SELECT pruebas.debe_ser('PC-11', 'Matriz 2.2: el vendedor no tiene permiso de ver costos',
  $q$SELECT count(*)::text FROM rol r JOIN rol_permiso rp ON rp.rol_id = r.id
     JOIN permiso p ON p.id = rp.permiso_id WHERE r.codigo = 'VENDEDOR' AND p.codigo = 'COSTOS_VER'$q$, '0');
SELECT pruebas.debe_funcionar('PC-12', 'RN-12 Se anula una factura del mismo día sin devoluciones',
  $q$DO $d$ DECLARE v bigint; BEGIN
       INSERT INTO documento (tipo, numero, estado, tercero_id, creado_por)
       VALUES ('FV', 900, 'CONFIRMADO', 1, 6) RETURNING id INTO v;
       UPDATE documento SET estado = 'ANULADO', anulado_por = 3, anulado_en = now(),
              motivo_anulacion = 'Error de digitación' WHERE id = v;
     END $d$$q$);
SELECT pruebas.debe_funcionar('PC-13', 'RN-25 Se reabre el último mes cerrado y se vuelve a cerrar',
  $q$DO $d$ BEGIN
       UPDATE periodo SET estado = 'REABIERTO' WHERE periodo = '2026-08-01';
       UPDATE periodo SET estado = 'CERRADO'   WHERE periodo = '2026-08-01';
     END $d$$q$);
SELECT pruebas.debe_funcionar('PC-14', 'RN-26 Un tercero puede ser cliente y proveedor a la vez',
  $q$UPDATE tercero SET es_cliente = true, es_proveedor = true WHERE id = 4$q$);

-- =====================================================================
-- C. CÁLCULO: valores del oráculo
-- =====================================================================
SELECT pruebas.debe_ser('PK-01', 'RN-06/07 FV-1: subtotal | IVA | redondeo | total',
  $q$SELECT concat_ws('|', subtotal, iva, ajuste_redondeo, total) FROM documento WHERE id = 5$q$,
  '87958|16712|-20|104650');
SELECT pruebas.debe_ser('PK-02', 'RN-07 FV-1: cambio entregado de $110.000',
  $q$SELECT cambio::text FROM pago WHERE documento_id = 5$q$, '5350');
SELECT pruebas.debe_ser('PK-03', 'RN-09 FV-2: subtotal | descuento | IVA | total',
  $q$SELECT concat_ws('|', subtotal, descuento, iva, total) FROM documento WHERE id = 6$q$,
  '517000|7130|96875|606745');
SELECT pruebas.debe_ser('PK-04', 'RN-07 FV-3 pago mixto: subtotal | IVA | redondeo | total',
  $q$SELECT concat_ws('|', subtotal, iva, ajuste_redondeo, total) FROM documento WHERE id = 10$q$,
  '526400|100016|-16|626400');
SELECT pruebas.debe_ser('PK-05', 'RN-19 FEFO: FV-3 consumió el lote B (vence antes aunque entró después)',
  $q$SELECT string_agg(lote_id || ':' || cantidad::numeric(14,0), ',') FROM movimiento_inventario
     WHERE documento_id = 10$q$, '11:8');
SELECT pruebas.debe_ser('PK-06', 'RN-19 FV-3: costo de venta al costo del lote B',
  $q$SELECT costo_total::text FROM documento_linea WHERE documento_id = 10$q$, '376000');
SELECT pruebas.debe_ser('PK-07', 'RN-19 El primer lote vendible de pintura es el B',
  $q$SELECT id::text FROM v_lote_vendible WHERE producto_id = 9 AND orden_consumo = 1$q$, '11');
SELECT pruebas.debe_ser('PK-08', 'RN-04 La silicona vencida no es vendible',
  $q$SELECT count(*)::text FROM v_lote_vendible WHERE id = 6$q$, '0');
SELECT pruebas.debe_ser('PK-09', 'RN-18 La silicona vencida pasó a dañada (8 unidades)',
  $q$SELECT (saldo_danado = 8 AND saldo_disponible = 0)::text FROM lote WHERE id = 6$q$, 'true');
SELECT pruebas.debe_ser('PK-10', 'RN-22 NC-1: subtotal | IVA | redondeo | total',
  $q$SELECT concat_ws('|', subtotal, iva, ajuste_redondeo, total) FROM documento WHERE id = 11$q$,
  '23400|4446|4|27850');
SELECT pruebas.debe_ser('PK-11', 'RN-22 La devolución reingresó al lote original con su costo',
  $q$SELECT concat_ws('|', lote_id, costo_unitario_base::numeric(18,0)) FROM movimiento_inventario
     WHERE documento_id = 11$q$, '1|18000');
SELECT pruebas.debe_ser('PK-12', 'RN-20 Precio del tubo tras subir el costo: base | con IVA',
  $q$SELECT concat_ws('|', precio_base, precio_con_iva) FROM v_precio_vigente WHERE presentacion_id = 10$q$,
  '5670|6747');
SELECT pruebas.debe_ser('PK-13', 'RN-20 El precio manual del bombillo se conserva',
  $q$SELECT concat_ws('|', origen, precio_base) FROM v_precio_vigente WHERE presentacion_id = 8$q$,
  'MANUAL|7900');
SELECT pruebas.debe_ser('PK-14', 'RN-20 Cuñete de 5 gal: precio = costo × factor × (1 + 40 %)',
  $q$SELECT precio_base::text FROM v_precio_vigente WHERE presentacion_id = 13$q$, '329000');
SELECT pruebas.debe_ser('PK-15', 'RN-01 Código interno EAN-13 del metro de cable',
  $q$SELECT (codigo_barras = fn_codigo_interno(2))::text || '|' || codigo_barras
     FROM presentacion WHERE id = 2$q$, 'true|2000000000022');
SELECT pruebas.debe_ser('PK-16', 'RN-18 Fecha fin de garantía de los bombillos de FV-5',
  $q$SELECT fecha_fin_garantia::text FROM documento_linea WHERE documento_id = 15$q$, '2027-10-05');
SELECT pruebas.debe_ser('PK-17', 'RN-21 La orden de compra quedó recibida',
  $q$SELECT estado FROM documento WHERE id = 2$q$, 'RECIBIDA');
SELECT pruebas.debe_ser('PK-18', 'RN-15 La reserva 1 quedó consumida por FV-4',
  $q$SELECT concat_ws('|', estado, factura_id) FROM reserva WHERE id = 1$q$, 'CONSUMIDA|13');

-- =====================================================================
-- D. REPORTES: funciones de 04 contra el oráculo
-- =====================================================================
SELECT pruebas.debe_ser('PR-01', 'R-03 Ventas de agosto: facturas | neta | costo | utilidad | IVA | facturado',
  $q$SELECT concat_ws('|', facturas, venta_neta, costo, utilidad, iva, total_facturado)
     FROM reportes.ventas_resumen('2026-08-01', '2026-08-31')$q$, '2|597828|460050|137778|113587|711395');
SELECT pruebas.debe_ser('PR-02', 'R-03 Ventas de septiembre, netas de la nota crédito',
  $q$SELECT concat_ws('|', facturas, venta_neta, costo, utilidad, iva, total_facturado)
     FROM reportes.ventas_resumen('2026-09-01', '2026-09-30')$q$, '2|559700|398000|161700|106343|693873');
SELECT pruebas.debe_ser('PR-03', 'R-03 Ventas de octubre',
  $q$SELECT concat_ws('|', facturas, venta_neta, costo, utilidad, iva, total_facturado)
     FROM reportes.ventas_resumen('2026-10-01', '2026-10-31')$q$, '1|23700|16500|7200|4503|28203');
SELECT pruebas.debe_ser('PR-04', 'R-04 La utilidad por producto suma la utilidad de agosto',
  $q$SELECT sum(utilidad)::text FROM reportes.ventas_por_producto('2026-08-01', '2026-08-31')$q$, '137778');
SELECT pruebas.debe_ser('PR-05', 'R-05 Ventas por vendedor en septiembre',
  $q$SELECT concat_ws('|', vendedor, facturas) FROM reportes.ventas_por_vendedor('2026-09-01', '2026-09-30')$q$,
  'Valeria Vendedora|2');
SELECT pruebas.debe_ser('PR-06', 'R-06 Compras de agosto: documentos | subtotal | total | neto',
  $q$SELECT concat_ws('|', sum(documentos), sum(subtotal), sum(total), sum(neto))
     FROM reportes.compras_por_proveedor('2026-08-01', '2026-08-31')$q$, '2|1122000|1335180|1122000');
SELECT pruebas.debe_ser('PR-07', 'R-06 Compras de septiembre, netas de la devolución a proveedor',
  $q$SELECT concat_ws('|', sum(documentos), sum(subtotal), sum(total), sum(neto))
     FROM reportes.compras_por_proveedor('2026-09-01', '2026-09-30')$q$, '2|638000|759220|602000');
SELECT pruebas.debe_ser('PR-08', 'R-01 Inventario valorizado al 31 de agosto',
  $q$SELECT sum(valor)::text FROM reportes.existencias_valorizadas('2026-08-31')$q$, '2774650');
SELECT pruebas.debe_ser('PR-09', 'R-13 Los saldos del cierre de agosto suman lo mismo que R-01',
  $q$SELECT sum(valor)::text FROM reportes.saldos_cierre('2026-08-01')$q$, '2774650');
SELECT pruebas.debe_ser('PR-10', 'R-01 Inventario valorizado al 20 de octubre',
  $q$SELECT sum(valor)::text FROM reportes.existencias_valorizadas('2026-10-20')$q$, '2944150');
SELECT pruebas.debe_ser('PR-11', 'R-02 El kardex del tornillo termina en 1.895 unidades',
  $q$SELECT (saldo = 1895)::text FROM reportes.kardex(3, '2026-08-01', '2026-10-31')
     ORDER BY fecha DESC, id DESC LIMIT 1$q$, 'true');
SELECT pruebas.debe_ser('PR-12', 'R-07 Lotes que vencen en 180 días desde el 20 de octubre',
  $q$SELECT string_agg(lote_id::text, ',' ORDER BY lote_id) FROM reportes.proximos_a_vencer(180, '2026-10-20')$q$,
  '8,11');
SELECT pruebas.debe_ser('PR-13', 'R-08 Daños y bajas ago–oct: eventos | costo',
  $q$SELECT concat_ws('|', count(*), sum(costo)) FROM reportes.danados_y_bajas('2026-08-01', '2026-10-31')$q$,
  '3|108000');
SELECT pruebas.debe_ser('PR-14', 'R-09 Diferencias del conteo de agosto: líneas | valor',
  $q$SELECT concat_ws('|', count(*), sum(valor)) FROM reportes.diferencias_conteo(1)$q$, '1|-300');
SELECT pruebas.debe_ser('PR-15', 'R-10 Reservas activas al 20 de octubre',
  $q$SELECT concat_ws('|', count(*), max(cliente)) FROM reportes.reservas_activas('2026-10-20')$q$,
  '1|Ferreconstrucciones Ltda.');
SELECT pruebas.debe_ser('PR-16', 'R-11 Historial del galón de pintura: cambios | último precio',
  $q$SELECT concat_ws('|', count(*), (array_agg(precio_base ORDER BY vigente_desde DESC))[1])
     FROM reportes.historial_precios(12)$q$, '2|65800');
SELECT pruebas.debe_ser('PR-17', 'R-12 IVA de agosto por tarifa: tarifa | IVA',
  $q$SELECT string_agg(tarifa::numeric(5,0) || '|' || iva, ',') FROM reportes.iva_por_tarifa('2026-08-01', '2026-08-31')$q$,
  '19|113587');

-- =====================================================================
-- RESULTADOS
-- =====================================================================
\echo ''
\echo '================= RESULTADO DE LAS PRUEBAS ================='
SELECT id, resultado, descripcion,
       CASE WHEN resultado = 'FALLA' THEN 'obtenido: ' || coalesce(obtenido, 'NULL') || '  esperado: ' || esperado END AS detalle
FROM pruebas.resultado ORDER BY orden;

SELECT count(*) AS total,
       count(*) FILTER (WHERE resultado = 'OK')    AS ok,
       count(*) FILTER (WHERE resultado = 'FALLA') AS fallas
FROM pruebas.resultado;
