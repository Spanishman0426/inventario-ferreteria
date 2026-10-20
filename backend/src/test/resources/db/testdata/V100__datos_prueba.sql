-- Nombre: V100__datos_prueba.sql
-- Descripción: Datos de prueba del escenario de tres meses; solo los carga el perfil de pruebas.
--              Copia de database/02_datos_prueba.sql sin las líneas BEGIN; y COMMIT; (Flyway maneja la transacción).
--              El SQL ejecutable no se modifica.
-- Fecha de creación: 2026-10-20
-- Última actualización: 2026-10-20
-- Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)

-- =====================================================================
-- 02_datos_prueba.sql — Escenario de prueba agosto–octubre 2026
-- Generado por el oráculo (oraculo.py). Requiere 01_esquema_inventario_ferreteria.sql
-- Ejecución: psql -U postgres -d ferreteria -v ON_ERROR_STOP=1 -f 02_datos_prueba.sql
-- =====================================================================
SET LOCAL app.usuario_id = '2';
SET LOCAL app.ip = '127.0.0.1';

INSERT INTO usuario (id, usuario, nombre_completo, correo, password_hash) OVERRIDING SYSTEM VALUE VALUES
  (2, 'admin', 'Ana Administradora', 'admin@ferreteria.test', '$2b$12$hash.de.prueba.no.valido.para.login'),
  (3, 'gerente', 'Gabriel Gerente', 'gerente@ferreteria.test', '$2b$12$hash.de.prueba.no.valido.para.login'),
  (4, 'jefe', 'Julia Jefe Inventario', 'jefe@ferreteria.test', '$2b$12$hash.de.prueba.no.valido.para.login'),
  (5, 'comprador', 'Carlos Comprador', 'compras@ferreteria.test', '$2b$12$hash.de.prueba.no.valido.para.login'),
  (6, 'vendedor', 'Valeria Vendedora', 'ventas@ferreteria.test', '$2b$12$hash.de.prueba.no.valido.para.login'),
  (7, 'auditor', 'Andrés Auditor', 'auditoria@ferreteria.test', '$2b$12$hash.de.prueba.no.valido.para.login');
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT v.u, r.id FROM (VALUES (2, 'ADMIN'), (3, 'GERENTE'), (4, 'JEFE_INVENTARIO'), (5, 'COMPRADOR'), (6, 'VENDEDOR'), (7, 'AUDITOR')) AS v(u, c) JOIN rol r ON r.codigo = v.c;
INSERT INTO empresa (id, nit, digito_verificacion, razon_social, direccion, telefono, correo) VALUES
  (1, '901234567', 7, 'Ferretería Surcolombiana S.A.S.', 'Calle 8 # 5-20, Neiva', '6088712345', 'facturacion@ferreteria.test');
INSERT INTO marca (id, nombre) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Stanley'),
  (2, 'Centelsa'),
  (3, 'Pavco'),
  (4, 'Pintuco'),
  (5, 'Truper'),
  (6, 'Argos'),
  (7, 'Philips'),
  (8, 'Sika');
INSERT INTO categoria (id, nombre) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Herramientas'),
  (2, 'Eléctricos'),
  (3, 'Plomería'),
  (4, 'Pinturas'),
  (5, 'Tornillería'),
  (6, 'Construcción'),
  (7, 'Adhesivos y selladores');
INSERT INTO producto (id, nombre, marca_id, categoria_id, unidad_base, tarifa_iva_id, porcentaje_incremento, control, plazo_garantia_meses) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Martillo de uña 16 oz', 1, 1, 'UND', 3, 30, 'GARANTIA', 6),
  (2, 'Cable THHN 12 AWG', 2, 2, 'MTR', 3, 25, 'NINGUNO', NULL),
  (3, 'Tornillo drywall 6 x 1"', 5, 5, 'UND', 3, 50, 'NINGUNO', NULL),
  (4, 'Cemento gris uso general', 6, 6, 'KG', 3, 15, 'NINGUNO', NULL),
  (5, 'Bombillo LED 9 W', 7, 2, 'UND', 3, 30, 'GARANTIA', 12),
  (6, 'Silicona acética transparente 280 ml', 8, 7, 'UND', 3, 40, 'VENCIMIENTO', NULL),
  (7, 'Tubo PVC presión 1/2" x 6 m', 3, 3, 'UND', 3, 35, 'NINGUNO', NULL),
  (8, 'Soldadura líquida PVC 1/4 gal', 3, 3, 'UND', 3, 35, 'VENCIMIENTO', NULL),
  (9, 'Pintura vinilo tipo 1 blanco', 4, 4, 'GAL', 3, 40, 'VENCIMIENTO', NULL);
