-- Nombre: datos-volumen.sql
-- Descripción: Genera en la base aparte ferreteria_carga el volumen de diseño de RNF-REN-001 a 003:
--              100.000 presentaciones (50.000 productos con 2 presentaciones cada uno), con sus marcas,
--              categorías y precios vigentes, usando generate_series. Es la base de la prueba de carga
--              load-tests/productos.js en modo "carga".
-- Fecha de creación: 2026-10-21
-- Última actualización: 2026-10-21
-- Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
--
-- Requisitos
--   * La base se llama ferreteria_carga (el script se niega a correr en otra, para no tocar desarrollo
--     ni producción) y ya tiene el esquema aplicado por Flyway (V1 a V3). Ver load-tests/README.md.
--   * Volumen de diseño (especificación, sección 5): 100.000 presentaciones. Con la variable psql
--     "productos" se puede generar menos o más: psql ... -v productos=5000 -f datos-volumen.sql
--
-- Qué genera (todo en una transacción; si algo falla no queda nada):
--   * 200 marcas y 40 categorías con el prefijo "Carga".
--   * N productos (50.000 por defecto) con nombres realistas de ferretería, 4 unidades base (UND, MTR,
--     KG, GAL), las 3 tarifas de IVA, los 3 tipos de control y un último costo base.
--   * 2 presentaciones por producto (unidad base y empaque), con código EAN-13 de fabricante válido
--     (prefijo 7709, distinto del escenario de prueba) y factor de conversión.
--   * 1 precio vigente CALCULADO por presentación: costo × factor × (1 + porcentaje / 100) (RN-20).
--   * La auditoría (fn_auditar) registra cada fila a nombre del usuario Sistema, como lo haría una
--     migración real; así la tabla auditoria también tiene volumen.
--   No genera lotes ni movimientos: la prueba de carga ejercita el catálogo (búsqueda, consulta y
--   creación de productos); el kardex y las ventas se medirán cuando existan esos módulos.

\set ON_ERROR_STOP on
\if :{?productos}
\else
  \set productos 50000
\endif

BEGIN;

-- Guardas: base correcta, esquema aplicado y volumen aún no cargado
DO $$
BEGIN
  IF current_database() <> 'ferreteria_carga' THEN
    RAISE EXCEPTION 'datos-volumen.sql solo se ejecuta en la base ferreteria_carga, no en %', current_database();
  END IF;
  IF to_regclass('public.presentacion') IS NULL THEN
    RAISE EXCEPTION 'La base no tiene el esquema: arranque el backend contra ferreteria_carga para que Flyway lo cree';
  END IF;
  IF EXISTS (SELECT 1 FROM marca WHERE nombre LIKE 'Marca Carga %') THEN
    RAISE EXCEPTION 'El volumen ya está cargado; recree la base para generarlo de nuevo';
  END IF;
END $$;

-- La auditoría queda a nombre del usuario Sistema (RN-28)
SELECT set_config('app.usuario_id', (SELECT id::text FROM usuario WHERE es_sistema), true);

-- Marcas y categorías
INSERT INTO marca (nombre)
SELECT 'Marca Carga ' || lpad(g::text, 3, '0') FROM generate_series(1, 200) AS g;

INSERT INTO categoria (nombre)
SELECT 'Categoría Carga ' || lpad(g::text, 2, '0') FROM generate_series(1, 40) AS g;

-- Tipos de artículo con su unidad base y su empaque (nombre y factor)
CREATE TEMP TABLE tipo_carga (n int PRIMARY KEY, tipo text, unidad text, empaque text, factor numeric) ON COMMIT DROP;
INSERT INTO tipo_carga VALUES
  ( 0, 'Tornillo',       'UND', 'Caja x100',     100), ( 1, 'Tuerca',         'UND', 'Caja x100',     100),
  ( 2, 'Arandela',       'UND', 'Caja x100',     100), ( 3, 'Martillo',       'UND', 'Caja x6',         6),
  ( 4, 'Destornillador', 'UND', 'Caja x12',       12), ( 5, 'Llave',          'UND', 'Caja x12',       12),
  ( 6, 'Alicate',        'UND', 'Caja x6',         6), ( 7, 'Broca',          'UND', 'Caja x10',       10),
  ( 8, 'Bisagra',        'UND', 'Caja x24',       24), ( 9, 'Candado',        'UND', 'Caja x12',       12),
  (10, 'Bombillo',       'UND', 'Caja x10',       10), (11, 'Interruptor',    'UND', 'Caja x20',       20),
  (12, 'Tomacorriente',  'UND', 'Caja x20',       20), (13, 'Codo',           'UND', 'Bolsa x25',      25),
  (14, 'Tubo',           'UND', 'Paquete x10',    10), (15, 'Grifo',          'UND', 'Caja x6',         6),
  (16, 'Brocha',         'UND', 'Caja x12',       12), (17, 'Rodillo',        'UND', 'Caja x12',       12),
  (18, 'Lija',           'UND', 'Paquete x50',    50), (19, 'Cinta',          'UND', 'Caja x24',       24),
  (20, 'Cable',          'MTR', 'Rollo 100 m',   100), (21, 'Manguera',       'MTR', 'Rollo 50 m',     50),
  (22, 'Cadena',         'MTR', 'Rollo 25 m',     25), (23, 'Malla',          'MTR', 'Rollo 30 m',     30),
  (24, 'Cemento',        'KG',  'Bulto 50 kg',    50), (25, 'Clavo',          'KG',  'Caja 25 kg',     25),
  (26, 'Estuco',         'KG',  'Bulto 25 kg',    25), (27, 'Yeso',           'KG',  'Bulto 40 kg',    40),
  (28, 'Pintura',        'GAL', 'Cuñete 5 gal',    5), (29, 'Esmalte',        'GAL', 'Cuñete 5 gal',    5),
  (30, 'Impermeabilizante','GAL','Cuñete 5 gal',   5), (31, 'Thinner',        'GAL', 'Caneca 5 gal',    5);

