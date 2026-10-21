/**
 * Nombre: criterios.mjs
 * Descripción: Mide el cumplimiento de los criterios de aceptación. Lee docs/criterios-aceptacion.yaml, los
 *              reportes JUnit XML del backend (Surefire), del frontend (Vitest), de Playwright y de
 *              Selenium, el resumen JSON de k6 y la cobertura de JaCoCo; marca cada criterio como CUMPLE
 *              (todas sus pruebas pasaron), FALLA (alguna falló) o SIN PRUEBA, y calcula el porcentaje de
 *              cumplimiento total y por requisito. Con --base compara contra un criterios.json anterior
 *              (en CI, el de la última ejecución de main) y lista los criterios que dejaron de cumplirse.
 *              Escribe en reportes/criterios/ el reporte completo (criterios.md y criterios.json) y uno
 *              corto (criterios-resumen.md) para el comentario del pull request.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Uso (desde la raíz): node scripts/criterios.mjs [--salida reportes/criterios] [--base ruta/criterios.json]
 * Requiere haber instalado la dependencia una vez: cd scripts && npm ci
 * Código de salida: 1 si algún criterio FALLA; 0 en otro caso (SIN PRUEBA no hace fallar).
 * Si --base no existe o no se puede leer, se informa y el reporte sale sin comparación.
 *
 * Fuentes (rutas relativas a la raíz; las que no existen se informan y se omiten):
 *   backend/target/surefire-reports/TEST-*.xml      ./mvnw verify
 *   frontend/reportes/junit/*.xml                   npm test (reportero junit de Vitest)
 *   e2e/playwright/reportes/junit/*.xml             npx playwright test
 *   e2e/selenium/reportes/junit/*.xml               npm test en e2e/selenium
 *   load-tests/results/k6-resumen-*.json            k6 run load-tests/productos.js
 *   backend/target/site/jacoco/jacoco.csv           ./mvnw verify (criterios con medicion: jacoco)
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';

import {
  PATRON_CRITERIO,
  RAIZ,
  buscarArchivos,
  celda,
  fechaArchivo,
  leerCatalogo,
  relativa,
} from './comun.mjs';
import { leerCasosJUnit } from './junit.mjs';

const { values: opciones } = parseArgs({
  options: {
    salida: { type: 'string', default: 'reportes/criterios' },
    base: { type: 'string' },
  },
});
const SALIDA = path.resolve(RAIZ, opciones.salida);
/** criterios.json de una ejecución anterior (en CI, la última de main) para comparar; opcional. */
const BASE = opciones.base ? path.resolve(process.cwd(), opciones.base) : null;

/** Umbral de cobertura de líneas de RNF-MAN-001 (igual a jacoco.umbral del pom). */
const UMBRAL_COBERTURA = 0.7;

/** Usuarios virtuales que exige RNF-REN-003 para que un resumen de k6 cuente. */
const VUS_CARGA = 50;

/**
 * Resultado de una prueba, venga de JUnit o de una medición.
 *
 * @typedef {object} Resultado
 * @property {string} fuente backend, frontend, playwright, selenium, k6 o jacoco
 * @property {string} archivo archivo del reporte, relativo a la raíz
 * @property {string} nombre nombre visible de la prueba
 * @property {'pasa'|'falla'|'omitida'} estado resultado
 * @property {string} [mensaje] causa de la falla
 * @property {string[]} criterios ids de criterios citados en el nombre
 */

// ------------------------------------------------------------------ lectura de JUnit XML

/**
 * Ids de criterios citados en un texto, sin repetir.
 *
 * @param {...string} textos textos donde buscar
 * @returns {string[]} ids
 */
function criteriosEn(...textos) {
  const ids = new Set();
  for (const texto of textos) {
    for (const [, id] of String(texto ?? '').matchAll(PATRON_CRITERIO)) ids.add(id);
  }
  return [...ids];
}

