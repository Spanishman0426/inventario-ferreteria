/**
 * Nombre: junit.mjs
 * Descripción: Lectura de reportes JUnit XML (Surefire, Vitest, Playwright y mocha-junit-reporter) sin
 *              dependencias externas. La usan scripts/criterios.mjs y .github/scripts/resumen_junit.mjs.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { readFileSync } from 'node:fs';

/**
 * Caso de prueba leído de un reporte JUnit.
 *
 * @typedef {object} CasoJUnit
 * @property {string} nombre nombre visible de la prueba (atributo name)
 * @property {string} clase clase o archivo de la prueba (atributo classname)
 * @property {'pasa'|'falla'|'omitida'} estado resultado
 * @property {string} [mensaje] causa de la falla (hasta 300 caracteres)
 */

/**
 * Decodifica las entidades XML de un texto.
 *
 * @param {string} texto texto con entidades
 * @returns {string} texto decodificado
 */
export function decodificar(texto) {
  return texto
    .replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(Number(d)))
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&');
}

/**
 * Atributos de una etiqueta XML.
 *
 * @param {string} etiqueta texto de apertura, por ejemplo `testcase name="x" time="1"`
 * @returns {Record<string, string>} atributos decodificados
 */
export function atributos(etiqueta) {
  const resultado = {};
  for (const [, nombre, valor] of etiqueta.matchAll(/([\w:-]+)\s*=\s*"([^"]*)"/g)) {
    resultado[nombre] = decodificar(valor);
  }
  return resultado;
}

/**
 * Lee los casos de un reporte JUnit XML. Un caso con <failure> o <error> falla; con <skipped> se omite;
 * si no, pasa.
 *
 * @param {string} archivo ruta del XML
 * @returns {CasoJUnit[]} casos en el orden del archivo
 */
export function leerCasosJUnit(archivo) {
  const xml = readFileSync(archivo, 'utf8');
  const casos = [];
  const patron = /<testcase\b([^>]*?)(\/>|>([\s\S]*?)<\/testcase>)/g;
  for (const [, apertura, , cuerpo = ''] of xml.matchAll(patron)) {
    const { name = '', classname = '' } = atributos(apertura);
    const falla = /<(failure|error)\b([^>]*?)(\/>|>([\s\S]*?)<\/\1>)/.exec(cuerpo);
    const omitida = /<skipped\b/.test(cuerpo);
    let mensaje;
    if (falla) {
      const { message, type } = atributos(falla[2]);
      mensaje = (message || decodificar(falla[4] ?? '').split('\n')[0] || type || falla[1]).slice(0, 300);
    }
    casos.push({
      nombre: name,
      clase: classname,
      estado: falla ? 'falla' : omitida ? 'omitida' : 'pasa',
      mensaje,
    });
  }
  return casos;
}