INSERT INTO presentacion (id, producto_id, nombre, codigo_barras, codigo_interno, factor_conversion, admite_fraccion) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 'Unidad', '7702001000012', false, 1, false),
  (2, 2, 'Metro', '2000000000022', true, 1, true),
  (3, 2, 'Rollo 100 m', '7702001000036', false, 100, false),
  (4, 3, 'Unidad', '2000000000046', true, 1, false),
  (5, 3, 'Caja x100', '7702001000050', false, 100, false),
  (6, 4, 'Bulto 50 kg', '7702001000067', false, 50, false),
  (7, 4, 'Kilo', '2000000000077', true, 1, true),
  (8, 5, 'Unidad', '7702001000081', false, 1, false),
  (9, 6, 'Unidad', '7702001000098', false, 1, false),
  (10, 7, 'Unidad', '7702001000104', false, 1, false),
  (11, 8, 'Unidad', '7702001000111', false, 1, false),
  (12, 9, 'Galón', '7702001000128', false, 1, false),
  (13, 9, 'Cuñete 5 gal', '7702001000135', false, 5, false);
INSERT INTO tercero (id, tipo_persona, tipo_identificacion, numero_identificacion, digito_verificacion, nombre, apellido, razon_social, direccion, telefono, correo, es_cliente, es_proveedor) OVERRIDING SYSTEM VALUE VALUES
  (2, 'JURIDICA', 'NIT', '900123456', 8, NULL, NULL, 'Distribuidora Pavco Neiva S.A.S.', 'Neiva', '3001234567', 'ventas@pavconeiva.test', false, true),
  (3, 'JURIDICA', 'NIT', '830045678', 8, NULL, NULL, 'Pinturas del Huila S.A.S.', 'Neiva', '3001234567', NULL, false, true),
  (4, 'JURIDICA', 'NIT', '900987654', 7, NULL, NULL, 'Constructora Andina S.A.S.', 'Carrera 5 # 10-30, Neiva', '3001234567', 'compras@andina.test', true, false),
  (5, 'NATURAL', 'CC', '1075234567', NULL, 'Juan', 'Pérez Gómez', NULL, 'Neiva', '3157654321', NULL, true, false),
  (6, 'JURIDICA', 'NIT', '800555123', 1, NULL, NULL, 'Ferreconstrucciones Ltda.', 'Neiva', '3001234567', NULL, true, true);

