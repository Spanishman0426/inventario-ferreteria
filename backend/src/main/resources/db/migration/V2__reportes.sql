-- Nombre: V2__reportes.sql
-- Descripción: Migración Flyway con las funciones del esquema reportes (R-01 a R-13).
--              Copia de database/04_consultas_reportes.sql sin las líneas BEGIN; y COMMIT; (Flyway maneja la transacción).
--              El SQL ejecutable no se modifica.
-- Fecha de creación: 2026-10-20
-- Última actualización: 2026-10-20
-- Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)

-- =====================================================================
-- 04_consultas_reportes.sql — Consultas base para reportes (RF-REP-001)
-- Requiere 01_esquema_inventario_ferreteria.sql
-- Cada reporte es una función del esquema "reportes": el backend la llama
-- con parámetros y las pruebas (03) comparan su resultado con el oráculo.
-- Ejecución: psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f 04_consultas_reportes.sql
-- =====================================================================


CREATE SCHEMA IF NOT EXISTS reportes;

-- Límites de un rango de fechas en hora de Colombia: [desde 00:00, hasta+1 00:00)
CREATE OR REPLACE FUNCTION reportes.ini(p date) RETURNS timestamptz
LANGUAGE sql IMMUTABLE AS $$ SELECT p::timestamp AT TIME ZONE 'America/Bogota' $$;

CREATE OR REPLACE FUNCTION reportes.fin(p date) RETURNS timestamptz
LANGUAGE sql IMMUTABLE AS $$ SELECT (p + 1)::timestamp AT TIME ZONE 'America/Bogota' $$;

CREATE OR REPLACE FUNCTION reportes.hoy() RETURNS date
LANGUAGE sql STABLE AS $$ SELECT (now() AT TIME ZONE 'America/Bogota')::date $$;

-- R-01 Existencias valorizadas a una fecha de corte (reconstruidas desde el kardex)
CREATE OR REPLACE FUNCTION reportes.existencias_valorizadas(p_corte date)
RETURNS TABLE (producto_id bigint, producto text, unidad varchar, disponible numeric, reservada numeric,
               danada numeric, total numeric, valor bigint)
LANGUAGE sql STABLE AS $$
  WITH por_lote AS (
    SELECT m.producto_id, m.lote_id,
           sum(CASE WHEN m.estado_destino = 'DISPONIBLE' THEN m.cantidad ELSE 0 END)
         - sum(CASE WHEN m.estado_origen  = 'DISPONIBLE' THEN m.cantidad ELSE 0 END) AS disp,
           sum(CASE WHEN m.estado_destino = 'RESERVADA'  THEN m.cantidad ELSE 0 END)
         - sum(CASE WHEN m.estado_origen  = 'RESERVADA'  THEN m.cantidad ELSE 0 END) AS res,
           sum(CASE WHEN m.estado_destino = 'DANADA'     THEN m.cantidad ELSE 0 END)
         - sum(CASE WHEN m.estado_origen  = 'DANADA'     THEN m.cantidad ELSE 0 END) AS dan
    FROM movimiento_inventario m
    WHERE m.fecha < reportes.fin(p_corte)
    GROUP BY m.producto_id, m.lote_id
  )
  SELECT p.id, p.nombre, p.unidad_base,
         sum(pl.disp), sum(pl.res), sum(pl.dan), sum(pl.disp + pl.res + pl.dan),
         round(sum((pl.disp + pl.res + pl.dan) * l.costo_unitario_base))::bigint
  FROM por_lote pl
  JOIN lote l     ON l.id = pl.lote_id
  JOIN producto p ON p.id = pl.producto_id
  GROUP BY p.id, p.nombre, p.unidad_base
  ORDER BY p.nombre;
$$;

-- R-02 Kardex de un producto con saldo acumulado (RF-MOV-001)
CREATE OR REPLACE FUNCTION reportes.kardex(p_producto bigint, p_desde date, p_hasta date)
RETURNS TABLE (id bigint, fecha timestamptz, tipo text, documento text, lote_id bigint, estado_origen text,
               estado_destino text, cantidad_neta numeric, saldo numeric, costo_total bigint)
LANGUAGE sql STABLE AS $$
  SELECT k.id, k.fecha, k.tipo, k.documento, k.lote_id, k.estado_origen, k.estado_destino,
         k.cantidad_neta, k.saldo, k.costo_total
  FROM v_kardex k
  WHERE k.producto_id = p_producto
    AND k.fecha >= reportes.ini(p_desde) AND k.fecha < reportes.fin(p_hasta)
  ORDER BY k.fecha, k.id;
