/**
 * Nombre: productos.js
 * Descripción: Prueba de carga del catálogo con k6 (RNF-REN-001 y RNF-REN-003): inicia sesión en setup y
 *              mezcla búsqueda por nombre (70 %), consulta por id (20 %) y creación de productos (10 %).
 *              Modo "carga": 50 usuarios virtuales durante 5 minutos; modo "humo" (CI): 1 usuario durante
 *              30 segundos. Falla si p95 de http_req_duration >= 1000 ms o http_req_failed >= 1 %, y
 *              exporta el resumen en JSON para el reporte de criterios (scripts/criterios.mjs).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Criterios que verifica (docs/criterios-aceptacion.yaml): [CA-RNF-REN-001-1] con el umbral de p95 y
 * [CA-RNF-REN-003-1] con el de fallas, este último solo en modo carga (50 usuarios).
 *
 * Uso (desde la raíz del repositorio; ver load-tests/README.md):
 *   k6 run load-tests/productos.js                      # modo carga (50 VU, 5 min)
 *   k6 run -e K6_MODO=humo load-tests/productos.js      # modo humo (1 VU, 30 s)
 *
 * Variables de entorno (también se leen del .env de la raíz; las del sistema tienen prioridad):
 *   K6_MODO        carga (por defecto) o humo.
 *   K6_BASE_URL    URL del frontend o del backend; por defecto http://127.0.0.1:${E2E_PUERTO:-8088}.
 *   K6_USUARIO     usuario con permiso CATALOGO_EDITAR; por defecto jefe.
 *   K6_CONTRASENA  su contraseña; por defecto E2E_JEFE_PASSWORD.
 *   K6_RESUMEN     archivo JSON del resumen; por defecto load-tests/results/k6-resumen-<modo>.json.
 */
import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

/**
 * Lee el .env de la raíz (KEY=VALUE por línea). Si no existe, devuelve un objeto vacío.
 *
 * @returns {Record<string, string>} variables del archivo
 */