-- ---------------- AGOSTO 2026 ----------------
INSERT INTO precio_presentacion (id, presentacion_id, precio_base, origen, costo_base_unitario, porcentaje_incremento, motivo, vigente_desde, vigente_hasta, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 23400, 'CALCULADO', 18000.0000, 30, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (2, 2, 2625, 'CALCULADO', 2100.0000, 25, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (3, 3, 262500, 'CALCULADO', 2100.0000, 25, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (4, 4, 90, 'CALCULADO', 60.0000, 50, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (5, 5, 9000, 'CALCULADO', 60.0000, 50, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (6, 6, 35650, 'CALCULADO', 620.0000, 15, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (7, 7, 713, 'CALCULADO', 620.0000, 15, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (8, 8, 7150, 'CALCULADO', 5500.0000, 30, NULL, '2026-08-03 07:00:00-05', '2026-09-01 08:00:00-05', 2),
  (9, 9, 12600, 'CALCULADO', 9000.0000, 40, NULL, '2026-08-03 07:00:00-05', NULL, 2),
  (10, 10, 5400, 'CALCULADO', 4000.0000, 35, NULL, '2026-08-08 10:00:00-05', '2026-09-02 10:00:00-05', 5),
  (11, 11, 24300, 'CALCULADO', 18000.0000, 35, NULL, '2026-08-08 10:00:00-05', NULL, 5),
  (12, 12, 63000, 'CALCULADO', 45000.0000, 40, NULL, '2026-08-10 10:00:00-05', '2026-09-03 10:00:00-05', 5),
  (13, 13, 315000, 'CALCULADO', 45000.0000, 40, NULL, '2026-08-10 10:00:00-05', '2026-09-03 10:00:00-05', 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (1, 'AP', 1, '2026-08-03 07:00:00-05', '2026-08-01', 'CONFIRMADO', NULL, NULL, NULL, NULL, NULL, 'Migración inicial desde Excel', 0, 0, 0, 0, 0, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 2, '2026-08-03 07:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 1, 1, 1, NULL, NULL, 1, 20, 1, 20, NULL, 18000, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL),
  (2, 1, 2, 2, 2, NULL, NULL, 1, 300, 1, 300, NULL, 2100, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL),
  (3, 1, 3, 3, 4, NULL, NULL, 1, 2000, 1, 2000, NULL, 60, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL),
  (4, 1, 4, 4, 7, NULL, NULL, 1, 1000, 1, 1000, NULL, 620, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL),
  (5, 1, 5, 5, 8, NULL, NULL, 1, 50, 1, 50, NULL, 5500, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL),
  (6, 1, 6, 6, 9, NULL, NULL, 1, 12, 1, 12, NULL, 9000, NULL, 0, 0, 0, 0, 0, 0, NULL, '2026-09-30', NULL, NULL, NULL);
INSERT INTO lote (id, producto_id, documento_linea_id, fecha_ingreso, fecha_vencimiento, costo_unitario_base, cantidad_inicial, saldo_disponible, saldo_reservado, saldo_danado) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 1, '2026-08-03 07:00:00-05', NULL, 18000.0000, 20, 18, 0, 0),
  (2, 2, 2, '2026-08-03 07:00:00-05', NULL, 2100.0000, 300, 285, 0, 0),
  (3, 3, 3, '2026-08-03 07:00:00-05', NULL, 60.0000, 2000, 1895, 0, 0),
  (4, 4, 4, '2026-08-03 07:00:00-05', NULL, 620.0000, 1000, 797.5, 0, 0),
  (5, 5, 5, '2026-08-03 07:00:00-05', NULL, 5500.0000, 50, 45, 2, 0),
  (6, 6, 6, '2026-08-03 07:00:00-05', '2026-09-30', 9000.0000, 12, 0, 0, 8);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (1, '2026-08-03 07:00:00-05', '2026-08-01', 'APERTURA', 1, 1, 1, 1, NULL, NULL, 'DISPONIBLE', 20, 18000.0000, 360000, 2),
  (2, '2026-08-03 07:00:00-05', '2026-08-01', 'APERTURA', 2, 2, 1, 2, NULL, NULL, 'DISPONIBLE', 300, 2100.0000, 630000, 2),
  (3, '2026-08-03 07:00:00-05', '2026-08-01', 'APERTURA', 3, 3, 1, 3, NULL, NULL, 'DISPONIBLE', 2000, 60.0000, 120000, 2),
  (4, '2026-08-03 07:00:00-05', '2026-08-01', 'APERTURA', 4, 4, 1, 4, NULL, NULL, 'DISPONIBLE', 1000, 620.0000, 620000, 2),
  (5, '2026-08-03 07:00:00-05', '2026-08-01', 'APERTURA', 5, 5, 1, 5, NULL, NULL, 'DISPONIBLE', 50, 5500.0000, 275000, 2),
  (6, '2026-08-03 07:00:00-05', '2026-08-01', 'APERTURA', 6, 6, 1, 6, NULL, NULL, 'DISPONIBLE', 12, 9000.0000, 108000, 2);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (2, 'OC', 1, '2026-08-05 10:00:00-05', '2026-08-01', 'EMITIDA', 2, NULL, NULL, NULL, NULL, NULL, 832000, 0, 158080, 0, 990080, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 5, '2026-08-05 10:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (7, 2, 1, 7, 10, NULL, NULL, 1, 100, 1, 100, NULL, 4000, NULL, 0, 0, 19, 76000, 400000, 476000, NULL, NULL, NULL, NULL, NULL),
  (8, 2, 2, 8, 11, NULL, NULL, 1, 24, 1, 24, NULL, 18000, NULL, 0, 0, 19, 82080, 432000, 514080, NULL, NULL, NULL, NULL, NULL);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (3, 'DI', 1, '2026-08-08 10:00:00-05', '2026-08-01', 'CONFIRMADO', 2, 2, NULL, NULL, 'FE-PAV-1201', NULL, 672000, 0, 127680, 0, 799680, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 5, '2026-08-08 10:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (9, 3, 1, 7, 10, NULL, 7, 1, 60, 1, 60, NULL, 4000, NULL, 0, 0, 19, 45600, 240000, 285600, NULL, NULL, NULL, NULL, NULL),
  (10, 3, 2, 8, 11, NULL, 8, 1, 24, 1, 24, NULL, 18000, NULL, 0, 0, 19, 82080, 432000, 514080, NULL, '2027-02-28', NULL, NULL, NULL);
INSERT INTO lote (id, producto_id, documento_linea_id, fecha_ingreso, fecha_vencimiento, costo_unitario_base, cantidad_inicial, saldo_disponible, saldo_reservado, saldo_danado) OVERRIDING SYSTEM VALUE VALUES
  (7, 7, 9, '2026-08-08 10:00:00-05', NULL, 4000.0000, 60, 50, 0, 0),
  (8, 8, 10, '2026-08-08 10:00:00-05', '2027-02-28', 18000.0000, 24, 22, 0, 0);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (7, '2026-08-08 10:00:00-05', '2026-08-01', 'ENTRADA_COMPRA', 7, 7, 3, 9, NULL, NULL, 'DISPONIBLE', 60, 4000.0000, 240000, 5),
  (8, '2026-08-08 10:00:00-05', '2026-08-01', 'ENTRADA_COMPRA', 8, 8, 3, 10, NULL, NULL, 'DISPONIBLE', 24, 18000.0000, 432000, 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (4, 'DI', 2, '2026-08-10 10:00:00-05', '2026-08-01', 'CONFIRMADO', 3, NULL, NULL, NULL, 'FV-PH-889', NULL, 450000, 0, 85500, 0, 535500, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 5, '2026-08-10 10:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (11, 4, 1, 9, 12, NULL, NULL, 1, 10, 1, 10, NULL, 45000, NULL, 0, 0, 19, 85500, 450000, 535500, NULL, '2027-06-30', NULL, NULL, NULL);
INSERT INTO lote (id, producto_id, documento_linea_id, fecha_ingreso, fecha_vencimiento, costo_unitario_base, cantidad_inicial, saldo_disponible, saldo_reservado, saldo_danado) OVERRIDING SYSTEM VALUE VALUES
  (9, 9, 11, '2026-08-10 10:00:00-05', '2027-06-30', 45000.0000, 10, 5, 0, 0);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (9, '2026-08-10 10:00:00-05', '2026-08-01', 'ENTRADA_COMPRA', 9, 9, 4, 11, NULL, NULL, 'DISPONIBLE', 10, 45000.0000, 450000, 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (5, 'FV', 1, '2026-08-12 11:15:00-05', '2026-08-01', 'CONFIRMADO', 1, NULL, NULL, NULL, NULL, NULL, 87958, 0, 16712, -20, 104650, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 6, '2026-08-12 11:15:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (12, 5, 1, 1, 1, NULL, NULL, 1, 2, 1, 2, NULL, NULL, 23400, 0, 0, 19, 8892, 46800, 55692, 36000, NULL, '2027-02-12', NULL, NULL),
  (13, 5, 2, 2, 2, NULL, NULL, 1, 15, 1, 15, NULL, NULL, 2625, 0, 0, 19, 7481, 39375, 46856, 31500, NULL, NULL, NULL, NULL),
  (14, 5, 3, 4, 7, NULL, NULL, 1, 2.5, 1, 2.5, NULL, NULL, 713, 0, 0, 19, 339, 1783, 2122, 1550, NULL, NULL, NULL, NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (10, '2026-08-12 11:15:00-05', '2026-08-01', 'SALIDA_VENTA', 1, 1, 5, 12, NULL, 'DISPONIBLE', NULL, 2, 18000.0000, 36000, 6),
  (11, '2026-08-12 11:15:00-05', '2026-08-01', 'SALIDA_VENTA', 2, 2, 5, 13, NULL, 'DISPONIBLE', NULL, 15, 2100.0000, 31500, 6),
  (12, '2026-08-12 11:15:00-05', '2026-08-01', 'SALIDA_VENTA', 4, 4, 5, 14, NULL, 'DISPONIBLE', NULL, 2.5, 620.0000, 1550, 6);
INSERT INTO pago (id, documento_id, medio, valor, valor_recibido, cambio, referencia) OVERRIDING SYSTEM VALUE VALUES
  (1, 5, 'EFECTIVO', 104650, 110000, 5350, NULL);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (6, 'FV', 2, '2026-08-20 15:40:00-05', '2026-08-01', 'CONFIRMADO', 4, NULL, NULL, NULL, NULL, NULL, 517000, 7130, 96875, 0, 606745, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 6, '2026-08-20 15:40:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (15, 6, 1, 9, 13, NULL, NULL, 1, 1, 5, 5, NULL, NULL, 315000, 0, 0, 19, 59850, 315000, 374850, 225000, NULL, NULL, NULL, NULL),
  (16, 6, 2, 3, 5, NULL, NULL, 1, 1, 100, 100, NULL, NULL, 9000, 0, 0, 19, 1710, 9000, 10710, 6000, NULL, NULL, NULL, NULL),
  (17, 6, 3, 4, 6, NULL, NULL, 1, 4, 50, 200, NULL, NULL, 35650, 5, 7130, 19, 25739, 142600, 161209, 124000, NULL, NULL, NULL, NULL),
  (18, 6, 4, 6, 9, NULL, NULL, 1, 4, 1, 4, NULL, NULL, 12600, 0, 0, 19, 9576, 50400, 59976, 36000, NULL, NULL, NULL, NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (13, '2026-08-20 15:40:00-05', '2026-08-01', 'SALIDA_VENTA', 9, 9, 6, 15, NULL, 'DISPONIBLE', NULL, 5, 45000.0000, 225000, 6),
  (14, '2026-08-20 15:40:00-05', '2026-08-01', 'SALIDA_VENTA', 3, 3, 6, 16, NULL, 'DISPONIBLE', NULL, 100, 60.0000, 6000, 6),
  (15, '2026-08-20 15:40:00-05', '2026-08-01', 'SALIDA_VENTA', 4, 4, 6, 17, NULL, 'DISPONIBLE', NULL, 200, 620.0000, 124000, 6),
  (16, '2026-08-20 15:40:00-05', '2026-08-01', 'SALIDA_VENTA', 6, 6, 6, 18, NULL, 'DISPONIBLE', NULL, 4, 9000.0000, 36000, 6);
INSERT INTO pago (id, documento_id, medio, valor, valor_recibido, cambio, referencia) OVERRIDING SYSTEM VALUE VALUES
  (2, 6, 'CREDITO', 606745, NULL, NULL, 'REF-6');
INSERT INTO cambio_estado (id, lote_id, estado_origen, estado_destino, cantidad, motivo_id, reserva_id, usuario_id, autorizado_por, fecha, periodo, observacion) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 'DISPONIBLE', 'DANADA', 1, 1, NULL, 4, NULL, '2026-08-25 09:00:00-05', '2026-08-01', NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (17, '2026-08-25 09:00:00-05', '2026-08-01', 'CAMBIO_ESTADO', 1, 1, NULL, NULL, 1, 'DISPONIBLE', 'DANADA', 1, 18000.0000, 18000, 4);
UPDATE documento SET estado = 'RECIBIDA_PARCIAL' WHERE id = 2;  -- OC-1 tras DI-1
INSERT INTO conteo (id, tipo, estado, alcance, periodo, iniciado_por, iniciado_en, cerrado_en, observacion) OVERRIDING SYSTEM VALUE VALUES
  (1, 'TOTAL', 'CERRADO', NULL, '2026-08-01', 4, '2026-08-31 17:00:00-05', '2026-08-31 18:00:00-05', 'Conteo total de cierre de agosto');
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (7, 'AJ', 1, '2026-08-31 19:00:00-05', '2026-08-01', 'APROBADO', NULL, NULL, 1, 6, NULL, NULL, 0, 0, 0, 0, 0, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 4, '2026-08-31 19:00:00-05', 3, '2026-08-31 19:30:00-05');
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (19, 7, 1, 3, 4, 3, NULL, -1, 5, 1, 5, 'DISPONIBLE', NULL, NULL, 0, 0, 0, 0, 0, 0, 300, NULL, NULL, NULL, 6);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (18, '2026-08-31 19:30:00-05', '2026-08-01', 'AJUSTE_NEGATIVO', 3, 3, 7, 19, NULL, 'DISPONIBLE', NULL, 5, 60.0000, 300, 3);
INSERT INTO conteo_linea (id, conteo_id, producto_id, lote_id, contado_buen_estado, contado_danado, registrado_buen_estado, registrado_danado, capturado_por, capturado_en, ajuste_linea_id) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 1, NULL, 17, 1, 17, 1, 4, '2026-08-31 18:00:00-05', NULL),
  (2, 1, 2, NULL, 285, 0, 285, 0, 4, '2026-08-31 18:00:00-05', NULL),
  (3, 1, 3, NULL, 1895, 0, 1900, 0, 4, '2026-08-31 18:00:00-05', 19),
  (4, 1, 4, NULL, 797.5, 0, 797.5, 0, 4, '2026-08-31 18:00:00-05', NULL),
  (5, 1, 5, NULL, 50, 0, 50, 0, 4, '2026-08-31 18:00:00-05', NULL),
  (6, 1, 6, 6, 8, 0, 8, 0, 4, '2026-08-31 18:00:00-05', NULL),
  (7, 1, 7, NULL, 60, 0, 60, 0, 4, '2026-08-31 18:00:00-05', NULL),
  (8, 1, 8, 8, 24, 0, 24, 0, 4, '2026-08-31 18:00:00-05', NULL),
  (9, 1, 9, 9, 5, 0, 5, 0, 4, '2026-08-31 18:00:00-05', NULL);
INSERT INTO cierre_saldo (periodo, lote_id, saldo_disponible, saldo_reservado, saldo_danado, costo_unitario_base, valor) VALUES
  ('2026-08-01', 1, 17, 0, 1, 18000.0000, 324000),
  ('2026-08-01', 2, 285, 0, 0, 2100.0000, 598500),
  ('2026-08-01', 3, 1895, 0, 0, 60.0000, 113700),
  ('2026-08-01', 4, 797.5, 0, 0, 620.0000, 494450),
  ('2026-08-01', 5, 50, 0, 0, 5500.0000, 275000),
  ('2026-08-01', 6, 8, 0, 0, 9000.0000, 72000),
  ('2026-08-01', 7, 60, 0, 0, 4000.0000, 240000),
  ('2026-08-01', 8, 24, 0, 0, 18000.0000, 432000),
  ('2026-08-01', 9, 5, 0, 0, 45000.0000, 225000);
UPDATE periodo SET estado = 'CERRADO', conteo_id = 1, cerrado_por = 3, cerrado_en = '2026-09-01 07:30:00-05'
 WHERE periodo = '2026-08-01';
INSERT INTO periodo_evento (periodo, accion, usuario_id, fecha) VALUES ('2026-08-01', 'CIERRE', 3, '2026-09-01 07:30:00-05');

-- ---------------- SEPTIEMBRE Y OCTUBRE 2026 ----------------
INSERT INTO reserva (id, cliente_id, presentacion_id, producto_id, cantidad, cantidad_base, fecha_limite, estado, factura_id, creado_por, creado_en, cerrado_en) OVERRIDING SYSTEM VALUE VALUES
  (1, 4, 10, 7, 10, 10, '2026-09-19', 'ACTIVA', NULL, 6, '2026-09-12 09:00:00-05', NULL),
  (2, 6, 8, 5, 2, 2, '2026-10-22', 'ACTIVA', NULL, 6, '2026-10-15 09:30:00-05', NULL);
INSERT INTO reserva_lote (reserva_id, lote_id, cantidad) VALUES
  (1, 7, 10),
  (2, 5, 2);
INSERT INTO precio_presentacion (id, presentacion_id, precio_base, origen, costo_base_unitario, porcentaje_incremento, motivo, vigente_desde, vigente_hasta, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (14, 8, 7900, 'MANUAL', 5500.0000, 30, 'Ajuste a precio de mercado', '2026-09-01 08:00:00-05', NULL, 3),
  (15, 10, 5670, 'CALCULADO', 4200.0000, 35, NULL, '2026-09-02 10:00:00-05', NULL, 5),
  (16, 12, 65800, 'CALCULADO', 47000.0000, 40, NULL, '2026-09-03 10:00:00-05', NULL, 5),
  (17, 13, 329000, 'CALCULADO', 47000.0000, 40, NULL, '2026-09-03 10:00:00-05', NULL, 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (8, 'DI', 3, '2026-09-02 10:00:00-05', '2026-09-01', 'CONFIRMADO', 2, 2, NULL, NULL, 'FE-PAV-1288', NULL, 168000, 0, 31920, 0, 199920, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 5, '2026-09-02 10:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (20, 8, 1, 7, 10, NULL, 7, 1, 40, 1, 40, NULL, 4200, NULL, 0, 0, 19, 31920, 168000, 199920, NULL, NULL, NULL, NULL, NULL);
INSERT INTO lote (id, producto_id, documento_linea_id, fecha_ingreso, fecha_vencimiento, costo_unitario_base, cantidad_inicial, saldo_disponible, saldo_reservado, saldo_danado) OVERRIDING SYSTEM VALUE VALUES
  (10, 7, 20, '2026-09-02 10:00:00-05', NULL, 4200.0000, 40, 40, 0, 0);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (19, '2026-09-02 10:00:00-05', '2026-09-01', 'ENTRADA_COMPRA', 7, 10, 8, 20, NULL, NULL, 'DISPONIBLE', 40, 4200.0000, 168000, 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (9, 'DI', 4, '2026-09-03 10:00:00-05', '2026-09-01', 'CONFIRMADO', 3, NULL, NULL, NULL, 'FV-PH-951', NULL, 470000, 0, 89300, 0, 559300, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 5, '2026-09-03 10:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (21, 9, 1, 9, 12, NULL, NULL, 1, 10, 1, 10, NULL, 47000, NULL, 0, 0, 19, 89300, 470000, 559300, NULL, '2027-03-31', NULL, NULL, NULL);
INSERT INTO lote (id, producto_id, documento_linea_id, fecha_ingreso, fecha_vencimiento, costo_unitario_base, cantidad_inicial, saldo_disponible, saldo_reservado, saldo_danado) OVERRIDING SYSTEM VALUE VALUES
  (11, 9, 21, '2026-09-03 10:00:00-05', '2027-03-31', 47000.0000, 10, 2, 0, 0);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (20, '2026-09-03 10:00:00-05', '2026-09-01', 'ENTRADA_COMPRA', 9, 11, 9, 21, NULL, NULL, 'DISPONIBLE', 10, 47000.0000, 470000, 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (10, 'FV', 3, '2026-09-05 16:20:00-05', '2026-09-01', 'CONFIRMADO', 5, NULL, NULL, NULL, NULL, NULL, 526400, 0, 100016, -16, 626400, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 6, '2026-09-05 16:20:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (22, 10, 1, 9, 12, NULL, NULL, 1, 8, 1, 8, NULL, NULL, 65800, 0, 0, 19, 100016, 526400, 626416, 376000, NULL, NULL, NULL, NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (21, '2026-09-05 16:20:00-05', '2026-09-01', 'SALIDA_VENTA', 9, 11, 10, 22, NULL, 'DISPONIBLE', NULL, 8, 47000.0000, 376000, 6);
INSERT INTO pago (id, documento_id, medio, valor, valor_recibido, cambio, referencia) OVERRIDING SYSTEM VALUE VALUES
  (3, 10, 'DEBITO', 400000, NULL, NULL, 'REF-10'),
  (4, 10, 'EFECTIVO', 226400, 230000, 3600, NULL);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (11, 'NC', 1, '2026-09-10 10:30:00-05', '2026-09-01', 'CONFIRMADO', 1, 5, NULL, 12, NULL, NULL, 23400, 0, 4446, 4, 27850, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 6, '2026-09-10 10:30:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (23, 11, 1, 1, 1, NULL, 12, 1, 1, 1, 1, 'DISPONIBLE', NULL, 23400, 0, 0, 19, 4446, 23400, 27846, 18000, NULL, NULL, NULL, 12);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (22, '2026-09-10 10:30:00-05', '2026-09-01', 'DEVOLUCION_CLIENTE', 1, 1, 11, 23, NULL, NULL, 'DISPONIBLE', 1, 18000.0000, 18000, 6);
INSERT INTO pago (id, documento_id, medio, valor, valor_recibido, cambio, referencia) OVERRIDING SYSTEM VALUE VALUES
  (5, 11, 'EFECTIVO', 27850, NULL, NULL, NULL);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (12, 'DP', 1, '2026-09-15 10:00:00-05', '2026-09-01', 'CONFIRMADO', 2, 3, NULL, 16, NULL, NULL, 36000, 0, 6840, 0, 42840, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 5, '2026-09-15 10:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (24, 12, 1, 8, 11, 8, 10, 1, 2, 1, 2, 'DISPONIBLE', 18000, NULL, 0, 0, 19, 6840, 36000, 42840, 36000, NULL, NULL, NULL, 16);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (24, '2026-09-15 10:00:00-05', '2026-09-01', 'DEVOLUCION_PROVEEDOR', 8, 8, 12, 24, NULL, 'DISPONIBLE', NULL, 2, 18000.0000, 36000, 5);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (13, 'FV', 4, '2026-09-16 14:00:00-05', '2026-09-01', 'CONFIRMADO', 4, NULL, NULL, NULL, NULL, NULL, 56700, 0, 10773, 0, 67473, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 6, '2026-09-16 14:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (25, 13, 1, 7, 10, NULL, NULL, 1, 10, 1, 10, NULL, NULL, 5670, 0, 0, 19, 10773, 56700, 67473, 40000, NULL, NULL, NULL, NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (25, '2026-09-16 14:00:00-05', '2026-09-01', 'SALIDA_VENTA', 7, 7, 13, 25, NULL, 'RESERVADA', NULL, 10, 4000.0000, 40000, 6);
INSERT INTO pago (id, documento_id, medio, valor, valor_recibido, cambio, referencia) OVERRIDING SYSTEM VALUE VALUES
  (6, 13, 'TRANSFERENCIA', 67473, NULL, NULL, 'REF-13');
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (14, 'AB', 1, '2026-09-20 10:00:00-05', '2026-09-01', 'APROBADO', NULL, NULL, NULL, 10, NULL, NULL, 0, 0, 0, 0, 0, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 4, '2026-09-20 10:00:00-05', 3, '2026-09-21 08:30:00-05');
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (26, 14, 1, 1, 1, 1, NULL, 1, 1, 1, 1, 'DANADA', NULL, NULL, 0, 0, 0, 0, 0, 0, 18000, NULL, NULL, 'RECICLAJE', 10);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (26, '2026-09-21 08:30:00-05', '2026-09-01', 'BAJA', 1, 1, 14, 26, NULL, 'DANADA', NULL, 1, 18000.0000, 18000, 3);
INSERT INTO documento (id, tipo, numero, fecha, periodo, estado, tercero_id, documento_origen_id, conteo_id, motivo_id, referencia_externa, observacion, subtotal, descuento, iva, ajuste_redondeo, total, emisor, creado_por, creado_en, aprobado_por, aprobado_en) OVERRIDING SYSTEM VALUE VALUES
  (15, 'FV', 5, '2026-10-05 12:00:00-05', '2026-10-01', 'CONFIRMADO', 5, NULL, NULL, NULL, NULL, NULL, 23700, 0, 4503, 0, 28203, '{"nit": "901234567", "digito_verificacion": 7, "razon_social": "Ferretería Surcolombiana S.A.S.", "direccion": "Calle 8 # 5-20, Neiva", "telefono": "6088712345", "correo": "facturacion@ferreteria.test"}'::jsonb, 6, '2026-10-05 12:00:00-05', NULL, NULL);
INSERT INTO documento_linea (id, documento_id, numero_linea, producto_id, presentacion_id, lote_id, linea_origen_id, sentido, cantidad, factor_conversion, cantidad_base, estado_inventario, costo_unitario, precio_unitario, descuento_pct, descuento_valor, tarifa_iva, iva_valor, subtotal, total, costo_total, fecha_vencimiento, fecha_fin_garantia, destino_baja, motivo_id) OVERRIDING SYSTEM VALUE VALUES
  (27, 15, 1, 5, 8, NULL, NULL, 1, 3, 1, 3, NULL, NULL, 7900, 0, 0, 19, 4503, 23700, 28203, 16500, NULL, '2027-10-05', NULL, NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (28, '2026-10-05 12:00:00-05', '2026-10-01', 'SALIDA_VENTA', 5, 5, 15, 27, NULL, 'DISPONIBLE', NULL, 3, 5500.0000, 16500, 6);
INSERT INTO pago (id, documento_id, medio, valor, valor_recibido, cambio, referencia) OVERRIDING SYSTEM VALUE VALUES
  (7, 15, 'TRANSFERENCIA', 28203, NULL, NULL, 'REF-15');
INSERT INTO cambio_estado (id, lote_id, estado_origen, estado_destino, cantidad, motivo_id, reserva_id, usuario_id, autorizado_por, fecha, periodo, observacion) OVERRIDING SYSTEM VALUE VALUES
  (2, 7, 'DISPONIBLE', 'RESERVADA', 10, NULL, 1, 6, NULL, '2026-09-12 09:00:00-05', '2026-09-01', NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (23, '2026-09-12 09:00:00-05', '2026-09-01', 'CAMBIO_ESTADO', 7, 7, NULL, NULL, 2, 'DISPONIBLE', 'RESERVADA', 10, 4000.0000, 40000, 6);
INSERT INTO cambio_estado (id, lote_id, estado_origen, estado_destino, cantidad, motivo_id, reserva_id, usuario_id, autorizado_por, fecha, periodo, observacion) OVERRIDING SYSTEM VALUE VALUES
  (3, 6, 'DISPONIBLE', 'DANADA', 8, 4, NULL, 1, NULL, '2026-10-01 00:05:00-05', '2026-10-01', NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (27, '2026-10-01 00:05:00-05', '2026-10-01', 'CAMBIO_ESTADO', 6, 6, NULL, NULL, 3, 'DISPONIBLE', 'DANADA', 8, 9000.0000, 72000, 1);
INSERT INTO cambio_estado (id, lote_id, estado_origen, estado_destino, cantidad, motivo_id, reserva_id, usuario_id, autorizado_por, fecha, periodo, observacion) OVERRIDING SYSTEM VALUE VALUES
  (4, 5, 'DISPONIBLE', 'RESERVADA', 2, NULL, 2, 6, NULL, '2026-10-15 09:30:00-05', '2026-10-01', NULL);
INSERT INTO movimiento_inventario (id, fecha, periodo, tipo, producto_id, lote_id, documento_id, documento_linea_id, cambio_estado_id, estado_origen, estado_destino, cantidad, costo_unitario_base, costo_total, usuario_id) OVERRIDING SYSTEM VALUE VALUES
  (29, '2026-10-15 09:30:00-05', '2026-10-01', 'CAMBIO_ESTADO', 5, 5, NULL, NULL, 4, 'DISPONIBLE', 'RESERVADA', 2, 5500.0000, 11000, 6);
UPDATE documento SET estado = 'RECIBIDA' WHERE id = 2;  -- OC-1 tras DI-3
UPDATE reserva SET estado = 'CONSUMIDA', factura_id = 13, cerrado_en = '2026-09-16 14:00:00-05' WHERE id = 1;
UPDATE precio_presentacion SET vigente_hasta = v.hasta FROM (VALUES (8, '2026-09-01 08:00:00-05'::timestamptz), (10, '2026-09-02 10:00:00-05'::timestamptz), (12, '2026-09-03 10:00:00-05'::timestamptz), (13, '2026-09-03 10:00:00-05'::timestamptz)) AS v(id, hasta) WHERE precio_presentacion.id = v.id;
UPDATE producto SET ultimo_costo_base = v.c FROM (VALUES (1, 18000.0000), (2, 2100.0000), (3, 60.0000), (4, 620.0000), (5, 5500.0000), (6, 9000.0000), (7, 4200.0000), (8, 18000.0000), (9, 47000.0000)) AS v(id, c) WHERE producto.id = v.id;

-- Consecutivos y secuencias de identidad
UPDATE consecutivo SET siguiente_numero = v.n FROM (VALUES ('OC', 2), ('DI', 5), ('FV', 6), ('NC', 2), ('DP', 2), ('AB', 2), ('AJ', 2), ('AP', 2)) AS v(t, n) WHERE tipo_documento = v.t;
SELECT setval(pg_get_serial_sequence('usuario', 'id'), (SELECT max(id) FROM usuario));
SELECT setval(pg_get_serial_sequence('marca', 'id'), (SELECT max(id) FROM marca));
SELECT setval(pg_get_serial_sequence('categoria', 'id'), (SELECT max(id) FROM categoria));
SELECT setval(pg_get_serial_sequence('producto', 'id'), (SELECT max(id) FROM producto));
SELECT setval(pg_get_serial_sequence('presentacion', 'id'), (SELECT max(id) FROM presentacion));
SELECT setval(pg_get_serial_sequence('tercero', 'id'), (SELECT max(id) FROM tercero));
SELECT setval(pg_get_serial_sequence('precio_presentacion', 'id'), (SELECT max(id) FROM precio_presentacion));
SELECT setval(pg_get_serial_sequence('documento', 'id'), (SELECT max(id) FROM documento));
SELECT setval(pg_get_serial_sequence('documento_linea', 'id'), (SELECT max(id) FROM documento_linea));
SELECT setval(pg_get_serial_sequence('pago', 'id'), (SELECT max(id) FROM pago));
SELECT setval(pg_get_serial_sequence('lote', 'id'), (SELECT max(id) FROM lote));
SELECT setval(pg_get_serial_sequence('reserva', 'id'), (SELECT max(id) FROM reserva));
SELECT setval(pg_get_serial_sequence('cambio_estado', 'id'), (SELECT max(id) FROM cambio_estado));
SELECT setval(pg_get_serial_sequence('movimiento_inventario', 'id'), (SELECT max(id) FROM movimiento_inventario));
SELECT setval(pg_get_serial_sequence('conteo', 'id'), (SELECT max(id) FROM conteo));
SELECT setval(pg_get_serial_sequence('conteo_linea', 'id'), (SELECT max(id) FROM conteo_linea));

