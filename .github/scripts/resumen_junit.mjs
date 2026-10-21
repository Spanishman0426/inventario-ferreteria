/**
 * Nombre: resumen_junit.mjs
 * Descripción: Resumen de pruebas para el pipeline de CI a partir de reportes JUnit XML (frontend con Vitest,
 *              Playwright y Selenium): tabla en el Job Summary, salidas del paso (total, fallidas, reportes)
 *              y un JSON con la misma forma que el de resumen_pruebas_backend.py, que usa reportar-fallas.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Uso (desde la raíz; no necesita npm ci, funciona en Windows y Linux):
 *   node .github/scripts/resumen_junit.mjs --nombre Frontend --junit frontend/reportes/junit \
 *     --cobertura frontend/coverage/coverage-summary.json --salida-json resumen-frontend.json
 */
import { appendFileSync, existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';

import { leerCasosJUnit } from '../../scripts/junit.mjs';

/** Máximo de pruebas fallidas que se listan en el Job Summary. */
const MAXIMO_FALLAS_LISTADAS = 50;

const { values: opciones } = parseArgs({
  options: {
    nombre: { type: 'string' },
    junit: { type: 'string' },
    cobertura: { type: 'string' },
    'salida-json': { type: 'string' },
  },
});
for (const obligatoria of ['nombre', 'junit', 'salida-json']) {
  if (!opciones[obligatoria]) {
    console.error(`Falta la opción --${obligatoria}.`);
    process.exit(2);
  }
}

/**
 * Archivos XML de una carpeta (recursivo).
 *
 * @param {string} carpeta carpeta de los reportes
 * @returns {string[]} rutas ordenadas
 */
function archivosXml(carpeta) {
  if (!existsSync(carpeta)) return [];
  return readdirSync(carpeta, { recursive: true })
    .map((relativa) => path.join(carpeta, String(relativa)))
    .filter((ruta) => ruta.endsWith('.xml'))
    .sort();
}

/**
 * Cobertura de líneas (0 a 1) del coverage-summary.json de Vitest (reportero json-summary), o null.
 *
 * @param {string|undefined} archivo ruta del resumen
 * @returns {number|null} cobertura
 */
function leerCobertura(archivo) {
  if (!archivo || !existsSync(archivo)) return null;
  const pct = JSON.parse(readFileSync(archivo, 'utf8'))?.total?.lines?.pct;
  return typeof pct === 'number' ? pct / 100 : null;
}

/**
 * Escapa un texto para una celda de tabla Markdown.
 *
 * @param {unknown} texto contenido
 * @returns {string} texto de una línea sin barras sueltas
 */
function celda(texto) {
  return String(texto ?? '').replace(/\|/g, '\\|').replace(/\s*\n\s*/g, ' ');
}

const archivos = archivosXml(opciones.junit);
const casos = archivos.flatMap((archivo) => leerCasosJUnit(archivo));
const fallas = casos
  .filter((c) => c.estado === 'falla')
  .map((c) => ({ clase: c.clase, prueba: c.nombre, mensaje: c.mensaje ?? '' }));
const cobertura = leerCobertura(opciones.cobertura);

const datos = {
  total: casos.length,
  fallidas: fallas.length,
  omitidas: casos.filter((c) => c.estado === 'omitida').length,
  reportes: archivos.length,
  cobertura_lineas: cobertura === null ? null : Math.round(cobertura * 10000) / 10000,
  fallas,
};

const estado = datos.reportes > 0 && datos.fallidas === 0 ? '✅' : '❌';
const lineas = [`## ${estado} ${opciones.nombre}: pruebas`, ''];
if (datos.reportes === 0) {
  lineas.push(`No se encontraron reportes JUnit en \`${opciones.junit}\`: las pruebas no llegaron a ejecutarse.`, '');
}
lineas.push(
  '| Indicador | Valor |',
  '| --- | --- |',
  `| Pruebas totales | ${datos.total} |`,
  `| Fallidas | ${datos.fallidas} |`,
  `| Omitidas | ${datos.omitidas} |`,
);
if (opciones.cobertura) {
  lineas.push(
    `| Cobertura de líneas | ${cobertura === null ? 'sin reporte' : `${(cobertura * 100).toFixed(1)} %`} |`,
  );
}
if (fallas.length > 0) {
  lineas.push('', '### Pruebas fallidas', '', '| Archivo o clase | Prueba | Mensaje |', '| --- | --- | --- |');
  for (const f of fallas.slice(0, MAXIMO_FALLAS_LISTADAS)) {
    lineas.push(`| ${celda(f.clase)} | ${celda(f.prueba)} | ${celda(f.mensaje)} |`);
  }
  if (fallas.length > MAXIMO_FALLAS_LISTADAS) {
    lineas.push('', `…y ${fallas.length - MAXIMO_FALLAS_LISTADAS} más en el artefacto de reportes.`);
  }
}
const markdown = `${lineas.join('\n')}\n`;

if (process.env.GITHUB_STEP_SUMMARY) {
  appendFileSync(process.env.GITHUB_STEP_SUMMARY, markdown, 'utf8');
} else {
  console.log(markdown);
}
writeFileSync(opciones['salida-json'], JSON.stringify(datos, null, 2), 'utf8');
if (process.env.GITHUB_OUTPUT) {
  appendFileSync(
    process.env.GITHUB_OUTPUT,
    `total=${datos.total}\nfallidas=${datos.fallidas}\nreportes=${datos.reportes}\n`,
    'utf8',
  );
}