$$;

-- R-03 Resumen de ventas: facturas, venta neta sin IVA, costo, utilidad bruta, IVA (netos de notas crédito)
CREATE OR REPLACE FUNCTION reportes.ventas_resumen(p_desde date, p_hasta date)
RETURNS TABLE (facturas bigint, venta_neta bigint, costo bigint, utilidad bigint, iva bigint, total_facturado bigint)
LANGUAGE sql STABLE AS $$
  WITH docs AS (
    SELECT d.* FROM documento d
    WHERE d.tipo IN ('FV','NC') AND d.estado = 'CONFIRMADO'
      AND d.fecha >= reportes.ini(p_desde) AND d.fecha < reportes.fin(p_hasta)
  ), lin AS (
    SELECT CASE d.tipo WHEN 'FV' THEN 1 ELSE -1 END AS s, l.*
    FROM docs d JOIN documento_linea l ON l.documento_id = d.id
  )
  SELECT (SELECT count(*) FROM docs WHERE tipo = 'FV'),
         COALESCE(sum(s * (subtotal - descuento_valor)), 0)::bigint,
         COALESCE(sum(s * costo_total), 0)::bigint,
         COALESCE(sum(s * (subtotal - descuento_valor - costo_total)), 0)::bigint,
         COALESCE(sum(s * iva_valor), 0)::bigint,
         (SELECT COALESCE(sum(total), 0) FROM docs WHERE tipo = 'FV')::bigint
  FROM lin;
$$;

-- R-04 Ventas y utilidad bruta por producto
CREATE OR REPLACE FUNCTION reportes.ventas_por_producto(p_desde date, p_hasta date)
RETURNS TABLE (producto_id bigint, producto text, cantidad_base numeric, venta_neta bigint, costo bigint,
               utilidad bigint, margen_pct numeric)
LANGUAGE sql STABLE AS $$
  WITH lin AS (
    SELECT CASE d.tipo WHEN 'FV' THEN 1 ELSE -1 END AS s, l.*
    FROM documento d JOIN documento_linea l ON l.documento_id = d.id
    WHERE d.tipo IN ('FV','NC') AND d.estado = 'CONFIRMADO'
      AND d.fecha >= reportes.ini(p_desde) AND d.fecha < reportes.fin(p_hasta)
  )
  SELECT p.id, p.nombre,
         sum(s * cantidad_base),
         sum(s * (subtotal - descuento_valor))::bigint,
         sum(s * costo_total)::bigint,
         sum(s * (subtotal - descuento_valor - costo_total))::bigint,
         round(100.0 * sum(s * (subtotal - descuento_valor - costo_total))
                     / NULLIF(sum(s * (subtotal - descuento_valor)), 0), 2)
  FROM lin JOIN producto p ON p.id = lin.producto_id
  GROUP BY p.id, p.nombre
  ORDER BY 6 DESC;
$$;

-- R-05 Ventas por vendedor (creador de la factura)
CREATE OR REPLACE FUNCTION reportes.ventas_por_vendedor(p_desde date, p_hasta date)
RETURNS TABLE (usuario_id bigint, vendedor text, facturas bigint, total_facturado bigint)
LANGUAGE sql STABLE AS $$
  SELECT u.id, u.nombre_completo, count(*), sum(d.total)::bigint
  FROM documento d JOIN usuario u ON u.id = d.creado_por
  WHERE d.tipo = 'FV' AND d.estado = 'CONFIRMADO'
    AND d.fecha >= reportes.ini(p_desde) AND d.fecha < reportes.fin(p_hasta)
  GROUP BY u.id, u.nombre_completo
  ORDER BY 4 DESC;
$$;

-- R-06 Compras por proveedor, netas de devoluciones (RF-COM-002, RF-DEV-002)
CREATE OR REPLACE FUNCTION reportes.compras_por_proveedor(p_desde date, p_hasta date)
RETURNS TABLE (proveedor_id bigint, proveedor text, documentos bigint, subtotal bigint, iva bigint,
               total bigint, devoluciones bigint, neto bigint)
