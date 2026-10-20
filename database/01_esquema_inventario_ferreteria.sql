-- =====================================================================
-- Sistema de Inventario de Ferretería — Modelo de datos v1
-- Motor: PostgreSQL 16
-- Fuente: Especificación de Requisitos v4 (27 RF, 29 RN, 17 RNF)
-- Ejecución: psql -U postgres -d ferreteria -f 01_esquema_inventario_ferreteria.sql
-- =====================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- búsqueda por nombre (RNF-REN-001)

-- =====================================================================
-- 0. FUNCIONES DE APOYO (inmutables, usadas en restricciones)
-- =====================================================================

-- Dígito de verificación del NIT (algoritmo DIAN, módulo 11)   RN-26
CREATE OR REPLACE FUNCTION fn_dv_nit(p_nit text)
RETURNS smallint LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE
  pesos int[] := ARRAY[3,7,13,17,19,23,29,37,41,43,47,53,59,67,71];
  suma  int := 0;
  n     int := length(p_nit);
  r     int;
BEGIN
  IF p_nit !~ '^[0-9]{1,15}$' THEN
    RETURN NULL;
  END IF;
  FOR i IN 1..n LOOP
    suma := suma + substr(p_nit, n - i + 1, 1)::int * pesos[i];
  END LOOP;
  r := suma % 11;
  RETURN CASE WHEN r IN (0, 1) THEN r ELSE 11 - r END;
END $$;

-- EAN-13: agrega el dígito de control a 12 dígitos               RN-01
CREATE OR REPLACE FUNCTION fn_ean13(p_12 text)
RETURNS text LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE
  suma int := 0;
BEGIN
  IF p_12 !~ '^[0-9]{12}$' THEN
    RAISE EXCEPTION 'fn_ean13 requiere 12 dígitos, recibió %', p_12;
  END IF;
  FOR i IN 1..12 LOOP
    suma := suma + substr(p_12, i, 1)::int * CASE WHEN i % 2 = 1 THEN 1 ELSE 3 END;
  END LOOP;
  RETURN p_12 || ((10 - suma % 10) % 10)::text;
END $$;

-- Código interno de presentación: prefijo GS1 de uso interno 20  RN-01
CREATE OR REPLACE FUNCTION fn_codigo_interno(p_id bigint)
RETURNS text LANGUAGE sql IMMUTABLE STRICT AS $$
  SELECT fn_ean13('20' || lpad(p_id::text, 10, '0'));
$$;

-- =====================================================================
-- 1. SEGURIDAD
-- =====================================================================