function leerEnv() {
  try {
    return Object.fromEntries(
      open('../.env')
        .split(/\r?\n/)
        .map((linea) => linea.trim())
        .filter((linea) => linea && !linea.startsWith('#') && linea.includes('='))
        .map((linea) => {
          const i = linea.indexOf('=');
          return [linea.slice(0, i).trim(), linea.slice(i + 1).trim().replace(/^['"]|['"]$/g, '')];
        }),
    );
  } catch {
    return {};
  }
}

const ARCHIVO_ENV = leerEnv();

/**
 * Variable de entorno del sistema o, si no está, del .env.
 *
 * @param {string} nombre nombre de la variable
 * @param {string} [porDefecto] valor si no está en ninguno
 * @returns {string|undefined} valor
 */
function variable(nombre, porDefecto) {
  return __ENV[nombre] || ARCHIVO_ENV[nombre] || porDefecto;
}

/** Modo de ejecución: "carga" (volumen de diseño) o "humo" (verificación rápida en CI). */
const MODO = variable('K6_MODO', 'carga');
if (!['carga', 'humo'].includes(MODO)) {
  throw new Error(`K6_MODO debe ser "carga" o "humo", no "${MODO}"`);
}

const BASE_URL = variable(
  'K6_BASE_URL',
  `http://127.0.0.1:${variable('E2E_PUERTO', '8088')}`,
).replace(/\/$/, '');
const USUARIO = variable('K6_USUARIO', 'jefe');
const CONTRASENA = variable('K6_CONTRASENA', variable('E2E_JEFE_PASSWORD'));
const ARCHIVO_RESUMEN = variable('K6_RESUMEN', `load-tests/results/k6-resumen-${MODO}.json`);

/** Escenarios por modo: RNF-REN-003 exige 50 usuarios concurrentes; el humo solo comprueba que todo responde. */
const ESCENARIOS = {
  carga: { executor: 'constant-vus', vus: 50, duration: '5m', gracefulStop: '30s' },
  humo: { executor: 'constant-vus', vus: 1, duration: '30s', gracefulStop: '10s' },
};

/** Textos de búsqueda: tipos, materiales y medidas presentes en datos-volumen.sql y en el escenario de prueba. */
const BUSQUEDAS = [
  'tornillo', 'tubo', 'cable', 'martillo', 'pintura', 'cemento', 'llave', 'bombillo', 'galvanizado',
  'pvc', 'acero', 'brocha', 'manguera', 'interruptor', 'serie 0001', '1/2', 'inoxidable', 'silicona',
];

/** Duración por operación (ms), para ver en el resumen cuál pesa más. */
const duracionBuscar = new Trend('duracion_buscar', true);
const duracionConsultar = new Trend('duracion_consultar', true);
const duracionCrear = new Trend('duracion_crear', true);
/** Productos creados durante la prueba. */
const productosCreados = new Counter('productos_creados');

export const options = {
  scenarios: { [MODO]: ESCENARIOS[MODO] },
  // Umbrales que hacen fallar la prueba (código de salida 99): RNF-REN-001 y RNF-REN-003
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  tags: { modo: MODO },
};

/**
 * Elige un elemento al azar.
 *
 * @template T
 * @param {T[]} lista elementos
 * @returns {T} elemento elegido
 */
function alAzar(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}

/**
 * Cabeceras con el token de la sesión.
 *
 * @param {string} token JWT de acceso
 * @param {string} operacion etiqueta de la operación para las métricas
 * @returns {object} parámetros de http
 */
function conToken(token, operacion) {
  return {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    tags: { operacion },
  };
}

/**
 * Inicia sesión una vez y prepara los datos que comparten los usuarios virtuales: token, marcas,
 * categorías, ids de productos para consultar y total de productos de la base.
 *
 * @returns {{token: string, marcas: number[], categorias: number[], ids: number[], totalProductos: number}}
 *   datos de la prueba
 */
export function setup() {
  if (!CONTRASENA) {
    fail('Falta la contraseña: defina K6_CONTRASENA o E2E_JEFE_PASSWORD (por ejemplo en el .env de la raíz).');
  }
  const login = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ usuario: USUARIO, contrasena: CONTRASENA }),
    { headers: { 'Content-Type': 'application/json' }, tags: { operacion: 'login' } },
  );
  if (login.status !== 200) {
    fail(`El inicio de sesión de ${USUARIO} en ${BASE_URL} respondió ${login.status}: ${login.body}`);
  }
  const token = login.json('accessToken');

  const marcas = http.get(`${BASE_URL}/api/v1/marcas`, conToken(token, 'preparar')).json().map((m) => m.id);
  const categorias = http
    .get(`${BASE_URL}/api/v1/categorias`, conToken(token, 'preparar'))
    .json()
    .map((c) => c.id);

  const ids = new Set();
  for (const q of BUSQUEDAS.slice(0, 8)) {
    const pagina = http
      .get(`${BASE_URL}/api/v1/productos?q=${encodeURIComponent(q)}&size=100`, conToken(token, 'preparar'))
      .json();
    pagina.contenido.forEach((p) => ids.add(p.id));
  }
  const totalProductos = http
    .get(`${BASE_URL}/api/v1/productos?size=1`, conToken(token, 'preparar'))
    .json('totalElementos');

  if (marcas.length === 0 || categorias.length === 0 || ids.size === 0) {
    fail('La base no tiene marcas, categorías o productos: cargue los datos de prueba o de volumen.');
  }
  console.log(`Modo ${MODO} contra ${BASE_URL}: ${totalProductos} productos, ${ids.size} ids para consultar.`);
  return { token, marcas, categorias, ids: [...ids], totalProductos };
}

/**
 * Iteración de un usuario virtual: 70 % búsqueda por nombre, 20 % consulta por id y 10 % creación,
 * con una pausa de 0,5 a 1,5 s entre operaciones (tiempo de lectura del usuario).
 *
 * @param {ReturnType<typeof setup>} datos datos preparados en setup
 */
export default function (datos) {
  const azar = Math.random();

  if (azar < 0.7) {
    const q = encodeURIComponent(alAzar(BUSQUEDAS));
    const pagina = Math.floor(Math.random() * 3);
    const res = http.get(
      `${BASE_URL}/api/v1/productos?q=${q}&page=${pagina}&size=20`,
      conToken(datos.token, 'buscar'),
    );
    duracionBuscar.add(res.timings.duration);
    check(res, {
      'buscar: 200': (r) => r.status === 200,
      'buscar: trae una página': (r) => r.status === 200 && Array.isArray(r.json('contenido')),
    });
  } else if (azar < 0.9) {
    const res = http.get(
      `${BASE_URL}/api/v1/productos/${alAzar(datos.ids)}`,
      conToken(datos.token, 'consultar'),
    );
    duracionConsultar.add(res.timings.duration);
    check(res, { 'consultar: 200': (r) => r.status === 200 });
  } else {
    const producto = {
      nombre: `Carga k6 ${__VU}-${__ITER}-${Date.now()}`,
      marcaId: alAzar(datos.marcas),
      categoriaId: alAzar(datos.categorias),
      unidadBase: 'UND',
      tarifaIvaId: 3,
      porcentajeIncremento: 30,
      control: 'NINGUNO',
      descripcion: 'Creado por la prueba de carga k6.',
    };
    const res = http.post(
      `${BASE_URL}/api/v1/productos`,
      JSON.stringify(producto),
      conToken(datos.token, 'crear'),
    );
    duracionCrear.add(res.timings.duration);
    if (check(res, { 'crear: 201': (r) => r.status === 201 })) {
      productosCreados.add(1);
    }
  }

  sleep(0.5 + Math.random());
}