/**
 * Lee los casos de un reporte JUnit XML (Surefire, Vitest, Playwright o mocha-junit-reporter).
 * Un caso con <failure> o <error> falla; con <skipped> se omite; si no, pasa.
 *
 * @param {string} archivo ruta absoluta del XML
 * @param {string} fuente nombre de la fuente
 * @returns {Resultado[]} resultados
 */
function leerJUnit(archivo, fuente) {
  return leerCasosJUnit(archivo).map((caso) => ({
    fuente,
    archivo: relativa(archivo),
    nombre: caso.nombre,
    estado: caso.estado,
    mensaje: caso.mensaje,
    criterios: criteriosEn(caso.nombre, caso.clase),
  }));
}

// ------------------------------------------------------------------ mediciones (k6 y JaCoCo)

/**
 * Convierte los resúmenes de k6 en resultados de los criterios de rendimiento. El umbral de p95 cuenta
 * para CA-RNF-REN-001-1 en cualquier modo; el de fallas cuenta para CA-RNF-REN-003-1 solo si la prueba
 * corrió con 50 usuarios virtuales o más (modo carga).
 *
 * @param {string} archivo ruta absoluta del resumen JSON
 * @returns {{resultados: Resultado[], resumen: object}} resultados y datos para el reporte
 */
function leerK6(archivo) {
  const datos = JSON.parse(readFileSync(archivo, 'utf8'));
  const umbrales = datos.umbrales ?? {};
  const umbral = (prefijo) => Object.entries(umbrales).find(([nombre]) => nombre.startsWith(prefijo));
  const r = datos.resultado ?? {};
  const resultados = [];
  const base = { fuente: 'k6', archivo: relativa(archivo) };

  const p95 = umbral('http_req_duration');
  if (p95) {
    resultados.push({
      ...base,
      nombre: `[CA-RNF-REN-001-1] k6 modo ${datos.modo} (${r.vusMax} VU, ${datos.volumen?.productos ?? '?'} productos): ${p95[0]} → p95 ${r.p95Ms?.toFixed(1)} ms`,
      estado: p95[1] ? 'pasa' : 'falla',
      mensaje: p95[1] ? undefined : `p95 = ${r.p95Ms?.toFixed(1)} ms`,
      criterios: ['CA-RNF-REN-001-1'],
    });
  }
  const fallas = umbral('http_req_failed');
  if (fallas && (r.vusMax ?? 0) >= VUS_CARGA) {
    resultados.push({
      ...base,
      nombre: `[CA-RNF-REN-003-1] k6 modo ${datos.modo} (${r.vusMax} VU): ${fallas[0]} → ${((r.tasaFallas ?? 0) * 100).toFixed(2)} % de fallas`,
      estado: fallas[1] ? 'pasa' : 'falla',
      mensaje: fallas[1] ? undefined : `tasa de fallas = ${((r.tasaFallas ?? 0) * 100).toFixed(2)} %`,
      criterios: ['CA-RNF-REN-003-1'],
    });
  }
  return {
    resultados,
    resumen: {
      archivo: relativa(archivo),
      modo: datos.modo,
      fecha: datos.fecha,
      vusMax: r.vusMax,
      productos: datos.volumen?.productos ?? null,
      solicitudes: r.solicitudes,
      p95Ms: r.p95Ms,
      tasaFallas: r.tasaFallas,
      aprobado: datos.aprobado,
    },
  };
}

/**
 * Cobertura de líneas del backend desde el CSV de JaCoCo, como resultado de los criterios con
 * medicion: jacoco.
 *
 * @param {string} archivo ruta absoluta de jacoco.csv
 * @param {string[]} criterios ids de los criterios que se miden con JaCoCo
 * @returns {{resultados: Resultado[], cobertura: number}} resultados y cobertura (0 a 1)
 */
