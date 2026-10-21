/**
 * Nombre: casos-de-prueba.mjs
 * Descripción: Genera docs/pruebas/casos-de-prueba.md a partir de docs/criterios-aceptacion.yaml: por cada
 *              criterio, sus casos con id, nivel, pasos, datos, resultado esperado y los archivos de prueba
 *              que lo implementan (buscados en el código por el id del criterio y el nivel).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Uso (desde la raíz): node scripts/casos-de-prueba.mjs [--salida docs/pruebas/casos-de-prueba.md]
 * Requiere haber instalado la dependencia una vez: cd scripts && npm ci
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';

import {
  NIVELES,
  NOMBRE_NIVEL,
  RAIZ,
  archivosDePrueba,
  celda,
  hoy,
  leerCatalogo,
  relativa,
} from './comun.mjs';

const { values: opciones } = parseArgs({
  options: { salida: { type: 'string', default: 'docs/pruebas/casos-de-prueba.md' } },
});
const SALIDA = path.resolve(RAIZ, opciones.salida);

/**
 * Id de un caso: CP-<criterio sin "CA-">-<nn>.
 *
 * @param {string} criterio id del criterio
 * @param {number} indice posición del caso (desde 0)
 * @returns {string} id del caso
 */
function idCaso(criterio, indice) {
  return `CP-${criterio.replace(/^CA-/, '')}-${String(indice + 1).padStart(2, '0')}`;
}

/**
 * Enlace Markdown a un archivo del repositorio, relativo a la carpeta del documento generado.
 *
 * @param {string} ruta ruta relativa a la raíz
 * @returns {string} enlace
 */
function enlace(ruta) {
  const desdeDocumento = path
    .relative(path.dirname(SALIDA), path.join(RAIZ, ruta))
    .split(path.sep)
    .join('/');
  return `[${ruta}](${desdeDocumento})`;
}

/**
 * Construye el documento.
 *
 * @returns {{markdown: string, avisos: string[], totales: object}} documento, avisos y totales
 */