/**
 * Valor de una estadística de una métrica del resumen, o null si no existe.
 *
 * @param {object} metricas métricas del resumen de k6
 * @param {string} nombre nombre de la métrica
 * @param {string} estadistica estadística (p(95), rate, count, max...)
 * @returns {number|null} valor
 */
function valor(metricas, nombre, estadistica) {
  const metrica = metricas[nombre];
  return metrica && metrica.values && estadistica in metrica.values ? metrica.values[estadistica] : null;
}

/**
 * Resumen final: JSON para el reporte de criterios y un texto corto en la consola.
 *
 * @param {object} data resumen de k6
 * @returns {Record<string, string>} archivos y salida estándar
 */
export function handleSummary(data) {
  const m = data.metrics;
  const umbrales = {};
  for (const [nombre, metrica] of Object.entries(m)) {
    for (const [expresion, resultado] of Object.entries(metrica.thresholds || {})) {
      umbrales[`${nombre}: ${expresion}`] = resultado.ok;
    }
  }
  const vusMax = valor(m, 'vus_max', 'max') ?? valor(m, 'vus_max', 'value');
  const resumen = {
    herramienta: 'k6',
    script: 'load-tests/productos.js',
    modo: MODO,
    baseUrl: BASE_URL,
    fecha: new Date().toISOString(),
    escenario: ESCENARIOS[MODO],
    volumen: { productos: data.setup_data ? data.setup_data.totalProductos : null },
    resultado: {
      vusMax,
      solicitudes: valor(m, 'http_reqs', 'count'),
      solicitudesPorSegundo: valor(m, 'http_reqs', 'rate'),
      p95Ms: valor(m, 'http_req_duration', 'p(95)'),
      tasaFallas: valor(m, 'http_req_failed', 'rate'),
      p95PorOperacionMs: {
        buscar: valor(m, 'duracion_buscar', 'p(95)'),
        consultar: valor(m, 'duracion_consultar', 'p(95)'),
        crear: valor(m, 'duracion_crear', 'p(95)'),
      },
      productosCreados: valor(m, 'productos_creados', 'count') ?? 0,
      checksAprobados: valor(m, 'checks', 'rate'),
    },
    umbrales,
    aprobado: Object.values(umbrales).every(Boolean),
    metricas: m,
  };

  const r = resumen.resultado;
  const ms = (v) => (v === null ? 's/d' : `${v.toFixed(1)} ms`);
  const texto = [
    '',
    `Prueba de carga de productos (modo ${MODO}, ${vusMax} VU) contra ${BASE_URL}`,
    `  Productos en la base: ${resumen.volumen.productos ?? 's/d'}`,
    `  Solicitudes: ${r.solicitudes} (${(r.solicitudesPorSegundo ?? 0).toFixed(1)}/s); creados: ${r.productosCreados}`,
    `  p95 total: ${ms(r.p95Ms)} | buscar ${ms(r.p95PorOperacionMs.buscar)} | consultar ${ms(r.p95PorOperacionMs.consultar)} | crear ${ms(r.p95PorOperacionMs.crear)}`,
    `  Fallas HTTP: ${((r.tasaFallas ?? 0) * 100).toFixed(2)} %; checks aprobados: ${((r.checksAprobados ?? 0) * 100).toFixed(2)} %`,
    ...Object.entries(umbrales).map(([u, ok]) => `  ${ok ? 'CUMPLE' : 'FALLA '}  ${u}`),
    `  Resumen JSON: ${ARCHIVO_RESUMEN}`,
    '',
  ].join('\n');

  return { [ARCHIVO_RESUMEN]: JSON.stringify(resumen, null, 2), stdout: texto };
}