function leerJacoco(archivo, criterios) {
  const [encabezado, ...filas] = readFileSync(archivo, 'utf8').trim().split(/\r?\n/);
  const columnas = encabezado.split(',');
  const perdidas = columnas.indexOf('LINE_MISSED');
  const cubiertas = columnas.indexOf('LINE_COVERED');
  let totalPerdidas = 0;
  let totalCubiertas = 0;
  for (const fila of filas) {
    const valores = fila.split(',');
    totalPerdidas += Number(valores[perdidas]);
    totalCubiertas += Number(valores[cubiertas]);
  }
  const cobertura = totalCubiertas / Math.max(1, totalPerdidas + totalCubiertas);
  const cumple = cobertura >= UMBRAL_COBERTURA;
  const porcentaje = (cobertura * 100).toFixed(1);
  return {
    cobertura,
    resultados: criterios.map((id) => ({
      fuente: 'jacoco',
      archivo: relativa(archivo),
      nombre: `[${id}] Cobertura de líneas del backend ${porcentaje} % (mínimo ${UMBRAL_COBERTURA * 100} %)`,
      estado: cumple ? 'pasa' : 'falla',
      mensaje: cumple ? undefined : `cobertura ${porcentaje} % < ${UMBRAL_COBERTURA * 100} %`,
      criterios: [id],
    })),
  };
}

// ------------------------------------------------------------------ evaluación

/**
 * Reúne todos los resultados de las fuentes disponibles.
 *
 * @param {import('./comun.mjs').Criterio[]} catalogo criterios
 * @returns {{resultados: Resultado[], fuentes: object[], k6: object[], cobertura: number|null}} datos leídos
 */
function reunirResultados(catalogo) {
  const resultados = [];
  const fuentes = [];
  const junit = [
    ['backend', 'backend/target/surefire-reports', (a) => /[\\/]TEST-[^\\/]+\.xml$/.test(a)],
    ['frontend', 'frontend/reportes/junit', (a) => a.endsWith('.xml')],
    ['playwright', 'e2e/playwright/reportes/junit', (a) => a.endsWith('.xml')],
    ['selenium', 'e2e/selenium/reportes/junit', (a) => a.endsWith('.xml')],
  ];
  for (const [fuente, carpeta, filtro] of junit) {
    const archivos = buscarArchivos(path.join(RAIZ, carpeta), filtro);
    const leidos = archivos.flatMap((a) => leerJUnit(a, fuente));
    resultados.push(...leidos);
    const fechas = archivos.map(fechaArchivo).sort();
    fuentes.push({
      fuente,
      ubicacion: carpeta,
      archivos: archivos.length,
      pruebas: leidos.length,
      fallidas: leidos.filter((r) => r.estado === 'falla').length,
      fecha: fechas.at(-1) ?? null,
    });
  }

  const k6 = [];
  const resumenesK6 = buscarArchivos(path.join(RAIZ, 'load-tests/results'), (a) =>
    /k6-resumen-[^\\/]+\.json$/.test(a),
  );
  for (const archivo of resumenesK6) {
    const { resultados: deK6, resumen } = leerK6(archivo);
    resultados.push(...deK6);
    k6.push(resumen);
  }
  fuentes.push({
    fuente: 'k6',
    ubicacion: 'load-tests/results',
    archivos: resumenesK6.length,
    pruebas: k6.length,
    fallidas: k6.filter((r) => !r.aprobado).length,
    fecha: resumenesK6.map(fechaArchivo).sort().at(-1) ?? null,
  });

  let cobertura = null;
  const csv = path.join(RAIZ, 'backend/target/site/jacoco/jacoco.csv');
  const conJacoco = catalogo.filter((c) => c.medicion === 'jacoco').map((c) => c.id);
  if (existsSync(csv) && conJacoco.length > 0) {
    const leido = leerJacoco(csv, conJacoco);
    resultados.push(...leido.resultados);
    cobertura = leido.cobertura;
  }
  fuentes.push({
    fuente: 'jacoco',
    ubicacion: 'backend/target/site/jacoco/jacoco.csv',
    archivos: existsSync(csv) ? 1 : 0,
    pruebas: cobertura === null ? 0 : 1,
    fallidas: cobertura !== null && cobertura < UMBRAL_COBERTURA ? 1 : 0,
    fecha: fechaArchivo(csv),
  });

  return { resultados, fuentes, k6, cobertura };
}