-- Productos: nombre = tipo + material + medida + serie, único por marca (RF-PRO-001)
INSERT INTO producto (nombre, marca_id, categoria_id, unidad_base, tarifa_iva_id, porcentaje_incremento,
                      control, plazo_garantia_meses, descripcion, ultimo_costo_base)
SELECT t.tipo || ' '
         || (ARRAY['acero','galvanizado','inoxidable','PVC','cobre','bronce','plástico','aluminio',
                   'industrial','profesional','económico','reforzado'])[1 + g % 12] || ' '
         || (ARRAY['1/4"','3/8"','1/2"','3/4"','1"','2"','6 mm','8 mm','10 mm','12 mm','N.º 8','N.º 12'])[1 + (g / 12) % 12]
         || ' serie ' || lpad(g::text, 6, '0'),
       (SELECT min(id) FROM marca WHERE nombre LIKE 'Marca Carga %') + g % 200,
       (SELECT min(id) FROM categoria WHERE nombre LIKE 'Categoría Carga %') + g % 40,
       t.unidad,
       CASE WHEN g % 20 = 0 THEN 1 WHEN g % 20 = 1 THEN 2 ELSE 3 END,          -- IVA 0, 5 o 19 %
       (10 + g % 60)::numeric,                                                   -- margen 10 a 69 %
       CASE WHEN g % 10 = 0 THEN 'GARANTIA' WHEN g % 25 = 1 THEN 'VENCIMIENTO' ELSE 'NINGUNO' END,
       CASE WHEN g % 10 = 0 THEN 6 + g % 19 END,                                 -- plazo 6 a 24 meses
       CASE WHEN g % 3 = 0 THEN 'Producto generado para la prueba de carga (RNF-REN).' END,
       round((200 + (g * 7919) % 30000)::numeric / CASE t.unidad WHEN 'UND' THEN 1 ELSE 3 END, 4)
  FROM generate_series(1, :productos) AS g
  JOIN tipo_carga t ON t.n = g % 32;

-- Presentaciones: 1 = unidad base (factor 1), 2 = empaque; código EAN-13 de fabricante único (RN-01)
INSERT INTO presentacion (producto_id, nombre, codigo_barras, codigo_interno, factor_conversion, admite_fraccion)
SELECT p.id,
       CASE k WHEN 1 THEN (SELECT nombre FROM unidad_medida WHERE codigo = p.unidad_base) ELSE t.empaque END,
       fn_ean13('7709' || lpad((p.id * 10 + k)::text, 8, '0')),
       false,
       CASE k WHEN 1 THEN 1 ELSE t.factor END,
       k = 1 AND p.unidad_base <> 'UND'
  FROM producto p
  JOIN tipo_carga t ON t.n = substring(p.nombre FROM 'serie ([0-9]+)$')::int % 32
 CROSS JOIN (VALUES (1), (2)) AS presentaciones(k)
 WHERE p.nombre ~ ' serie [0-9]{6}$'
   AND p.marca_id >= (SELECT min(id) FROM marca WHERE nombre LIKE 'Marca Carga %');

-- Precio vigente calculado de cada presentación generada (RN-20, redondeo al peso RN-06)
INSERT INTO precio_presentacion (presentacion_id, precio_base, origen, costo_base_unitario,
                                 porcentaje_incremento, usuario_id)
SELECT pr.id,
       greatest(1, round(p.ultimo_costo_base * pr.factor_conversion * (1 + p.porcentaje_incremento / 100))),
       'CALCULADO', p.ultimo_costo_base, p.porcentaje_incremento,
       (SELECT id FROM usuario WHERE es_sistema)
  FROM presentacion pr
  JOIN producto p ON p.id = pr.producto_id
 WHERE pr.codigo_barras LIKE '7709%';

-- Verificación: el volumen generado es el pedido
DO $$
DECLARE
  v_productos     bigint := (SELECT count(*) FROM producto p JOIN marca m ON m.id = p.marca_id
                              WHERE m.nombre LIKE 'Marca Carga %');
  v_presentaciones bigint := (SELECT count(*) FROM presentacion WHERE codigo_barras LIKE '7709%');
  v_precios       bigint := (SELECT count(*) FROM precio_presentacion pp JOIN presentacion pr ON pr.id = pp.presentacion_id
                              WHERE pr.codigo_barras LIKE '7709%' AND pp.vigente_hasta IS NULL);
BEGIN
  IF v_presentaciones <> 2 * v_productos OR v_precios <> v_presentaciones THEN
    RAISE EXCEPTION 'Volumen inconsistente: % productos, % presentaciones, % precios',
      v_productos, v_presentaciones, v_precios;
  END IF;
  RAISE NOTICE 'Volumen generado: % productos, % presentaciones, % precios vigentes',
    v_productos, v_presentaciones, v_precios;
END $$;

COMMIT;

-- Estadísticas al día para que el planificador use los índices (trigram de nombre, únicos de código)
ANALYZE marca;
ANALYZE categoria;
ANALYZE producto;
ANALYZE presentacion;
ANALYZE precio_presentacion;
ANALYZE auditoria;

SELECT (SELECT count(*) FROM producto)            AS productos_total,
       (SELECT count(*) FROM presentacion)        AS presentaciones_total,
       (SELECT count(*) FROM precio_presentacion) AS precios_total,
       (SELECT count(*) FROM auditoria)           AS registros_auditoria,
       pg_size_pretty(pg_database_size(current_database())) AS tamano_base;
