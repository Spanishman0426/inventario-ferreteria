/**
 * Nombre: comun.mjs
 * Descripción: Funciones compartidas por casos-de-prueba.mjs y criterios.mjs: rutas del repositorio,
 *              lectura del catálogo docs/criterios-aceptacion.yaml, búsqueda de archivos y clasificación
 *              de los archivos de prueba por nivel (unitaria, integración, e2e, carga).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';

/** Carpeta raíz del repositorio (scripts/ → ..). */
export const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

/** Catálogo de criterios de aceptación. */
export const CATALOGO = path.join(RAIZ, 'docs', 'criterios-aceptacion.yaml');

/** Patrón del id de un criterio dentro de un nombre de prueba, por ejemplo [CA-RF-PRO-001-2]. */
export const PATRON_CRITERIO = /\[(CA-[A-Z]+-[A-Z]+-\d{3}-(?:D)?\d+)\]/g;

/** Niveles de prueba en el orden en que se presentan. */
export const NIVELES = ['unitaria', 'integracion', 'e2e', 'carga'];

/** Nombre visible de cada nivel. */
export const NOMBRE_NIVEL = {
  unitaria: 'Unitaria',
  integracion: 'Integración',
  e2e: 'E2E',
  carga: 'Carga',
};

/**
 * Criterio de aceptación del catálogo.
 *
 * @typedef {object} Criterio
 * @property {string} id identificador CA-...
 * @property {string} requisito requisito al que pertenece (RF-... o RNF-...)
 * @property {string} descripcion texto del criterio
 * @property {string[]} niveles niveles de prueba previstos
 * @property {string} [observacion] aclaración sobre su cobertura
 * @property {string} [origen] origen de un criterio derivado
 * @property {string} [medicion] "k6" o "jacoco" cuando no es una prueba con nombre
 * @property {string[]} [e2e] escenarios E2E que lo verifican
 * @property {{nivel: string, pasos: string[], datos: string, esperado: string}[]} [casos] casos de prueba
 */

/**
 * Lee y valida el catálogo de criterios.
 *
 * @param {string} [archivo] ruta del YAML
 * @returns {Criterio[]} criterios en el orden del archivo
 * @throws {Error} si falta un campo obligatorio, un id se repite o un nivel no existe
 */
export function leerCatalogo(archivo = CATALOGO) {
  const documento = parse(readFileSync(archivo, 'utf8'));
  const criterios = documento?.criterios;
  if (!Array.isArray(criterios) || criterios.length === 0) {
    throw new Error(`${archivo} no tiene la lista "criterios".`);
  }
  const vistos = new Set();
  for (const c of criterios) {
    for (const campo of ['id', 'requisito', 'descripcion', 'niveles']) {
      if (!c[campo]) throw new Error(`Criterio ${c.id ?? '(sin id)'}: falta "${campo}".`);
    }
    if (vistos.has(c.id)) throw new Error(`Criterio repetido: ${c.id}.`);
    vistos.add(c.id);
    for (const nivel of [...c.niveles, ...(c.casos ?? []).map((caso) => caso.nivel)]) {
      if (!NIVELES.includes(nivel)) throw new Error(`Criterio ${c.id}: nivel desconocido "${nivel}".`);
    }
    c.descripcion = String(c.descripcion).trim();
  }
  return criterios;
}

/**
 * Lista los archivos de una carpeta, recursivamente, que cumplen un filtro. Omite node_modules.
 *
 * @param {string} carpeta carpeta absoluta
 * @param {(archivo: string) => boolean} filtro condición sobre la ruta absoluta
 * @returns {string[]} rutas absolutas ordenadas
 */
export function buscarArchivos(carpeta, filtro) {
  if (!existsSync(carpeta)) return [];
  const encontrados = [];
  for (const entrada of readdirSync(carpeta, { withFileTypes: true })) {
    const ruta = path.join(carpeta, entrada.name);
    if (entrada.isDirectory()) {
      if (entrada.name !== 'node_modules') encontrados.push(...buscarArchivos(ruta, filtro));
    } else if (filtro(ruta)) {
      encontrados.push(ruta);
    }
  }
  return encontrados.sort();
}