/**
 * Estado de un criterio según sus pruebas (las omitidas no cuentan).
 *
 * @param {Resultado[]} pruebas resultados que citan el criterio
 * @returns {'CUMPLE'|'FALLA'|'SIN PRUEBA'} estado
 */
function estadoDe(pruebas) {
  const ejecutadas = pruebas.filter((p) => p.estado !== 'omitida');
  if (ejecutadas.length === 0) return 'SIN PRUEBA';
  return ejecutadas.some((p) => p.estado === 'falla') ? 'FALLA' : 'CUMPLE';
}

/**
 * Porcentaje con un decimal.
 *
 * @param {number} parte criterios que cumplen
 * @param {number} total criterios
 * @returns {number} porcentaje
 */
function porcentaje(parte, total) {
  return total === 0 ? 0 : Math.round((parte / total) * 1000) / 10;
}

/**
 * Compara el estado actual de los criterios con el de un reporte anterior (opción --base).
 * "Dejaron de cumplir" son los que en la base estaban en CUMPLE y ahora están en FALLA o SIN PRUEBA;
 * "empezaron a cumplir", lo contrario.
 *
 * @param {{id: string, requisito: string, descripcion: string, estado: string}[]} criterios estado actual
 * @param {{porcentaje: number}} total totales actuales
 * @returns {object|null} comparación, o null si no hay base utilizable
 */
function comparar(criterios, total) {
  if (!BASE) return null;
  if (!existsSync(BASE)) {
    console.warn(`Aviso: no existe el reporte base ${BASE}; se omite la comparación.`);
    return null;
  }
  let base;
  try {
    base = JSON.parse(readFileSync(BASE, 'utf8'));
  } catch (error) {
    console.warn(`Aviso: no se pudo leer el reporte base ${BASE}: ${error.message}`);
    return null;
  }
  const antes = new Map((base.criterios ?? []).map((c) => [c.id, c.estado]));
  const fila = (c) => ({
    id: c.id,
    requisito: c.requisito,
    descripcion: c.descripcion,
    antes: antes.get(c.id),
    ahora: c.estado,
  });
  return {
    base: { generado: base.generado ?? null, porcentaje: base.total?.porcentaje ?? null },
    variacion:
      typeof base.total?.porcentaje === 'number'
        ? Math.round((total.porcentaje - base.total.porcentaje) * 10) / 10
        : null,
    dejaronDeCumplir: criterios.filter((c) => antes.get(c.id) === 'CUMPLE' && c.estado !== 'CUMPLE').map(fila),
    empezaronACumplir: criterios
      .filter((c) => antes.has(c.id) && antes.get(c.id) !== 'CUMPLE' && c.estado === 'CUMPLE')
      .map(fila),
  };
}

/**
 * Evalúa el catálogo contra los resultados.
 *
 * @returns {object} reporte completo
 */