LANGUAGE sql STABLE AS $$
  SELECT t.id, COALESCE(t.razon_social, t.nombre || ' ' || t.apellido),
         count(*) FILTER (WHERE d.tipo = 'DI'),
         COALESCE(sum(d.subtotal) FILTER (WHERE d.tipo = 'DI'), 0)::bigint,
         COALESCE(sum(d.iva)      FILTER (WHERE d.tipo = 'DI'), 0)::bigint,
         COALESCE(sum(d.total)    FILTER (WHERE d.tipo = 'DI'), 0)::bigint,
         COALESCE(sum(d.subtotal) FILTER (WHERE d.tipo = 'DP'), 0)::bigint,
         (COALESCE(sum(d.subtotal) FILTER (WHERE d.tipo = 'DI'), 0)
        - COALESCE(sum(d.subtotal) FILTER (WHERE d.tipo = 'DP'), 0))::bigint
  FROM documento d JOIN tercero t ON t.id = d.tercero_id
  WHERE d.tipo IN ('DI','DP')
    AND d.fecha >= reportes.ini(p_desde) AND d.fecha < reportes.fin(p_hasta)
  GROUP BY t.id, t.razon_social, t.nombre, t.apellido
  ORDER BY 8 DESC;
$$;

-- R-07 Productos próximos a vencer (RF-REP-001, RN-18)
CREATE OR REPLACE FUNCTION reportes.proximos_a_vencer(p_dias integer DEFAULT 30, p_hoy date DEFAULT NULL)
RETURNS TABLE (lote_id bigint, producto text, fecha_vencimiento date, dias_restantes integer,
               cantidad numeric, valor bigint)
LANGUAGE sql STABLE AS $$
  SELECT l.id, p.nombre, l.fecha_vencimiento,
         l.fecha_vencimiento - COALESCE(p_hoy, reportes.hoy()),
         l.saldo_disponible + l.saldo_reservado,
         round((l.saldo_disponible + l.saldo_reservado) * l.costo_unitario_base)::bigint
  FROM lote l JOIN producto p ON p.id = l.producto_id
  WHERE l.fecha_vencimiento IS NOT NULL
    AND l.saldo_disponible + l.saldo_reservado > 0
    AND l.fecha_vencimiento BETWEEN COALESCE(p_hoy, reportes.hoy())
                                AND COALESCE(p_hoy, reportes.hoy()) + p_dias
  ORDER BY l.fecha_vencimiento;
$$;

-- R-08 Daños y bajas del período
CREATE OR REPLACE FUNCTION reportes.danados_y_bajas(p_desde date, p_hasta date)
RETURNS TABLE (fecha timestamptz, evento text, producto text, lote_id bigint, cantidad numeric,
               costo bigint, motivo text)
LANGUAGE sql STABLE AS $$
  SELECT m.fecha,
         CASE WHEN m.tipo = 'BAJA' THEN 'Baja' ELSE 'Paso a dañada' END,
         p.nombre, m.lote_id, m.cantidad, m.costo_total,
         COALESCE(mc.nombre, md.nombre)
  FROM movimiento_inventario m
  JOIN producto p            ON p.id = m.producto_id
  LEFT JOIN cambio_estado c  ON c.id = m.cambio_estado_id
  LEFT JOIN motivo mc        ON mc.id = c.motivo_id
  LEFT JOIN documento_linea l ON l.id = m.documento_linea_id
  LEFT JOIN motivo md        ON md.id = l.motivo_id
  WHERE (m.tipo = 'BAJA' OR (m.tipo = 'CAMBIO_ESTADO' AND m.estado_destino = 'DANADA'))
    AND m.fecha >= reportes.ini(p_desde) AND m.fecha < reportes.fin(p_hasta)
  ORDER BY m.fecha;
$$;

-- R-09 Diferencias de un conteo físico (RF-CON-001)
CREATE OR REPLACE FUNCTION reportes.diferencias_conteo(p_conteo bigint)
RETURNS TABLE (producto text, lote_id bigint, registrado numeric, contado numeric, diferencia numeric, valor bigint)
LANGUAGE sql STABLE AS $$
  SELECT p.nombre, cl.lote_id,
         cl.registrado_buen_estado + cl.registrado_danado,
         cl.contado_buen_estado + cl.contado_danado,
         (cl.contado_buen_estado + cl.contado_danado) - (cl.registrado_buen_estado + cl.registrado_danado),
         round(((cl.contado_buen_estado + cl.contado_danado) - (cl.registrado_buen_estado + cl.registrado_danado))
               * COALESCE(l.costo_unitario_base, p.ultimo_costo_base, 0))::bigint
  FROM conteo_linea cl
  JOIN producto p  ON p.id = cl.producto_id
  LEFT JOIN lote l ON l.id = cl.lote_id
  WHERE cl.conteo_id = p_conteo
    AND (cl.contado_buen_estado + cl.contado_danado) <> (cl.registrado_buen_estado + cl.registrado_danado)
  ORDER BY 6;