CREATE TABLE usuario (
  id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario           text        NOT NULL UNIQUE,
  nombre_completo   text        NOT NULL,
  correo            text        NOT NULL UNIQUE,
  password_hash     text        NOT NULL,              -- bcrypt / Argon2 (RNF-SEG-001)
  activo            boolean     NOT NULL DEFAULT true,
  es_sistema        boolean     NOT NULL DEFAULT false, -- usuario de tareas programadas
  intentos_fallidos smallint    NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
  bloqueado_hasta   timestamptz,
  ultimo_acceso     timestamptz,
  creado_en         timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE rol (
  id     smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo text NOT NULL UNIQUE,
  nombre text NOT NULL
);

CREATE TABLE permiso (
  id          smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo      text NOT NULL UNIQUE,
  descripcion text NOT NULL
);

CREATE TABLE rol_permiso (
  rol_id     smallint NOT NULL REFERENCES rol (id),
  permiso_id smallint NOT NULL REFERENCES permiso (id),
  PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuario_rol (
  usuario_id bigint   NOT NULL REFERENCES usuario (id),
  rol_id     smallint NOT NULL REFERENCES rol (id),
  PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE api_credencial (                         -- RF-INT-001
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre      text        NOT NULL,
  token_hash  text        NOT NULL UNIQUE,
  usuario_id  bigint      NOT NULL REFERENCES usuario (id), -- usuario técnico que firma la auditoría
  activo      boolean     NOT NULL DEFAULT true,
  creado_en   timestamptz NOT NULL DEFAULT now(),
  ultimo_uso  timestamptz
);

-- =====================================================================
-- 2. CONFIGURACIÓN Y CATÁLOGOS
-- =====================================================================

CREATE TABLE empresa (                                -- RF-CFG-001 (fila única)
  id                  smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  nit                 varchar(15) NOT NULL CHECK (nit ~ '^[0-9]+$'),
  digito_verificacion smallint    NOT NULL,
  razon_social        text        NOT NULL,
  direccion           text        NOT NULL,
  telefono            varchar(10) NOT NULL,
  correo              text        NOT NULL,
  logo_url            text,
  actualizado_en      timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_empresa_dv CHECK (digito_verificacion = fn_dv_nit(nit))
);

CREATE TABLE consecutivo (                            -- RN-11: numeración sin saltos
  tipo_documento   char(2) PRIMARY KEY
                   CHECK (tipo_documento IN ('OC','DI','FV','NC','DP','AB','AJ','AP')),
  prefijo          text    NOT NULL,
  siguiente_numero bigint  NOT NULL DEFAULT 1 CHECK (siguiente_numero > 0)
);

CREATE TABLE unidad_medida (                          -- RN-05
  codigo      varchar(4) PRIMARY KEY,
  nombre      text       NOT NULL,
  es_discreta boolean    NOT NULL
);

CREATE TABLE tarifa_iva (                             -- RN-08
  id         smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre     text         NOT NULL,
  porcentaje numeric(5,2) NOT NULL UNIQUE CHECK (porcentaje BETWEEN 0 AND 100),
  activo     boolean      NOT NULL DEFAULT true
);

CREATE TABLE marca (
  id     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre text    NOT NULL,
  activo boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX ux_marca_nombre ON marca (lower(nombre));

CREATE TABLE categoria (
  id     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre text    NOT NULL,
  activo boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX ux_categoria_nombre ON categoria (lower(nombre));

CREATE TABLE motivo (
  id     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tipo   text    NOT NULL
         CHECK (tipo IN ('DANO','AJUSTE','BAJA','DEV_CLIENTE','DEV_PROVEEDOR','ANULACION')),
  nombre text    NOT NULL,
  activo boolean NOT NULL DEFAULT true,
  UNIQUE (tipo, nombre)
);

-- =====================================================================
-- 3. CATÁLOGO DE PRODUCTOS Y PRECIOS
-- =====================================================================

CREATE TABLE producto (                               -- RF-PRO-001
  id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre                text          NOT NULL,
  marca_id              bigint        NOT NULL REFERENCES marca (id),
  categoria_id          bigint        NOT NULL REFERENCES categoria (id),
  unidad_base           varchar(4)    NOT NULL REFERENCES unidad_medida (codigo),
  tarifa_iva_id         smallint      NOT NULL REFERENCES tarifa_iva (id),
  porcentaje_incremento numeric(6,2)  NOT NULL CHECK (porcentaje_incremento BETWEEN 0 AND 500),
  control               text          NOT NULL DEFAULT 'NINGUNO'
                        CHECK (control IN ('NINGUNO','VENCIMIENTO','GARANTIA')),
  plazo_garantia_meses  smallint      CHECK (plazo_garantia_meses > 0),
  descripcion           text,
  ultimo_costo_base     numeric(18,4) CHECK (ultimo_costo_base >= 0),  -- RN-20, se actualiza en cada DI
  activo                boolean       NOT NULL DEFAULT true,
  creado_en             timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ck_producto_garantia
    CHECK ((control = 'GARANTIA') = (plazo_garantia_meses IS NOT NULL))
);
CREATE UNIQUE INDEX ux_producto_marca_nombre ON producto (marca_id, lower(nombre));

CREATE TABLE presentacion (                           -- RF-PRO-002
  id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  producto_id       bigint        NOT NULL REFERENCES producto (id),
  nombre            text          NOT NULL,
  codigo_barras     varchar(20)   NOT NULL UNIQUE CHECK (codigo_barras ~ '^[0-9A-Za-z-]+$'),
  codigo_interno    boolean       NOT NULL DEFAULT false,
  factor_conversion numeric(12,3) NOT NULL CHECK (factor_conversion > 0),
  admite_fraccion   boolean       NOT NULL,
  activo            boolean       NOT NULL DEFAULT true,
  UNIQUE (producto_id, nombre),
  CONSTRAINT ck_presentacion_codigo_interno
    CHECK (NOT codigo_interno OR codigo_barras ~ '^20[0-9]{11}$')
);

CREATE TABLE precio_presentacion (                    -- RF-PRE-001, RN-20
  id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  presentacion_id       bigint        NOT NULL REFERENCES presentacion (id),
  precio_base           bigint        NOT NULL CHECK (precio_base > 0),   -- sin IVA, COP
  origen                text          NOT NULL CHECK (origen IN ('CALCULADO','MANUAL')),
  costo_base_unitario   numeric(18,4),
  porcentaje_incremento numeric(6,2),
  motivo                text,
  vigente_desde         timestamptz   NOT NULL DEFAULT now(),
  vigente_hasta         timestamptz,
  usuario_id            bigint        NOT NULL REFERENCES usuario (id),
  CONSTRAINT ck_precio_manual_motivo CHECK (origen <> 'MANUAL' OR motivo IS NOT NULL),
  CONSTRAINT ck_precio_vigencia      CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);
-- Un solo precio vigente por presentación
CREATE UNIQUE INDEX ux_precio_vigente ON precio_presentacion (presentacion_id) WHERE vigente_hasta IS NULL;

-- =====================================================================
-- 4. TERCEROS
-- =====================================================================

CREATE TABLE tercero (                                -- RF-TER-001, RN-26
  id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tipo_persona          text        NOT NULL CHECK (tipo_persona IN ('NATURAL','JURIDICA')),
  tipo_identificacion   text        NOT NULL CHECK (tipo_identificacion IN ('CC','NIT')),
  numero_identificacion varchar(15) NOT NULL CHECK (numero_identificacion ~ '^[0-9]+$'),
  digito_verificacion   smallint,
  nombre                text,
  apellido              text,
  razon_social          text,
  direccion             text        NOT NULL,
  telefono              varchar(10) NOT NULL CHECK (telefono ~ '^[0-9]{7,10}$'),
  correo                text        CHECK (correo ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
  es_cliente            boolean     NOT NULL DEFAULT false,
  es_proveedor          boolean     NOT NULL DEFAULT false,
  es_consumidor_final   boolean     NOT NULL DEFAULT false,
  activo                boolean     NOT NULL DEFAULT true,
  creado_en             timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tipo_identificacion, numero_identificacion),
  CONSTRAINT ck_tercero_rol      CHECK (es_cliente OR es_proveedor),
  CONSTRAINT ck_tercero_juridica CHECK (tipo_persona = 'NATURAL' OR tipo_identificacion = 'NIT'),
  CONSTRAINT ck_tercero_nombres  CHECK (
       (tipo_persona = 'NATURAL'  AND nombre IS NOT NULL AND apellido IS NOT NULL)
    OR (tipo_persona = 'JURIDICA' AND razon_social IS NOT NULL)),
  CONSTRAINT ck_tercero_dv CHECK (
       (tipo_identificacion = 'NIT' AND digito_verificacion = fn_dv_nit(numero_identificacion))
    OR (tipo_identificacion = 'CC'  AND digito_verificacion IS NULL))
);
CREATE UNIQUE INDEX ux_tercero_consumidor_final ON tercero (es_consumidor_final) WHERE es_consumidor_final;

-- =====================================================================
-- 5. PERÍODOS Y CIERRES
-- =====================================================================

CREATE TABLE periodo (                                -- RF-CIE-001/002, RN-25
  periodo       date PRIMARY KEY CHECK (extract(day FROM periodo) = 1), -- primer día del mes
  estado        text        NOT NULL DEFAULT 'ABIERTO'
                CHECK (estado IN ('ABIERTO','CERRADO','REABIERTO')),
  conteo_id     bigint,                                -- FK agregada tras crear conteo
  cerrado_por   bigint      REFERENCES usuario (id),
  cerrado_en    timestamptz,
  observaciones text,
  CONSTRAINT ck_periodo_cierre CHECK (estado <> 'CERRADO' OR (cerrado_por IS NOT NULL AND cerrado_en IS NOT NULL))
);

CREATE TABLE periodo_evento (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  periodo    date        NOT NULL REFERENCES periodo (periodo),
  accion     text        NOT NULL CHECK (accion IN ('CIERRE','REAPERTURA')),
  usuario_id bigint      NOT NULL REFERENCES usuario (id),
  fecha      timestamptz NOT NULL DEFAULT now(),
  motivo     text,
  CONSTRAINT ck_reapertura_motivo CHECK (accion <> 'REAPERTURA' OR length(motivo) >= 20)
);

-- =====================================================================
-- 6. DOCUMENTOS (supertipo para OC, DI, FV, NC, DP, AB, AJ, AP)
-- =====================================================================

CREATE TABLE documento (
  id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tipo                char(2)     NOT NULL
                      CHECK (tipo IN ('OC','DI','FV','NC','DP','AB','AJ','AP')),
  numero              bigint,                         -- se asigna al confirmar/emitir/aprobar
  fecha               timestamptz NOT NULL DEFAULT now(),
  periodo             date        NOT NULL REFERENCES periodo (periodo),  -- lo asigna trigger
  estado              text        NOT NULL,
  tercero_id          bigint      REFERENCES tercero (id),
  documento_origen_id bigint      REFERENCES documento (id),  -- DI→OC, NC→FV, DP→DI
  conteo_id           bigint,                         -- AJ originado en conteo (FK más abajo)
  motivo_id           bigint      REFERENCES motivo (id),
  referencia_externa  text,                           -- factura del proveedor en DI
  observacion         text,
  subtotal            bigint      NOT NULL DEFAULT 0,
  descuento           bigint      NOT NULL DEFAULT 0,
  iva                 bigint      NOT NULL DEFAULT 0,
  ajuste_redondeo     bigint      NOT NULL DEFAULT 0, -- RN-07
  total               bigint      NOT NULL DEFAULT 0,
  emisor              jsonb,                          -- copia de empresa al emitir (RF-DOC-001)
  clave_idempotencia  text        UNIQUE,             -- RF-INT-001
  creado_por          bigint      NOT NULL REFERENCES usuario (id),
  creado_en           timestamptz NOT NULL DEFAULT now(),
  aprobado_por        bigint      REFERENCES usuario (id),
  aprobado_en         timestamptz,
  anulado_por         bigint      REFERENCES usuario (id),
  anulado_en          timestamptz,
  motivo_anulacion    text,
  UNIQUE (tipo, numero),
  CONSTRAINT ck_documento_estado CHECK (
       (tipo = 'OC'                    AND estado IN ('BORRADOR','EMITIDA','RECIBIDA_PARCIAL','RECIBIDA','CANCELADA'))
    OR (tipo IN ('DI','NC','DP','AP')  AND estado = 'CONFIRMADO')
    OR (tipo = 'FV'                    AND estado IN ('CONFIRMADO','ANULADO'))
    OR (tipo IN ('AB','AJ')            AND estado IN ('PROPUESTO','APROBADO','RECHAZADO'))),
  CONSTRAINT ck_documento_numero CHECK (numero IS NOT NULL OR estado IN ('BORRADOR','PROPUESTO','RECHAZADO')),
  CONSTRAINT ck_documento_tercero CHECK (tipo NOT IN ('OC','DI','FV','NC','DP') OR tercero_id IS NOT NULL),
  CONSTRAINT ck_documento_separacion CHECK (aprobado_por IS NULL OR aprobado_por <> creado_por), -- RN-27
  CONSTRAINT ck_documento_aprobacion CHECK (estado NOT IN ('APROBADO','RECHAZADO') OR aprobado_por IS NOT NULL),
  CONSTRAINT ck_documento_anulacion CHECK (
    estado <> 'ANULADO' OR (anulado_por IS NOT NULL AND anulado_en IS NOT NULL AND motivo_anulacion IS NOT NULL)),
  CONSTRAINT ck_documento_montos CHECK (subtotal >= 0 AND descuento >= 0 AND iva >= 0 AND total >= 0),
  CONSTRAINT ck_documento_total  CHECK (total = subtotal - descuento + iva + ajuste_redondeo)
);

CREATE TABLE documento_linea (
  id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  documento_id       bigint        NOT NULL REFERENCES documento (id),
  numero_linea       smallint      NOT NULL CHECK (numero_linea > 0),
  producto_id        bigint        NOT NULL REFERENCES producto (id),
  presentacion_id    bigint        REFERENCES presentacion (id),
  lote_id            bigint,                          -- AB, DP y AJ sobre un lote concreto (FK más abajo)
  linea_origen_id    bigint        REFERENCES documento_linea (id), -- DI→OC, NC→FV
  sentido            smallint      NOT NULL DEFAULT 1 CHECK (sentido IN (1, -1)), -- AJ positivo/negativo
  cantidad           numeric(14,3) NOT NULL CHECK (cantidad > 0),   -- en la presentación
  factor_conversion  numeric(12,3) NOT NULL DEFAULT 1 CHECK (factor_conversion > 0),
  cantidad_base      numeric(14,3) NOT NULL CHECK (cantidad_base > 0),
  estado_inventario  text          CHECK (estado_inventario IN ('DISPONIBLE','DANADA')),
  costo_unitario     bigint        CHECK (costo_unitario >= 0),     -- OC, DI: por presentación, sin IVA
  precio_unitario    bigint        CHECK (precio_unitario >= 0),    -- FV, NC: por presentación, sin IVA
  descuento_pct      numeric(5,2)  NOT NULL DEFAULT 0 CHECK (descuento_pct BETWEEN 0 AND 50), -- RN-09
  descuento_valor    bigint        NOT NULL DEFAULT 0 CHECK (descuento_valor >= 0),
  tarifa_iva         numeric(5,2)  NOT NULL DEFAULT 0 CHECK (tarifa_iva BETWEEN 0 AND 100), -- copia (RN-08)
  iva_valor          bigint        NOT NULL DEFAULT 0 CHECK (iva_valor >= 0),
  subtotal           bigint        NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
  total              bigint        NOT NULL DEFAULT 0 CHECK (total >= 0),
  costo_total        bigint        CHECK (costo_total >= 0),        -- costo de los lotes afectados
  fecha_vencimiento  date,                            -- DI, AP, AJ positivo
  fecha_fin_garantia date,                            -- FV (RN-18)
  destino_baja       text          CHECK (destino_baja IN ('RECICLAJE','DISPOSICION_FINAL')), -- RN-17
  motivo_id          bigint        REFERENCES motivo (id),
  UNIQUE (documento_id, numero_linea),
  CONSTRAINT ck_linea_total CHECK (total = subtotal - descuento_valor + iva_valor)
);

CREATE TABLE pago (                                   -- RN-07, RN-10
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  documento_id   bigint NOT NULL REFERENCES documento (id),  -- FV (pago) o NC (reintegro)
  medio          text   NOT NULL CHECK (medio IN ('EFECTIVO','DEBITO','CREDITO','TRANSFERENCIA')),
  valor          bigint NOT NULL CHECK (valor > 0),
  valor_recibido bigint,
  cambio         bigint CHECK (cambio >= 0),
  referencia     text,                                -- voucher o número de transferencia
  CONSTRAINT ck_pago_efectivo_50 CHECK (medio <> 'EFECTIVO' OR valor % 50 = 0),
  CONSTRAINT ck_pago_cambio CHECK (
       (medio = 'EFECTIVO' AND (valor_recibido IS NULL OR (valor_recibido >= valor AND cambio = valor_recibido - valor)))
    OR (medio <> 'EFECTIVO' AND valor_recibido IS NULL AND cambio IS NULL))
);

CREATE TABLE documento_envio (                        -- RF-DOC-001
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  documento_id bigint      NOT NULL REFERENCES documento (id),
  correo       text        NOT NULL,
  estado       text        NOT NULL CHECK (estado IN ('ENVIADO','FALLIDO')),
  detalle      text,
  usuario_id   bigint      NOT NULL REFERENCES usuario (id),
  enviado_en   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE importacion (                            -- RF-MIG-001, RN-29
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  archivo_nombre text        NOT NULL,
  fecha_corte    date        NOT NULL,
  estado         text        NOT NULL CHECK (estado IN ('VALIDADO','CARGADO','RECHAZADO')),
  filas_total    integer     NOT NULL CHECK (filas_total >= 0),
  filas_error    integer     NOT NULL DEFAULT 0 CHECK (filas_error >= 0),
  resumen        jsonb,
  documento_id   bigint      REFERENCES documento (id),  -- documento AP generado
  usuario_id     bigint      NOT NULL REFERENCES usuario (id),
  creado_en      timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_importacion_cargada CHECK (estado <> 'CARGADO' OR (filas_error = 0 AND documento_id IS NOT NULL))
);

-- =====================================================================
-- 7. INVENTARIO: LOTES, ESTADOS Y KARDEX
-- =====================================================================

CREATE TABLE lote (                                   -- RN-03, RN-13, RN-19
  id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  producto_id         bigint        NOT NULL REFERENCES producto (id),
  documento_linea_id  bigint        NOT NULL UNIQUE REFERENCES documento_linea (id), -- línea DI, AP o AJ+
  fecha_ingreso       timestamptz   NOT NULL,
  fecha_vencimiento   date,
  costo_unitario_base numeric(18,4) NOT NULL CHECK (costo_unitario_base >= 0),
  cantidad_inicial    numeric(14,3) NOT NULL CHECK (cantidad_inicial > 0),
  saldo_disponible    numeric(14,3) NOT NULL DEFAULT 0 CHECK (saldo_disponible >= 0),
  saldo_reservado     numeric(14,3) NOT NULL DEFAULT 0 CHECK (saldo_reservado  >= 0),
  saldo_danado        numeric(14,3) NOT NULL DEFAULT 0 CHECK (saldo_danado     >= 0),
  actualizado_en      timestamptz   NOT NULL DEFAULT now()
);

ALTER TABLE documento_linea
  ADD CONSTRAINT fk_linea_lote FOREIGN KEY (lote_id) REFERENCES lote (id);

CREATE TABLE reserva (                                -- RF-EST-001, RN-15
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cliente_id      bigint        NOT NULL REFERENCES tercero (id),
  presentacion_id bigint        NOT NULL REFERENCES presentacion (id),
  producto_id     bigint        NOT NULL REFERENCES producto (id),
  cantidad        numeric(14,3) NOT NULL CHECK (cantidad > 0),
  cantidad_base   numeric(14,3) NOT NULL CHECK (cantidad_base > 0),
  fecha_limite    date          NOT NULL,
  estado          text          NOT NULL DEFAULT 'ACTIVA'
                  CHECK (estado IN ('ACTIVA','CONSUMIDA','CANCELADA','VENCIDA')),
  factura_id      bigint        REFERENCES documento (id),
  creado_por      bigint        NOT NULL REFERENCES usuario (id),
  creado_en       timestamptz   NOT NULL DEFAULT now(),
  cerrado_en      timestamptz,
  CONSTRAINT ck_reserva_consumida CHECK (estado <> 'CONSUMIDA' OR factura_id IS NOT NULL),
  CONSTRAINT ck_reserva_plazo CHECK (fecha_limite <= (creado_en AT TIME ZONE 'America/Bogota')::date + 7)
);

CREATE TABLE reserva_lote (
  reserva_id bigint        NOT NULL REFERENCES reserva (id),
  lote_id    bigint        NOT NULL REFERENCES lote (id),
  cantidad   numeric(14,3) NOT NULL CHECK (cantidad > 0),
  PRIMARY KEY (reserva_id, lote_id)
);

CREATE TABLE cambio_estado (                          -- RF-EST-001/002, RN-14, RN-16
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  lote_id        bigint        NOT NULL REFERENCES lote (id),
  estado_origen  text          NOT NULL CHECK (estado_origen  IN ('DISPONIBLE','RESERVADA','DANADA')),
  estado_destino text          NOT NULL CHECK (estado_destino IN ('DISPONIBLE','RESERVADA','DANADA')),
  cantidad       numeric(14,3) NOT NULL CHECK (cantidad > 0),
  motivo_id      bigint        REFERENCES motivo (id),
  reserva_id     bigint        REFERENCES reserva (id),
  usuario_id     bigint        NOT NULL REFERENCES usuario (id),
  autorizado_por bigint        REFERENCES usuario (id),
  fecha          timestamptz   NOT NULL DEFAULT now(),
  periodo        date          NOT NULL REFERENCES periodo (periodo),
  observacion    text,
  CONSTRAINT ck_cambio_transicion CHECK ((estado_origen, estado_destino) IN (
    ('DISPONIBLE','RESERVADA'), ('RESERVADA','DISPONIBLE'), ('DISPONIBLE','DANADA'),
    ('RESERVADA','DANADA'),     ('DANADA','DISPONIBLE'))),
  CONSTRAINT ck_cambio_motivo_dano  CHECK (estado_destino <> 'DANADA' OR motivo_id IS NOT NULL),
  CONSTRAINT ck_cambio_autorizacion CHECK (
    NOT (estado_origen = 'DANADA' AND estado_destino = 'DISPONIBLE')
    OR (autorizado_por IS NOT NULL AND autorizado_por <> usuario_id)),
  CONSTRAINT ck_cambio_reserva CHECK (
    (estado_origen = 'RESERVADA' OR estado_destino = 'RESERVADA') = (reserva_id IS NOT NULL))
);

CREATE TABLE movimiento_inventario (                  -- kardex: una fila por lote afectado
  id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  fecha               timestamptz   NOT NULL DEFAULT now(),
  periodo             date          NOT NULL REFERENCES periodo (periodo),
  tipo                text          NOT NULL CHECK (tipo IN (
                        'ENTRADA_COMPRA','APERTURA','AJUSTE_POSITIVO','DEVOLUCION_CLIENTE','ANULACION_VENTA',
                        'SALIDA_VENTA','DEVOLUCION_PROVEEDOR','BAJA','AJUSTE_NEGATIVO','CAMBIO_ESTADO')),
  producto_id         bigint        NOT NULL REFERENCES producto (id),
  lote_id             bigint        NOT NULL REFERENCES lote (id),
  documento_id        bigint        REFERENCES documento (id),
  documento_linea_id  bigint        REFERENCES documento_linea (id),
  cambio_estado_id    bigint        REFERENCES cambio_estado (id),
  estado_origen       text          CHECK (estado_origen  IN ('DISPONIBLE','RESERVADA','DANADA')),
  estado_destino      text          CHECK (estado_destino IN ('DISPONIBLE','RESERVADA','DANADA')),
  cantidad            numeric(14,3) NOT NULL CHECK (cantidad > 0),   -- en unidad base
  costo_unitario_base numeric(18,4) NOT NULL CHECK (costo_unitario_base >= 0),
  costo_total         bigint        NOT NULL CHECK (costo_total >= 0),
  usuario_id          bigint        NOT NULL REFERENCES usuario (id),
  CONSTRAINT ck_mov_origen_unico CHECK ((documento_id IS NULL) <> (cambio_estado_id IS NULL)),       -- RN-02
  CONSTRAINT ck_mov_cambio       CHECK ((tipo = 'CAMBIO_ESTADO') = (cambio_estado_id IS NOT NULL)),
  CONSTRAINT ck_mov_estados      CHECK (estado_origen IS NOT NULL OR estado_destino IS NOT NULL),
  CONSTRAINT ck_mov_sentido      CHECK (
       (tipo IN ('ENTRADA_COMPRA','APERTURA','AJUSTE_POSITIVO','DEVOLUCION_CLIENTE','ANULACION_VENTA')
          AND estado_origen IS NULL AND estado_destino IS NOT NULL)
    OR (tipo IN ('SALIDA_VENTA','DEVOLUCION_PROVEEDOR','BAJA','AJUSTE_NEGATIVO')
          AND estado_origen IS NOT NULL AND estado_destino IS NULL)
    OR (tipo = 'CAMBIO_ESTADO' AND estado_origen IS NOT NULL AND estado_destino IS NOT NULL))
);

-- =====================================================================
-- 8. CONTEO FÍSICO Y SALDOS DE CIERRE
-- =====================================================================

CREATE TABLE conteo (                                 -- RF-CON-001, RN-24
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tipo         text        NOT NULL CHECK (tipo IN ('TOTAL','PARCIAL')),
  estado       text        NOT NULL DEFAULT 'EN_CURSO' CHECK (estado IN ('EN_CURSO','CERRADO','ANULADO')),
  alcance      jsonb,                                  -- categorías o productos del conteo parcial
  periodo      date        NOT NULL REFERENCES periodo (periodo),
  iniciado_por bigint      NOT NULL REFERENCES usuario (id),
  iniciado_en  timestamptz NOT NULL DEFAULT now(),
  cerrado_en   timestamptz,
  observacion  text,
  CONSTRAINT ck_conteo_alcance CHECK (tipo = 'TOTAL' OR alcance IS NOT NULL)
);

CREATE TABLE conteo_linea (
  id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conteo_id            bigint        NOT NULL REFERENCES conteo (id),
  producto_id          bigint        NOT NULL REFERENCES producto (id),
  lote_id              bigint        REFERENCES lote (id),  -- solo si el producto controla vencimiento
  contado_buen_estado  numeric(14,3) CHECK (contado_buen_estado >= 0),
  contado_danado       numeric(14,3) CHECK (contado_danado >= 0),
  registrado_buen_estado numeric(14,3) CHECK (registrado_buen_estado >= 0), -- disponible + reservada al capturar
  registrado_danado    numeric(14,3) CHECK (registrado_danado >= 0),
  capturado_por        bigint        REFERENCES usuario (id),
  capturado_en         timestamptz,
  ajuste_linea_id      bigint        REFERENCES documento_linea (id),
  UNIQUE NULLS NOT DISTINCT (conteo_id, producto_id, lote_id)
);

ALTER TABLE periodo   ADD CONSTRAINT fk_periodo_conteo   FOREIGN KEY (conteo_id) REFERENCES conteo (id);
ALTER TABLE documento ADD CONSTRAINT fk_documento_conteo FOREIGN KEY (conteo_id) REFERENCES conteo (id);

CREATE TABLE cierre_saldo (                           -- RF-CIE-001
  periodo             date          NOT NULL REFERENCES periodo (periodo),
  lote_id             bigint        NOT NULL REFERENCES lote (id),
  saldo_disponible    numeric(14,3) NOT NULL CHECK (saldo_disponible >= 0),
  saldo_reservado     numeric(14,3) NOT NULL CHECK (saldo_reservado  >= 0),
  saldo_danado        numeric(14,3) NOT NULL CHECK (saldo_danado     >= 0),
  costo_unitario_base numeric(18,4) NOT NULL,
  valor               bigint        NOT NULL CHECK (valor >= 0),
  PRIMARY KEY (periodo, lote_id)
);

-- =====================================================================
-- 9. AUDITORÍA (particionada por mes, retención 24 meses)   RN-28
-- =====================================================================

CREATE TABLE auditoria (
  id              bigserial,                          -- IDENTITY en tablas particionadas exige PostgreSQL 17
  fecha           timestamptz NOT NULL DEFAULT now(),
  usuario_id      bigint      REFERENCES usuario (id),
  ip              inet,
  accion          text        NOT NULL CHECK (accion IN (
                    'CREAR','MODIFICAR','ELIMINAR','ANULAR','APROBAR','CAMBIAR_ESTADO',
                    'INICIAR_SESION','SESION_FALLIDA','DEPURAR')),
  entidad         text        NOT NULL,
  entidad_id      bigint,
  valores_antes   jsonb,
  valores_despues jsonb,
  PRIMARY KEY (id, fecha)
) PARTITION BY RANGE (fecha);

CREATE TABLE auditoria_default PARTITION OF auditoria DEFAULT;

CREATE OR REPLACE FUNCTION fn_crear_particion_auditoria(p_mes date)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  v_ini date := date_trunc('month', p_mes)::date;
BEGIN
  EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF auditoria FOR VALUES FROM (%L) TO (%L)',
                 'auditoria_' || to_char(v_ini, 'YYYY_MM'), v_ini, (v_ini + interval '1 month')::date);
END $$;

DO $$
DECLARE m date := date '2026-01-01';
BEGIN
  WHILE m < date '2028-01-01' LOOP
    PERFORM fn_crear_particion_auditoria(m);
    m := (m + interval '1 month')::date;
  END LOOP;
END $$;

-- Depuración mensual: elimina particiones completas con más de 24 meses y deja constancia
CREATE OR REPLACE FUNCTION fn_depurar_auditoria()
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE
  v_limite date := (date_trunc('month', now()) - interval '24 months')::date;
  v_part   record;
  v_borradas integer := 0;
BEGIN
  FOR v_part IN
    SELECT c.relname
    FROM pg_inherits i
    JOIN pg_class c ON c.oid = i.inhrelid
    JOIN pg_class p ON p.oid = i.inhparent
    WHERE p.relname = 'auditoria'
      AND c.relname ~ '^auditoria_[0-9]{4}_[0-9]{2}$'
      AND to_date(substr(c.relname, 11), 'YYYY_MM') < v_limite
  LOOP
    EXECUTE format('DROP TABLE %I', v_part.relname);
    v_borradas := v_borradas + 1;
  END LOOP;
  INSERT INTO auditoria (usuario_id, accion, entidad, valores_despues)
  VALUES (NULLIF(current_setting('app.usuario_id', true), '')::bigint, 'DEPURAR', 'auditoria',
          jsonb_build_object('limite', v_limite, 'particiones_eliminadas', v_borradas));
  RETURN v_borradas;
END $$;

-- =====================================================================
-- 10. FUNCIONES DE NEGOCIO Y TRIGGERS
-- =====================================================================

-- Consecutivo sin saltos: bloquea la fila hasta el fin de la transacción  RN-11
CREATE OR REPLACE FUNCTION fn_siguiente_numero(p_tipo char(2))
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE v_num bigint;
BEGIN
  UPDATE consecutivo
     SET siguiente_numero = siguiente_numero + 1
   WHERE tipo_documento = p_tipo
  RETURNING siguiente_numero - 1 INTO v_num;
  IF v_num IS NULL THEN
    RAISE EXCEPTION 'No existe consecutivo para el tipo %', p_tipo;
  END IF;
  RETURN v_num;
END $$;

-- Asigna el período a partir de la fecha y rechaza períodos cerrados  RN-25
CREATE OR REPLACE FUNCTION fn_validar_periodo()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_estado text;
BEGIN
  NEW.periodo := date_trunc('month', NEW.fecha AT TIME ZONE 'America/Bogota')::date;
  INSERT INTO periodo (periodo) VALUES (NEW.periodo) ON CONFLICT (periodo) DO NOTHING;
  SELECT estado INTO v_estado FROM periodo WHERE periodo = NEW.periodo;
  IF v_estado = 'CERRADO' THEN
    RAISE EXCEPTION 'El período % está cerrado; no admite operaciones (RN-25)', to_char(NEW.periodo, 'YYYY-MM');
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER trg_documento_periodo  BEFORE INSERT OR UPDATE OF fecha ON documento
  FOR EACH ROW EXECUTE FUNCTION fn_validar_periodo();
CREATE TRIGGER trg_movimiento_periodo BEFORE INSERT ON movimiento_inventario
  FOR EACH ROW EXECUTE FUNCTION fn_validar_periodo();
CREATE TRIGGER trg_cambio_periodo     BEFORE INSERT ON cambio_estado
  FOR EACH ROW EXECUTE FUNCTION fn_validar_periodo();

-- Nota: una factura solo se anula el mismo día (RN-12), así que su período siempre está abierto.

-- Inmutabilidad de documentos confirmados  RN-12
CREATE OR REPLACE FUNCTION fn_documento_inmutable()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    IF OLD.estado <> 'BORRADOR' THEN
      RAISE EXCEPTION 'Solo se eliminan documentos en BORRADOR (RN-12)';
    END IF;
    RETURN OLD;
  END IF;

  IF OLD.estado IN ('BORRADOR','PROPUESTO') THEN
    RETURN NEW;
  END IF;

  IF OLD.tipo = 'OC' AND OLD.estado IN ('EMITIDA','RECIBIDA_PARCIAL')
     AND NEW.estado IN ('RECIBIDA_PARCIAL','RECIBIDA','CANCELADA')
     AND (to_jsonb(OLD) - 'estado') = (to_jsonb(NEW) - 'estado') THEN
    RETURN NEW;
  END IF;

  IF OLD.tipo = 'FV' AND OLD.estado = 'CONFIRMADO' AND NEW.estado = 'ANULADO'
     AND (to_jsonb(OLD) - ARRAY['estado','anulado_por','anulado_en','motivo_anulacion'])
       = (to_jsonb(NEW) - ARRAY['estado','anulado_por','anulado_en','motivo_anulacion']) THEN
    IF (OLD.fecha AT TIME ZONE 'America/Bogota')::date <> (now() AT TIME ZONE 'America/Bogota')::date THEN
      RAISE EXCEPTION 'Una factura solo se anula el mismo día de su emisión (RN-12)';
    END IF;
    IF EXISTS (SELECT 1 FROM documento d WHERE d.tipo = 'NC' AND d.documento_origen_id = OLD.id) THEN
      RAISE EXCEPTION 'La factura tiene devoluciones; use una nota crédito (RN-12)';
    END IF;
    RETURN NEW;
  END IF;

  RAISE EXCEPTION 'El documento %-% en estado % no se puede modificar (RN-12)', OLD.tipo, OLD.numero, OLD.estado;
END $$;

CREATE TRIGGER trg_documento_inmutable BEFORE UPDATE OR DELETE ON documento
  FOR EACH ROW EXECUTE FUNCTION fn_documento_inmutable();

-- Las líneas solo cambian mientras su documento está en BORRADOR o PROPUESTO
CREATE OR REPLACE FUNCTION fn_linea_inmutable()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_estado text;
BEGIN
  SELECT estado INTO v_estado FROM documento WHERE id = OLD.documento_id;
  IF v_estado NOT IN ('BORRADOR','PROPUESTO') THEN
    RAISE EXCEPTION 'Las líneas de un documento en estado % no se modifican (RN-12)', v_estado;
  END IF;
  RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER trg_linea_inmutable BEFORE UPDATE OR DELETE ON documento_linea
  FOR EACH ROW EXECUTE FUNCTION fn_linea_inmutable();

-- Tablas de solo inserción (kardex, cambios de estado, auditoría, saldos de cierre)
CREATE OR REPLACE FUNCTION fn_solo_insercion()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'La tabla % es de solo inserción (RN-02, RN-28)', TG_TABLE_NAME;
END $$;

CREATE TRIGGER trg_movimiento_solo_insercion BEFORE UPDATE OR DELETE ON movimiento_inventario
  FOR EACH ROW EXECUTE FUNCTION fn_solo_insercion();
CREATE TRIGGER trg_cambio_solo_insercion     BEFORE UPDATE OR DELETE ON cambio_estado
  FOR EACH ROW EXECUTE FUNCTION fn_solo_insercion();
CREATE TRIGGER trg_auditoria_solo_insercion  BEFORE UPDATE OR DELETE ON auditoria
  FOR EACH ROW EXECUTE FUNCTION fn_solo_insercion();
CREATE TRIGGER trg_lote_sin_borrado          BEFORE DELETE ON lote
  FOR EACH ROW EXECUTE FUNCTION fn_solo_insercion();

-- Reglas del cierre y la reapertura  RN-24, RN-25
CREATE OR REPLACE FUNCTION fn_validar_cierre()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.estado = 'CERRADO' AND OLD.estado <> 'CERRADO' THEN
    IF EXISTS (SELECT 1 FROM periodo p WHERE p.periodo < NEW.periodo AND p.estado <> 'CERRADO') THEN
      RAISE EXCEPTION 'Hay meses anteriores sin cerrar (RN-25)';
    END IF;
    IF NEW.conteo_id IS NULL OR NOT EXISTS (
         SELECT 1 FROM conteo c
          WHERE c.id = NEW.conteo_id AND c.tipo = 'TOTAL'
            AND c.estado = 'CERRADO' AND c.periodo = NEW.periodo) THEN
      RAISE EXCEPTION 'El cierre exige un conteo físico total cerrado del mismo mes (RN-24)';
    END IF;
    IF EXISTS (SELECT 1 FROM documento d
                WHERE d.tipo = 'AJ' AND d.estado = 'PROPUESTO' AND d.conteo_id = NEW.conteo_id) THEN
      RAISE EXCEPTION 'El conteo tiene ajustes sin resolver (RN-24)';
    END IF;
  END IF;

  IF NEW.estado = 'REABIERTO' AND OLD.estado = 'CERRADO' THEN
    IF EXISTS (SELECT 1 FROM periodo p WHERE p.periodo > NEW.periodo AND p.estado = 'CERRADO') THEN
      RAISE EXCEPTION 'Solo se reabre el último mes cerrado (RN-25)';
    END IF;
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER trg_periodo_cierre BEFORE UPDATE OF estado ON periodo
  FOR EACH ROW EXECUTE FUNCTION fn_validar_cierre();

-- Auditoría genérica  RN-28
-- La aplicación fija en cada transacción: SET LOCAL app.usuario_id = '<id>'; SET LOCAL app.ip = '<ip>';
CREATE OR REPLACE FUNCTION fn_auditar()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  v_antes   jsonb;
  v_despues jsonb;
  v_accion  text;
BEGIN
  IF TG_OP IN ('UPDATE','DELETE') THEN v_antes   := to_jsonb(OLD) - 'password_hash'; END IF;
  IF TG_OP IN ('INSERT','UPDATE') THEN v_despues := to_jsonb(NEW) - 'password_hash'; END IF;
  IF TG_OP = 'UPDATE' AND v_antes = v_despues THEN
    RETURN NULL;
  END IF;

  v_accion := CASE TG_OP WHEN 'INSERT' THEN 'CREAR' WHEN 'DELETE' THEN 'ELIMINAR' ELSE 'MODIFICAR' END;
  IF TG_OP = 'UPDATE' AND (v_antes ->> 'estado') IS DISTINCT FROM (v_despues ->> 'estado') THEN
    v_accion := CASE v_despues ->> 'estado'
                  WHEN 'ANULADO'  THEN 'ANULAR'
                  WHEN 'APROBADO' THEN 'APROBAR'
                  ELSE 'CAMBIAR_ESTADO' END;
  END IF;

  INSERT INTO auditoria (usuario_id, ip, accion, entidad, entidad_id, valores_antes, valores_despues)
  VALUES (NULLIF(current_setting('app.usuario_id', true), '')::bigint,
          NULLIF(current_setting('app.ip', true), '')::inet,
          v_accion, TG_TABLE_NAME,
          COALESCE(v_despues ->> 'id', v_antes ->> 'id')::bigint,
          v_antes, v_despues);
  RETURN NULL;
END $$;

DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'usuario','usuario_rol','rol_permiso','api_credencial','empresa','unidad_medida','tarifa_iva',
    'marca','categoria','motivo','producto','presentacion','precio_presentacion','tercero',
    'documento','cambio_estado','reserva','conteo','periodo','periodo_evento','importacion']
  LOOP
    EXECUTE format('CREATE TRIGGER trg_auditar_%1$s AFTER INSERT OR UPDATE OR DELETE ON %1$I
                    FOR EACH ROW EXECUTE FUNCTION fn_auditar()', t);
  END LOOP;
END $$;

-- =====================================================================
-- 11. ÍNDICES  (RNF-REN-001/002)
-- =====================================================================

CREATE INDEX ix_producto_nombre_trgm  ON producto USING gin (lower(nombre) gin_trgm_ops);
CREATE INDEX ix_producto_categoria    ON producto (categoria_id);
CREATE INDEX ix_presentacion_producto ON presentacion (producto_id);
CREATE INDEX ix_precio_historial      ON precio_presentacion (presentacion_id, vigente_desde DESC);
CREATE INDEX ix_tercero_nombre_trgm   ON tercero USING gin (lower(coalesce(razon_social, nombre || ' ' || apellido)) gin_trgm_ops);

CREATE INDEX ix_documento_tipo_fecha  ON documento (tipo, fecha DESC);
CREATE INDEX ix_documento_tercero     ON documento (tercero_id, fecha DESC);
CREATE INDEX ix_documento_origen      ON documento (documento_origen_id);
CREATE INDEX ix_documento_periodo     ON documento (periodo);
CREATE INDEX ix_linea_documento       ON documento_linea (documento_id);
CREATE INDEX ix_linea_origen          ON documento_linea (linea_origen_id);
CREATE INDEX ix_linea_producto        ON documento_linea (producto_id);
CREATE INDEX ix_pago_documento        ON pago (documento_id);

-- Orden de consumo PEPS/FEFO (RN-19): solo lotes con saldo disponible
CREATE INDEX ix_lote_consumo     ON lote (producto_id, fecha_vencimiento NULLS LAST, fecha_ingreso, id)
  WHERE saldo_disponible > 0;
CREATE INDEX ix_lote_producto    ON lote (producto_id);
CREATE INDEX ix_lote_vencimiento ON lote (fecha_vencimiento)
  WHERE fecha_vencimiento IS NOT NULL AND (saldo_disponible > 0 OR saldo_reservado > 0);

CREATE INDEX ix_mov_producto_fecha ON movimiento_inventario (producto_id, fecha, id);
CREATE INDEX ix_mov_lote           ON movimiento_inventario (lote_id);
CREATE INDEX ix_mov_linea          ON movimiento_inventario (documento_linea_id);
CREATE INDEX ix_mov_documento      ON movimiento_inventario (documento_id);
CREATE INDEX ix_mov_periodo        ON movimiento_inventario (periodo);

CREATE INDEX ix_cambio_lote        ON cambio_estado (lote_id);
CREATE INDEX ix_reserva_activa     ON reserva (fecha_limite) WHERE estado = 'ACTIVA';
CREATE INDEX ix_reserva_cliente    ON reserva (cliente_id, estado);
CREATE INDEX ix_conteo_linea       ON conteo_linea (conteo_id);
CREATE INDEX ix_auditoria_entidad  ON auditoria (entidad, entidad_id);
CREATE INDEX ix_auditoria_usuario  ON auditoria (usuario_id, fecha);

-- =====================================================================
-- 12. VISTAS
-- =====================================================================

-- Existencia por producto y estado, valorizada al costo de cada lote (RF-PRO-003, RF-REP-001)
CREATE VIEW v_existencia_producto AS
SELECT p.id AS producto_id,
       p.nombre,
       p.unidad_base,
       COALESCE(sum(l.saldo_disponible), 0) AS disponible,
       COALESCE(sum(l.saldo_reservado), 0)  AS reservada,
       COALESCE(sum(l.saldo_danado), 0)     AS danada,
       COALESCE(sum(l.saldo_disponible + l.saldo_reservado + l.saldo_danado), 0) AS total,
       COALESCE(round(sum((l.saldo_disponible + l.saldo_reservado + l.saldo_danado)
                          * l.costo_unitario_base)), 0)::bigint AS valor
FROM producto p
LEFT JOIN lote l ON l.producto_id = p.id
GROUP BY p.id, p.nombre, p.unidad_base;

-- Lotes vendibles en orden de consumo FEFO/PEPS (RN-04, RN-19)
CREATE VIEW v_lote_vendible AS
SELECT l.*,
       row_number() OVER (PARTITION BY l.producto_id
                          ORDER BY l.fecha_vencimiento NULLS LAST, l.fecha_ingreso, l.id) AS orden_consumo
FROM lote l
WHERE l.saldo_disponible > 0
  AND (l.fecha_vencimiento IS NULL
       OR l.fecha_vencimiento >= (now() AT TIME ZONE 'America/Bogota')::date);

-- Precio vigente con IVA, redondeo al peso con mitades hacia arriba (RN-06, RN-08)
CREATE VIEW v_precio_vigente AS
SELECT pr.id AS presentacion_id,
       pr.codigo_barras,
       pr.nombre AS presentacion,
       p.id      AS producto_id,
       p.nombre  AS producto,
       pp.precio_base,
       pp.origen,
       t.porcentaje AS tarifa_iva,
       round(pp.precio_base * (1 + t.porcentaje / 100))::bigint AS precio_con_iva
FROM presentacion pr
JOIN producto p    ON p.id = pr.producto_id
JOIN tarifa_iva t  ON t.id = p.tarifa_iva_id
LEFT JOIN precio_presentacion pp ON pp.presentacion_id = pr.id AND pp.vigente_hasta IS NULL;

-- Kardex con saldo acumulado por producto (RF-MOV-001)
CREATE VIEW v_kardex AS
SELECT m.id,
       m.fecha,
       m.producto_id,
       m.lote_id,
       m.tipo,
       CASE WHEN d.id IS NOT NULL THEN d.tipo || '-' || d.numero END AS documento,
       m.estado_origen,
       m.estado_destino,
       CASE WHEN m.estado_origen  IS NULL THEN  m.cantidad
            WHEN m.estado_destino IS NULL THEN -m.cantidad
            ELSE 0 END AS cantidad_neta,
       sum(CASE WHEN m.estado_origen  IS NULL THEN  m.cantidad
                WHEN m.estado_destino IS NULL THEN -m.cantidad
                ELSE 0 END)
         OVER (PARTITION BY m.producto_id ORDER BY m.fecha, m.id) AS saldo,
       m.costo_unitario_base,
       m.costo_total,
       m.usuario_id
FROM movimiento_inventario m
LEFT JOIN documento d ON d.id = m.documento_id;

-- =====================================================================
-- 13. DATOS INICIALES
-- =====================================================================

INSERT INTO unidad_medida (codigo, nombre, es_discreta) VALUES
  ('UND','Unidad',true), ('PAR','Par',true), ('JGO','Juego',true),
  ('KG','Kilogramo',false), ('G','Gramo',false), ('LB','Libra',false),
  ('LTR','Litro',false), ('ML','Mililitro',false), ('GAL','Galón',false),
  ('MTR','Metro',false), ('CM','Centímetro',false), ('M2','Metro cuadrado',false),
  ('M3','Metro cúbico',false);

INSERT INTO tarifa_iva (nombre, porcentaje) VALUES
  ('Excluido / exento', 0), ('Tarifa reducida', 5), ('Tarifa general', 19);

INSERT INTO consecutivo (tipo_documento, prefijo) VALUES
  ('OC','OC'), ('DI','DI'), ('FV','FV'), ('NC','NC'),
  ('DP','DP'), ('AB','AB'), ('AJ','AJ'), ('AP','AP');

INSERT INTO motivo (tipo, nombre) VALUES
  ('DANO','Rotura'), ('DANO','Defecto de fábrica'), ('DANO','Humedad'), ('DANO','Vencido'), ('DANO','Otro'),
  ('AJUSTE','Diferencia de conteo'), ('AJUSTE','Error de registro'), ('AJUSTE','Pérdida'), ('AJUSTE','Hallazgo'),
  ('BAJA','Producto inservible'), ('BAJA','Vencido sin devolución'),
  ('DEV_CLIENTE','Cambio de opinión'), ('DEV_CLIENTE','Defecto'), ('DEV_CLIENTE','Garantía'), ('DEV_CLIENTE','Error de despacho'),
  ('DEV_PROVEEDOR','Defecto'), ('DEV_PROVEEDOR','Error de pedido'), ('DEV_PROVEEDOR','Vencimiento próximo'),
  ('ANULACION','Error de digitación'), ('ANULACION','Cliente desiste');

INSERT INTO rol (codigo, nombre) VALUES
  ('ADMIN','Administrador'), ('GERENTE','Gerente'), ('JEFE_INVENTARIO','Jefe de inventario'),
  ('COMPRADOR','Comprador'), ('VENDEDOR','Vendedor'), ('AUDITOR','Auditor');

INSERT INTO permiso (codigo, descripcion) VALUES
  ('USUARIOS_GESTIONAR','Usuarios y roles'),
  ('CONFIG_GESTIONAR','Configuración de empresa, impuestos y catálogos'),
  ('CATALOGO_EDITAR','Crear y editar productos y presentaciones'),
  ('CATALOGO_CONSULTAR','Consultar productos y existencias'),
  ('COSTOS_VER','Ver costos y utilidad'),
  ('PRECIO_MANUAL','Fijar precio manual'),
  ('TERCERO_CLIENTE_EDITAR','Registrar clientes'),
  ('TERCERO_PROVEEDOR_EDITAR','Registrar proveedores'),
  ('ORDEN_COMPRA_GESTIONAR','Órdenes de compra'),
  ('RECEPCION_REGISTRAR','Recepciones de mercancía'),
  ('DEV_PROVEEDOR_REGISTRAR','Devoluciones a proveedor'),
  ('VENTA_REGISTRAR','Registrar ventas'),
  ('RESERVA_GESTIONAR','Crear y cancelar reservas'),
  ('DESCUENTO_MAYOR','Descuento mayor a 10 %'),
  ('FACTURA_ANULAR','Anular factura del día'),
  ('DEV_CLIENTE_REGISTRAR','Devoluciones de clientes'),
  ('DANADO_MARCAR','Marcar existencias dañadas'),
  ('DANADO_RETORNAR','Retornar dañado a disponible'),
  ('BAJA_AJUSTE_PROPONER','Proponer bajas y ajustes'),
  ('CONTEO_REGISTRAR','Registrar conteos físicos'),
  ('BAJA_AJUSTE_APROBAR','Aprobar bajas y ajustes'),
  ('PERIODO_CERRAR','Cerrar mes'),
  ('PERIODO_REABRIR','Reabrir mes'),
  ('REPORTES_VER','Reportes e historial de movimientos'),
  ('AUDITORIA_VER','Consultar auditoría'),
  ('MIGRACION_EJECUTAR','Migración desde Excel');

-- Matriz de permisos (Especificación v4, sección 2.2)
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r CROSS JOIN permiso p WHERE r.codigo = 'ADMIN';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo NOT IN
  ('USUARIOS_GESTIONAR','CONFIG_GESTIONAR','PERIODO_REABRIR','MIGRACION_EJECUTAR')
WHERE r.codigo = 'GERENTE';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN
  ('CATALOGO_EDITAR','CATALOGO_CONSULTAR','COSTOS_VER','RECEPCION_REGISTRAR',
   'DANADO_MARCAR','BAJA_AJUSTE_PROPONER','CONTEO_REGISTRAR','REPORTES_VER')
WHERE r.codigo = 'JEFE_INVENTARIO';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN
  ('CATALOGO_CONSULTAR','COSTOS_VER','TERCERO_PROVEEDOR_EDITAR','ORDEN_COMPRA_GESTIONAR',
   'RECEPCION_REGISTRAR','DEV_PROVEEDOR_REGISTRAR','REPORTES_VER')
WHERE r.codigo = 'COMPRADOR';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN
  ('CATALOGO_CONSULTAR','TERCERO_CLIENTE_EDITAR','VENTA_REGISTRAR','RESERVA_GESTIONAR',
   'DEV_CLIENTE_REGISTRAR','REPORTES_VER')
WHERE r.codigo = 'VENDEDOR';

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN
  ('CATALOGO_CONSULTAR','COSTOS_VER','REPORTES_VER','AUDITORIA_VER')
WHERE r.codigo = 'AUDITOR';

-- Usuario de tareas programadas (sin inicio de sesión: hash inválido)
INSERT INTO usuario (usuario, nombre_completo, correo, password_hash, es_sistema)
VALUES ('sistema', 'Sistema (tareas programadas)', 'sistema@localhost', '!', true);

-- Consumidor final (RN-26)
INSERT INTO tercero (tipo_persona, tipo_identificacion, numero_identificacion, nombre, apellido,
                     direccion, telefono, es_cliente, es_consumidor_final)
VALUES ('NATURAL', 'CC', '222222222222', 'Consumidor', 'final', 'No aplica', '0000000', true, true);

COMMIT;

-- El primer administrador y los datos de la empresa los crea el asistente de instalación
-- de la aplicación, porque requieren una contraseña con hash y el NIT real.