function evaluar() {
  const catalogo = leerCatalogo();
  const { resultados, fuentes, k6, cobertura } = reunirResultados(catalogo);
  const conocidos = new Set(catalogo.map((c) => c.id));

  const criterios = catalogo.map((c) => {
    const pruebas = resultados.filter((r) => r.criterios.includes(c.id));
    const porFuente = {};
    for (const p of pruebas) {
      porFuente[p.fuente] ??= { pasan: 0, fallan: 0, omitidas: 0 };
      porFuente[p.fuente][p.estado === 'pasa' ? 'pasan' : p.estado === 'falla' ? 'fallan' : 'omitidas'] += 1;
    }
    return {
      id: c.id,
      requisito: c.requisito,
      descripcion: c.descripcion,
      estado: estadoDe(pruebas),
      pruebas: pruebas.filter((p) => p.estado !== 'omitida').length,
      porFuente,
      fallas: pruebas
        .filter((p) => p.estado === 'falla')
        .map(({ fuente, archivo, nombre, mensaje }) => ({ fuente, archivo, nombre, mensaje })),
      observacion: c.observacion ? String(c.observacion).replace(/\s+/g, ' ').trim() : undefined,
    };
  });

  const contar = (lista) => ({
    total: lista.length,
    cumple: lista.filter((c) => c.estado === 'CUMPLE').length,
    falla: lista.filter((c) => c.estado === 'FALLA').length,
    sinPrueba: lista.filter((c) => c.estado === 'SIN PRUEBA').length,
  });
  const total = contar(criterios);
  total.porcentaje = porcentaje(total.cumple, total.total);

  const requisitos = [...new Set(criterios.map((c) => c.requisito))].map((requisito) => {
    const conteo = contar(criterios.filter((c) => c.requisito === requisito));
    return { requisito, ...conteo, porcentaje: porcentaje(conteo.cumple, conteo.total) };
  });

  const desconocidos = [
    ...new Set(resultados.flatMap((r) => r.criterios).filter((id) => !conocidos.has(id))),
  ].sort();
  const sinCriterio = resultados.filter((r) => r.criterios.length === 0).length;

  return {
    generado: new Date().toISOString(),
    catalogo: 'docs/criterios-aceptacion.yaml',
    total,
    requisitos,
    criterios,
    comparacion: comparar(criterios, total),
    fuentes,
    k6,
    coberturaBackend: cobertura,
    pruebasLeidas: resultados.length,
    pruebasSinCriterio: sinCriterio,
    criteriosDesconocidos: desconocidos,
  };
}

// ------------------------------------------------------------------ salida

/**
 * Fecha ISO en hora de Bogotá con formato dd/mm/aaaa hh:mm (RNF-INT-003).
 *
 * @param {string|null} iso fecha
 * @returns {string} fecha legible
 */