function generar() {
  const criterios = leerCatalogo();
  const archivos = archivosDePrueba();
  const conocidos = new Set(criterios.map((c) => c.id));
  const avisos = [];

  /** Archivos que citan un criterio en un nivel. */
  const implementan = (criterio, nivel) =>
    archivos.filter((a) => a.nivel === nivel && a.criterios.has(criterio));

  // Pruebas que citan criterios que no están en el catálogo
  for (const a of archivos) {
    for (const id of a.criterios.keys()) {
      if (!conocidos.has(id)) avisos.push(`${a.ruta} cita ${id}, que no está en el catálogo.`);
    }
  }

  const totales = { criterios: criterios.length, casos: 0, implementados: 0, pendientes: 0 };
  const filasResumen = [];
  const secciones = [];
  let requisitoActual = null;

  for (const c of criterios) {
    const casos = c.casos ?? [];
    if (casos.length === 0) avisos.push(`${c.id} no tiene casos en el catálogo.`);

    // Niveles con pruebas que no tienen caso descrito
    for (const nivel of NIVELES) {
      if (implementan(c.id, nivel).length > 0 && !casos.some((caso) => caso.nivel === nivel)) {
        avisos.push(`${c.id} tiene pruebas de nivel ${nivel} sin caso descrito en el catálogo.`);
      }
    }

    const filasCasos = casos.map((caso, i) => {
      const id = idCaso(c.id, i);
      const enArchivos = implementan(c.id, caso.nivel);
      totales.casos += 1;
      if (enArchivos.length > 0) totales.implementados += 1;
      else totales.pendientes += 1;
      const pasos = (Array.isArray(caso.pasos) ? caso.pasos : [caso.pasos])
        .map((paso, n) => `${n + 1}. ${celda(paso)}`)
        .join('<br>');
      const implementado =
        enArchivos.length > 0
          ? enArchivos
              .map((a) => {
                const veces = a.criterios.get(c.id);
                return `${enlace(a.ruta)} (${a.herramienta}, ${veces} ${veces === 1 ? 'mención' : 'menciones'})`;
              })
              .join('<br>')
          : '_Sin prueba automatizada_';
      return `| ${id} | ${NOMBRE_NIVEL[caso.nivel]} | ${pasos} | ${celda(caso.datos)} | ${celda(caso.esperado)} | ${implementado} |`;
    });

    const implementados = casos.filter((caso) => implementan(c.id, caso.nivel).length > 0).length;
    const estado =
      casos.length === 0
        ? 'Sin casos'
        : implementados === casos.length
          ? 'Implementado'
          : implementados === 0
            ? c.medicion
              ? `Medición (${c.medicion})`
              : 'Pendiente'
            : 'Parcial';
    filasResumen.push(
      `| [${c.id}](#${c.id.toLowerCase()}) | ${c.requisito} | ${casos.length} | ${casos
        .map((caso) => NOMBRE_NIVEL[caso.nivel])
        .join(', ')} | ${estado} |`,
    );

    if (c.requisito !== requisitoActual) {
      requisitoActual = c.requisito;
      secciones.push(`## ${c.requisito}`, '');
    }
    secciones.push(`### ${c.id}`, '', `**Criterio.** ${c.descripcion}`, '');
    if (c.origen) secciones.push(`**Origen (criterio derivado).** ${celda(c.origen)}`, '');
    if (c.e2e?.length) secciones.push(`**Escenarios E2E.** ${c.e2e.join(', ')}`, '');
    if (c.medicion) {
      secciones.push(
        `**Medición.** ${
          c.medicion === 'k6'
            ? 'umbral del resumen de k6 (`load-tests/results/k6-resumen-*.json`).'
            : 'cobertura de líneas de JaCoCo (`backend/target/site/jacoco/jacoco.csv`).'
        }`,
        '',
      );
    }
    if (c.observacion) secciones.push(`**Observación.** ${celda(c.observacion)}`, '');
    if (filasCasos.length > 0) {
      secciones.push(
        '| Caso | Nivel | Pasos | Datos | Resultado esperado | Implementado en |',
        '| --- | --- | --- | --- | --- | --- |',
        ...filasCasos,
        '',
      );
    }
  }

  const fecha = hoy();
  const markdown = [
    '<!--',
    '  Nombre: casos-de-prueba.md',
    '  Descripción: Casos de prueba por criterio de aceptación, generados desde docs/criterios-aceptacion.yaml',
    '               con los archivos de prueba que los implementan. No editar a mano: regenerar con',
    '               node scripts/casos-de-prueba.mjs.',
    `  Fecha de creación: 2026-10-21`,
    `  Última actualización: ${fecha}`,
    '  Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)',
    '-->',
    '',
    '# Casos de prueba por criterio de aceptación',
    '',
    `${totales.criterios} criterios y ${totales.casos} casos: ${totales.implementados} con prueba automatizada y ` +
      `${totales.pendientes} sin ella. Este documento se genera desde ` +
      '[`criterios-aceptacion.yaml`](../criterios-aceptacion.yaml); para cambiar un caso se edita el catálogo y se ' +
      'ejecuta `node scripts/casos-de-prueba.mjs`.',
    '',
    '**Cómo leerlo.** El id del caso es `CP-<criterio>-<nn>`. "Implementado en" lista los archivos que citan el id ' +
      'del criterio (por ejemplo `[CA-RF-PRO-001-2]`) en el nombre visible de sus pruebas y que son del mismo nivel ' +
      'que el caso; el número de menciones aproxima cuántas pruebas lo verifican (una prueba parametrizada cuenta una ' +
      'vez aquí, pero ejecuta varias combinaciones). Niveles: clases `*IntegracionTests`, `*BaseDatosTests` y ' +
      '`*WebTests` del backend son de integración; las demás clases JUnit y las pruebas de Vitest, unitarias; ' +
      'Playwright y Selenium, E2E; k6, carga. El resultado de cada ejecución está en `reportes/criterios/` ' +
      '(`node scripts/criterios.mjs`).',
    '',
    '## Resumen',
    '',
    '| Criterio | Requisito | Casos | Niveles | Estado |',
    '| --- | --- | --- | --- | --- |',
    ...filasResumen,
    '',
    ...secciones,
    ...(avisos.length > 0
      ? ['## Avisos de consistencia', '', ...avisos.map((a) => `- ${a}`), '']
      : []),
  ].join('\n');

  return { markdown, avisos, totales };
}

const { markdown, avisos, totales } = generar();
mkdirSync(path.dirname(SALIDA), { recursive: true });
writeFileSync(SALIDA, markdown, 'utf8');
console.log(
  `${relativa(SALIDA)}: ${totales.criterios} criterios, ${totales.casos} casos ` +
    `(${totales.implementados} implementados, ${totales.pendientes} sin prueba).`,
);
for (const aviso of avisos) console.warn(`Aviso: ${aviso}`);