/**
 * Ruta relativa a la raíz con barras "/", para mostrar en reportes en Windows y Linux.
 *
 * @param {string} ruta ruta absoluta
 * @returns {string} ruta relativa
 */
export function relativa(ruta) {
  return path.relative(RAIZ, ruta).split(path.sep).join('/');
}

/**
 * Archivo de prueba con su nivel, herramienta y criterios que menciona.
 *
 * @typedef {object} ArchivoPrueba
 * @property {string} ruta ruta relativa a la raíz
 * @property {string} nivel unitaria, integracion, e2e o carga
 * @property {string} herramienta JUnit, Vitest, Playwright, Selenium o k6
 * @property {Map<string, number>} criterios id del criterio → veces que aparece
 */

/**
 * Nivel de una clase de prueba del backend según su sufijo: las de capa web (MockMvc), base de datos y
 * contexto completo son de integración; las demás (JUnit + Mockito) son unitarias.
 *
 * @param {string} archivo ruta del .java
 * @returns {'unitaria'|'integracion'} nivel
 */
function nivelBackend(archivo) {
  const clase = path.basename(archivo, '.java');
  return /(Integracion|BaseDatos|Web)Tests$|^(ContratoOpenApi|MigracionesFlyway)Tests$/.test(clase)
    ? 'integracion'
    : 'unitaria';
}

/**
 * Busca los archivos de prueba del repositorio y los criterios que cita cada uno.
 *
 * @returns {ArchivoPrueba[]} archivos con al menos un criterio
 */
export function archivosDePrueba() {
  const fuentes = [
    {
      carpeta: 'backend/src/test/java',
      filtro: (a) => a.endsWith('Tests.java'),
      nivel: nivelBackend,
      herramienta: 'JUnit',
    },
    {
      carpeta: 'frontend/src',
      filtro: (a) => /\.test\.jsx?$/.test(a),
      nivel: () => 'unitaria',
      herramienta: 'Vitest',
    },
    {
      carpeta: 'e2e/playwright/tests',
      filtro: (a) => a.endsWith('.spec.js'),
      nivel: () => 'e2e',
      herramienta: 'Playwright',
    },
    {
      carpeta: 'e2e/selenium/tests',
      filtro: (a) => a.endsWith('.spec.js'),
      nivel: () => 'e2e',
      herramienta: 'Selenium',
    },
    {
      carpeta: 'load-tests',
      filtro: (a) => a.endsWith('.js') && !a.includes(`${path.sep}results${path.sep}`),
      nivel: () => 'carga',
      herramienta: 'k6',
    },
  ];
  const archivos = [];
  for (const fuente of fuentes) {
    for (const archivo of buscarArchivos(path.join(RAIZ, fuente.carpeta), fuente.filtro)) {
      const criterios = new Map();
      for (const [, id] of readFileSync(archivo, 'utf8').matchAll(PATRON_CRITERIO)) {
        criterios.set(id, (criterios.get(id) ?? 0) + 1);
      }
      if (criterios.size > 0) {
        archivos.push({
          ruta: relativa(archivo),
          nivel: fuente.nivel(archivo),
          herramienta: fuente.herramienta,
          criterios,
        });
      }
    }
  }
  return archivos;
}

/**
 * Fecha de modificación de un archivo en formato ISO, o null si no existe.
 *
 * @param {string} archivo ruta absoluta
 * @returns {string|null} fecha
 */
export function fechaArchivo(archivo) {
  return existsSync(archivo) ? statSync(archivo).mtime.toISOString() : null;
}

/**
 * Fecha de hoy en Bogotá (AAAA-MM-DD), para los encabezados de los archivos generados.
 *
 * @returns {string} fecha
 */
export function hoy() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date());
}

/**
 * Escapa el texto de una celda de tabla Markdown.
 *
 * @param {unknown} texto contenido
 * @returns {string} texto sin saltos de línea ni barras verticales sueltas
 */
export function celda(texto) {
  return String(texto ?? '')
    .replace(/\s*\n\s*/g, ' ')
    .replace(/\|/g, '\\|')
    .trim();
}