function fechaLegible(iso) {
  if (!iso) return '—';
  return new Intl.DateTimeFormat('es-CO', {
    timeZone: 'America/Bogota',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(iso));
}

/** Estado de un criterio con su icono. */
const ICONO = { CUMPLE: '✅ CUMPLE', FALLA: '❌ FALLA', 'SIN PRUEBA': '⚪ SIN PRUEBA' };

/**
 * Encabezado común de los dos reportes: fecha, porcentaje total y tabla por requisito.
 *
 * @param {ReturnType<typeof evaluar>} reporte datos evaluados
 * @returns {string[]} líneas Markdown
 */
function encabezado(reporte) {
  const { total } = reporte;
  return [
    '# Cumplimiento de criterios de aceptación',
    '',
    `Generado el ${fechaLegible(reporte.generado)} (hora de Bogotá) por \`scripts/criterios.mjs\` desde ` +
      `\`${reporte.catalogo}\` y ${reporte.pruebasLeidas} resultados de prueba.`,
    '',
    `**Cumplimiento total: ${total.porcentaje} %** — ${total.cumple} de ${total.total} criterios cumplen, ` +
      `${total.falla} fallan y ${total.sinPrueba} no tienen prueba.`,
    '',
    'Un criterio CUMPLE cuando todas las pruebas que citan su id pasaron, FALLA si alguna falló y queda SIN ' +
      'PRUEBA si ningún reporte lo cita. El porcentaje es criterios que cumplen sobre el total del catálogo.',
    '',
    '## Por requisito',
    '',
    '| Requisito | Criterios | Cumple | Falla | Sin prueba | Cumplimiento |',
    '| --- | --- | --- | --- | --- | --- |',
    ...reporte.requisitos.map(
      (r) => `| ${r.requisito} | ${r.total} | ${r.cumple} | ${r.falla} | ${r.sinPrueba} | ${r.porcentaje} % |`,
    ),
    '',
  ];
}

/**
 * Sección con la comparación contra el reporte base (vacía si no se pasó --base).
 *
 * @param {ReturnType<typeof evaluar>} reporte datos evaluados
 * @returns {string[]} líneas Markdown
 */
function seccionComparacion(reporte) {
  const comparacion = reporte.comparacion;
  if (!comparacion) return [];
  const signo = comparacion.variacion > 0 ? '+' : '';
  const tabla = (lista) => [
    '| Criterio | Antes | Ahora | Descripción |',
    '| --- | --- | --- | --- |',
    ...lista.map((c) => `| ${c.id} | ${ICONO[c.antes]} | ${ICONO[c.ahora]} | ${celda(c.descripcion)} |`),
  ];
  return [
    '## Cambios respecto a main',
    '',
    `Base: reporte del ${fechaLegible(comparacion.base.generado)}, con ${comparacion.base.porcentaje ?? '—'} % ` +
      `de cumplimiento` +
      (comparacion.variacion === null ? '.' : ` (variación: ${signo}${comparacion.variacion} puntos).`),
    '',
    '### Dejaron de cumplirse',
    '',
    ...(comparacion.dejaronDeCumplir.length === 0 ? ['Ninguno.'] : tabla(comparacion.dejaronDeCumplir)),
    '',
    '### Empezaron a cumplirse',
    '',
    ...(comparacion.empezaronACumplir.length === 0 ? ['Ninguno.'] : tabla(comparacion.empezaronACumplir)),
    '',
  ];
}

/**
 * Tabla de los criterios que no cumplen, con el detalle de sus fallas o la observación del catálogo.
 *
 * @param {ReturnType<typeof evaluar>} reporte datos evaluados
 * @returns {string[]} líneas Markdown
 */
function seccionPendientes(reporte) {
  const pendientes = reporte.criterios.filter((c) => c.estado !== 'CUMPLE');
  return [
    '## Criterios que no cumplen o no tienen prueba',
    '',
    ...(pendientes.length === 0
      ? ['Ninguno.']
      : [
          '| Criterio | Estado | Descripción | Detalle |',
          '| --- | --- | --- | --- |',
          ...pendientes.map((c) => {
            const detalle =
              c.estado === 'FALLA'
                ? c.fallas.map((f) => `${f.fuente}: ${f.nombre} — ${f.mensaje ?? ''}`).join('<br>')
                : (c.observacion ?? '');
            return `| ${c.id} | ${ICONO[c.estado]} | ${celda(c.descripcion)} | ${celda(detalle)} |`;
          }),
        ]),
    '',
  ];
}

/**
 * Reporte corto para el comentario del pull request: encabezado, comparación y pendientes.
 *
 * @param {ReturnType<typeof evaluar>} reporte datos evaluados
 * @returns {string} Markdown
 */
function markdownResumen(reporte) {
  return [...encabezado(reporte), ...seccionComparacion(reporte), ...seccionPendientes(reporte)].join('\n');
}

/**
 * Reporte completo en Markdown.
 *
 * @param {ReturnType<typeof evaluar>} reporte datos evaluados
 * @returns {string} Markdown
 */
function markdown(reporte) {
  const fuentes = (c) =>
    Object.entries(c.porFuente)
      .map(([f, n]) => `${f} ${n.pasan}${n.fallan ? `/${n.fallan} ✗` : ''}`)
      .join(', ') || '—';

  const lineas = [
    ...encabezado(reporte),
    ...seccionComparacion(reporte),
    ...seccionPendientes(reporte),
    '## Todos los criterios',
    '',
    '| Criterio | Requisito | Estado | Pruebas | Por fuente (pasan/fallan) |',
    '| --- | --- | --- | --- | --- |',
    ...reporte.criterios.map(
      (c) => `| ${c.id} | ${c.requisito} | ${ICONO[c.estado]} | ${c.pruebas} | ${fuentes(c)} |`,
    ),
    '',
    '## Fuentes leídas',
    '',
    '| Fuente | Ubicación | Archivos | Pruebas | Fallidas | Última modificación |',
    '| --- | --- | --- | --- | --- | --- |',
    ...reporte.fuentes.map(
      (f) =>
        `| ${f.fuente} | \`${f.ubicacion}\` | ${f.archivos} | ${f.pruebas} | ${f.fallidas} | ${fechaLegible(f.fecha)} |`,
    ),
    '',
  ];

  if (reporte.k6.length > 0) {
    lineas.push(
      '## Pruebas de carga (k6)',
      '',
      '| Resumen | Modo | VU | Productos en la base | Solicitudes | p95 | Fallas | Umbrales |',
      '| --- | --- | --- | --- | --- | --- | --- | --- |',
      ...reporte.k6.map(
        (k) =>
          `| \`${k.archivo}\` | ${k.modo} | ${k.vusMax} | ${k.productos ?? '—'} | ${k.solicitudes} | ` +
          `${k.p95Ms?.toFixed(1)} ms | ${((k.tasaFallas ?? 0) * 100).toFixed(2)} % | ${k.aprobado ? 'cumplen' : 'no cumplen'} |`,
      ),
      '',
      `CA-RNF-REN-003-1 solo cuenta con un resumen de ${VUS_CARGA} VU o más (modo carga).`,
      '',
    );
  }
  if (reporte.coberturaBackend !== null) {
    lineas.push(
      `Cobertura de líneas del backend (JaCoCo): ${(reporte.coberturaBackend * 100).toFixed(1)} %.`,
      '',
    );
  }
  if (reporte.criteriosDesconocidos.length > 0) {
    lineas.push(
      '## Avisos',
      '',
      `Las pruebas citan criterios que no están en el catálogo: ${reporte.criteriosDesconocidos.join(', ')}.`,
      '',
    );
  }
  return lineas.join('\n');
}

const reporte = evaluar();
mkdirSync(SALIDA, { recursive: true });
writeFileSync(path.join(SALIDA, 'criterios.json'), JSON.stringify(reporte, null, 2), 'utf8');
writeFileSync(path.join(SALIDA, 'criterios.md'), markdown(reporte), 'utf8');
writeFileSync(path.join(SALIDA, 'criterios-resumen.md'), markdownResumen(reporte), 'utf8');

const { total } = reporte;
console.log(
  `Cumplimiento: ${total.porcentaje} % (${total.cumple} de ${total.total} cumplen, ${total.falla} fallan, ` +
    `${total.sinPrueba} sin prueba). Reporte en ${relativa(SALIDA)}/criterios.md y criterios.json.`,
);
if (reporte.comparacion) {
  const { dejaronDeCumplir, empezaronACumplir } = reporte.comparacion;
  console.log(
    `Respecto a la base: ${dejaronDeCumplir.length} dejaron de cumplirse, ${empezaronACumplir.length} empezaron a cumplirse.`,
  );
  for (const c of dejaronDeCumplir) console.log(`  DEJÓ DE CUMPLIR ${c.id} (${c.ahora})`);
}
for (const f of reporte.fuentes.filter((f) => f.archivos === 0)) {
  console.warn(`Aviso: no hay reportes de ${f.fuente} en ${f.ubicacion}.`);
}
for (const c of reporte.criterios.filter((c) => c.estado !== 'CUMPLE')) {
  console.log(`  ${c.estado.padEnd(10)} ${c.id}`);
}
process.exitCode = total.falla > 0 ? 1 : 0;