$$;

-- R-10 Reservas activas (RF-EST-001)
CREATE OR REPLACE FUNCTION reportes.reservas_activas(p_hoy date DEFAULT NULL)
RETURNS TABLE (reserva_id bigint, cliente text, producto text, cantidad numeric, fecha_limite date, dias_restantes integer)
LANGUAGE sql STABLE AS $$
  SELECT r.id, COALESCE(t.razon_social, t.nombre || ' ' || t.apellido), p.nombre, r.cantidad, r.fecha_limite,
         r.fecha_limite - COALESCE(p_hoy, reportes.hoy())
  FROM reserva r
  JOIN tercero t  ON t.id = r.cliente_id
  JOIN producto p ON p.id = r.producto_id
  WHERE r.estado = 'ACTIVA'
  ORDER BY r.fecha_limite;
$$;

-- R-11 Historial de precios de una presentación (RF-PRE-001)
CREATE OR REPLACE FUNCTION reportes.historial_precios(p_presentacion bigint)
RETURNS TABLE (vigente_desde timestamptz, vigente_hasta timestamptz, precio_base bigint, precio_con_iva bigint,
               origen text, costo_base_unitario numeric, usuario text, motivo text)
LANGUAGE sql STABLE AS $$
  SELECT pp.vigente_desde, pp.vigente_hasta, pp.precio_base,
         round(pp.precio_base * (1 + t.porcentaje / 100))::bigint,
         pp.origen, pp.costo_base_unitario, u.nombre_completo, pp.motivo
  FROM precio_presentacion pp
  JOIN presentacion pr ON pr.id = pp.presentacion_id
  JOIN producto p      ON p.id = pr.producto_id
  JOIN tarifa_iva t    ON t.id = p.tarifa_iva_id
  JOIN usuario u       ON u.id = pp.usuario_id
  WHERE pp.presentacion_id = p_presentacion
  ORDER BY pp.vigente_desde;
$$;

-- R-12 IVA generado por tarifa, neto de notas crédito
CREATE OR REPLACE FUNCTION reportes.iva_por_tarifa(p_desde date, p_hasta date)
RETURNS TABLE (tarifa numeric, base bigint, iva bigint)
LANGUAGE sql STABLE AS $$
  SELECT l.tarifa_iva,
         sum(CASE d.tipo WHEN 'FV' THEN 1 ELSE -1 END * (l.subtotal - l.descuento_valor))::bigint,
         sum(CASE d.tipo WHEN 'FV' THEN 1 ELSE -1 END * l.iva_valor)::bigint
  FROM documento d JOIN documento_linea l ON l.documento_id = d.id
  WHERE d.tipo IN ('FV','NC') AND d.estado = 'CONFIRMADO'
    AND d.fecha >= reportes.ini(p_desde) AND d.fecha < reportes.fin(p_hasta)
  GROUP BY l.tarifa_iva
  ORDER BY l.tarifa_iva;
$$;

-- R-13 Saldos congelados de un cierre mensual (RF-CIE-001)
CREATE OR REPLACE FUNCTION reportes.saldos_cierre(p_periodo date)
RETURNS TABLE (producto_id bigint, producto text, disponible numeric, reservada numeric, danada numeric, valor bigint)
LANGUAGE sql STABLE AS $$
  SELECT p.id, p.nombre, sum(cs.saldo_disponible), sum(cs.saldo_reservado), sum(cs.saldo_danado),
         sum(cs.valor)::bigint
  FROM cierre_saldo cs
  JOIN lote l     ON l.id = cs.lote_id
  JOIN producto p ON p.id = l.producto_id
  WHERE cs.periodo = date_trunc('month', p_periodo)::date
  GROUP BY p.id, p.nombre
  ORDER BY p.nombre;
$$;


-- Ejemplos de uso:
--   SELECT * FROM reportes.existencias_valorizadas('2026-08-31');
--   SELECT * FROM reportes.ventas_resumen('2026-09-01', '2026-09-30');
--   SELECT * FROM reportes.ventas_por_producto('2026-08-01', '2026-10-31');
--   SELECT * FROM reportes.proximos_a_vencer(180);
--   SELECT * FROM reportes.kardex(3, '2026-08-01', '2026-10-31');
