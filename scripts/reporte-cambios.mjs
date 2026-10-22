/**
 * Nombre: reporte-cambios.mjs
 * Descripción: Reporte de cambios por integrante a partir de git log, en Markdown y JSON. Para un rango de fechas
 *              (por defecto la última semana) y para el total del proyecto muestra, por integrante: commits,
 *              archivos, líneas agregadas y eliminadas, área afectada (backend, frontend, e2e, carga, ci, docs,
 *              otros) y la lista de cambios por tipo (feat, fix, test, docs, ci, refactor, otros). Las identidades
 *              se unifican con el archivo .mailmap de la raíz. Lo usa .github/workflows/reportes.yml.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Uso (desde la raíz): node scripts/reporte-cambios.mjs [--desde AAAA-MM-DD] [--hasta AAAA-MM-DD]
 *                        [--salida reportes/seguimiento]
 * Por defecto: desde = hoy − 6 días y hasta = hoy (7 días, hora de Colombia). Los commits de merge no se cuentan
 * (son la fusión de un pull request, no trabajo nuevo); el reporte informa cuántos hubo.
 * Requiere el historial completo (en CI: actions/checkout con fetch-depth: 0) y haber ejecutado una vez
 * cd scripts && npm ci. Escribe cambios.md y cambios.json en la carpeta de salida.
 */
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';

import { RAIZ, celda, hoy, relativa } from './comun.mjs';

const { values: opciones } = parseArgs({
  options: {
    desde: { type: 'string' },
    hasta: { type: 'string' },
    salida: { type: 'string', default: 'reportes/seguimiento' },
  },
});
const SALIDA = path.resolve(RAIZ, opciones.salida);

/** Desfase horario de Colombia (sin horario de verano), para interpretar los límites del rango. */
const DESFASE_BOGOTA = '-05:00';

/** Áreas en el orden en que se presentan. */
const AREAS = ['backend', 'frontend', 'e2e', 'carga', 'ci', 'docs', 'otros'];

/** Tipos de cambio en el orden en que se presentan (formato de commit "tipo(alcance): descripción"). */
const TIPOS = ['feat', 'fix', 'test', 'docs', 'ci', 'refactor', 'otros'];

/** Separadores que no aparecen en mensajes ni rutas. */
const INICIO_COMMIT = '\u001e';
const CAMPO = '\u001f';

// ------------------------------------------------------------------ fechas

/**
 * Valida una fecha AAAA-MM-DD.
 *
 * @param {string} valor texto
 * @param {string} opcion nombre de la opción, para el mensaje de error
 * @returns {string} la misma fecha
 */
function validarFecha(valor, opcion) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(valor) || Number.isNaN(Date.parse(`${valor}T00:00:00Z`))) {
    console.error(`--${opcion} debe tener el formato AAAA-MM-DD (recibido: ${valor}).`);
    process.exit(2);
  }
  return valor;
}

/**
 * Suma días a una fecha AAAA-MM-DD.
 *
 * @param {string} fecha fecha base
 * @param {number} dias días a sumar (negativo para restar)
 * @returns {string} fecha resultante
 */
function sumarDias(fecha, dias) {
  const d = new Date(`${fecha}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + dias);
  return d.toISOString().slice(0, 10);
}

const HASTA = opciones.hasta ? validarFecha(opciones.hasta, 'hasta') : hoy();
const DESDE = opciones.desde ? validarFecha(opciones.desde, 'desde') : sumarDias(HASTA, -6);
if (DESDE > HASTA) {
  console.error(`--desde (${DESDE}) es posterior a --hasta (${HASTA}).`);
  process.exit(2);
}

// ------------------------------------------------------------------ lectura de git log

/**
 * Commit leído de git log.
 *
 * @typedef {object} Commit
 * @property {string} hash hash completo
 * @property {string} autor nombre (unificado por .mailmap)
 * @property {string} correo correo (unificado por .mailmap)
 * @property {string} fecha fecha del autor AAAA-MM-DD
 * @property {string} asunto primera línea del mensaje
 * @property {string} tipo feat, fix, test, docs, ci, refactor u otros
 * @property {{ruta: string, agregadas: number, eliminadas: number}[]} archivos archivos con sus líneas
 */

/**
 * Ejecuta git en la raíz del repositorio.
 *
 * @param {string[]} argumentos argumentos de git
 * @returns {string} salida estándar
 */
function git(argumentos) {
  return execFileSync('git', argumentos, { cwd: RAIZ, encoding: 'utf8', maxBuffer: 256 * 1024 * 1024 });
}

/**
 * Filtros de fecha de git para el rango (o vacío para el total).
 *
 * @param {{desde: string, hasta: string}|null} rango rango de fechas
 * @returns {string[]} argumentos
 */
function filtroFechas(rango) {
  return rango
    ? [`--since=${rango.desde}T00:00:00${DESFASE_BOGOTA}`, `--until=${rango.hasta}T23:59:59${DESFASE_BOGOTA}`]
    : [];
}

/**
 * Tipo de un commit según su asunto "tipo(alcance): descripción".
 *
 * @param {string} asunto primera línea del mensaje
 * @returns {string} tipo conocido u "otros"
 */
function tipoDe(asunto) {
  const coincidencia = asunto.match(/^(\w+)(?:\([^)]*\))?!?:/);
  const tipo = coincidencia?.[1].toLowerCase();
  return TIPOS.includes(tipo) ? tipo : 'otros';
}

/**
 * Área de un archivo según su ruta.
 *
 * @param {string} ruta ruta relativa con "/"
 * @returns {string} área
 */
function areaDe(ruta) {
  if (ruta.startsWith('backend/')) return 'backend';
  if (ruta.startsWith('frontend/')) return 'frontend';
  if (ruta.startsWith('e2e/')) return 'e2e';
  if (ruta.startsWith('load-tests/')) return 'carga';
  if (/^(\.github\/|scripts\/|docker-compose[^/]*$|render\.yaml$)/.test(ruta)) return 'ci';
  if (ruta.startsWith('docs/') || ruta.endsWith('.md')) return 'docs';
  return 'otros';
}

/**
 * Lee los commits sin merges de un rango (o de todo el historial).
 *
 * @param {{desde: string, hasta: string}|null} rango rango de fechas
 * @returns {Commit[]} commits del más reciente al más antiguo
 */
function leerCommits(rango) {
  const salida = git([
    'log', '--no-merges', '--use-mailmap', '--numstat', '--no-color', '--no-renames',
    `--format=${INICIO_COMMIT}%H${CAMPO}%aN${CAMPO}%aE${CAMPO}%as${CAMPO}%s`,
    ...filtroFechas(rango),
  ]);
  const commits = [];
  for (const bloque of salida.split(INICIO_COMMIT).slice(1)) {
    const [cabecera, ...lineas] = bloque.split('\n');
    const [hash, autor, correo, fecha, asunto] = cabecera.split(CAMPO);
    const archivos = [];
    for (const linea of lineas) {
      const partes = linea.split('\t');
      if (partes.length < 3) continue;
      // Con --no-renames un renombre cuenta como archivo eliminado y agregado.
      // Los binarios aparecen con "-" en vez de números: cuentan como archivo sin líneas
      archivos.push({
        ruta: partes.slice(2).join('\t'),
        agregadas: partes[0] === '-' ? 0 : Number(partes[0]),
        eliminadas: partes[1] === '-' ? 0 : Number(partes[1]),
      });
    }
    commits.push({ hash, autor, correo, fecha, asunto, tipo: tipoDe(asunto), archivos });
  }
  return commits;
}

/**
 * Cuenta los commits de merge del rango (se informan, pero no se suman).
 *
 * @param {{desde: string, hasta: string}|null} rango rango de fechas
 * @returns {number} cantidad de merges
 */
function contarMerges(rango) {
  return Number(git(['rev-list', '--count', '--merges', 'HEAD', ...filtroFechas(rango)]).trim());
}

// ------------------------------------------------------------------ agregación

/**
 * Resumen de un integrante.
 *
 * @typedef {object} Integrante
 * @property {string} nombre nombre unificado
 * @property {string} correo correo unificado
 * @property {number} commits commits
 * @property {number} archivos archivos distintos tocados
 * @property {number} agregadas líneas agregadas
 * @property {number} eliminadas líneas eliminadas
 * @property {Record<string, {archivos: number, agregadas: number, eliminadas: number}>} areas por área
 * @property {Record<string, {fecha: string, hash: string, asunto: string}[]>} cambios por tipo
 */

/**
 * Agrupa los commits por integrante.
 *
 * @param {Commit[]} commits commits del período
 * @returns {Integrante[]} integrantes ordenados por commits descendentes
 */
function porIntegrante(commits) {
  const mapa = new Map();
  for (const c of commits) {
    const clave = `${c.autor} <${c.correo}>`;
    if (!mapa.has(clave)) {
      mapa.set(clave, {
        nombre: c.autor,
        correo: c.correo,
        commits: 0,
        rutas: new Set(),
        agregadas: 0,
        eliminadas: 0,
        areas: {},
        cambios: {},
      });
    }
    const i = mapa.get(clave);
    i.commits += 1;
    (i.cambios[c.tipo] ??= []).push({ fecha: c.fecha, hash: c.hash.slice(0, 7), asunto: c.asunto });
    for (const a of c.archivos) {
      i.rutas.add(a.ruta);
      i.agregadas += a.agregadas;
      i.eliminadas += a.eliminadas;
      const area = (i.areas[areaDe(a.ruta)] ??= { rutas: new Set(), agregadas: 0, eliminadas: 0 });
      area.rutas.add(a.ruta);
      area.agregadas += a.agregadas;
      area.eliminadas += a.eliminadas;
    }
  }
  return [...mapa.values()]
    .map(({ rutas, areas, ...resto }) => ({
      ...resto,
      archivos: rutas.size,
      areas: Object.fromEntries(
        AREAS.filter((a) => areas[a]).map((a) => [
          a,
          { archivos: areas[a].rutas.size, agregadas: areas[a].agregadas, eliminadas: areas[a].eliminadas },
        ]),
      ),
    }))
    .sort((a, b) => b.commits - a.commits || a.nombre.localeCompare(b.nombre));
}

/**
 * Calcula un período completo: integrantes y totales.
 *
 * @param {string} titulo nombre del período
 * @param {{desde: string, hasta: string}|null} rango rango o null para el total
 * @returns {object} período
 */
function periodo(titulo, rango) {
  const commits = leerCommits(rango);
  const rutas = new Set(commits.flatMap((c) => c.archivos.map((a) => a.ruta)));
  return {
    titulo,
    rango,
    merges: contarMerges(rango),
    total: {
      commits: commits.length,
      archivos: rutas.size,
      agregadas: commits.reduce((s, c) => s + c.archivos.reduce((t, a) => t + a.agregadas, 0), 0),
      eliminadas: commits.reduce((s, c) => s + c.archivos.reduce((t, a) => t + a.eliminadas, 0), 0),
    },
    integrantes: porIntegrante(commits),
  };
}

// ------------------------------------------------------------------ Markdown

/** Número con separador de miles colombiano. */
const numero = (n) => n.toLocaleString('es-CO');

/**
 * Markdown de un período.
 *
 * @param {object} p período calculado por periodo()
 * @param {boolean} conLista si se incluye la lista de cambios por tipo
 * @returns {string[]} líneas
 */
function markdownPeriodo(p, conLista) {
  const lineas = [`## ${p.titulo}`, ''];
  if (p.total.commits === 0) {
    lineas.push(`No hay commits en este período${p.merges ? ` (solo ${p.merges} merges)` : ''}.`);
    return lineas;
  }
  lineas.push(
    `${numero(p.total.commits)} commits, ${numero(p.total.archivos)} archivos, ` +
      `+${numero(p.total.agregadas)} / −${numero(p.total.eliminadas)} líneas` +
      `${p.merges ? ` (además, ${p.merges} merges de pull requests que no se cuentan)` : ''}.`,
    '',
    '| Integrante | Commits | Archivos | Líneas + | Líneas − | Áreas | Tipos |',
    '| --- | --- | --- | --- | --- | --- | --- |',
    ...p.integrantes.map(
      (i) =>
        `| ${celda(i.nombre)} | ${i.commits} | ${i.archivos} | ${numero(i.agregadas)} | ${numero(i.eliminadas)} | ` +
        `${Object.keys(i.areas).join(', ')} | ` +
        `${TIPOS.filter((t) => i.cambios[t]).map((t) => `${t} ${i.cambios[t].length}`).join(', ')} |`,
    ),
  );
  for (const i of p.integrantes) {
    lineas.push(
      '',
      `### ${i.nombre} <${i.correo}>`,
      '',
      '| Área | Archivos | Líneas + | Líneas − |',
      '| --- | --- | --- | --- |',
      ...Object.entries(i.areas).map(
        ([a, v]) => `| ${a} | ${v.archivos} | ${numero(v.agregadas)} | ${numero(v.eliminadas)} |`,
      ),
    );
    if (conLista) {
      for (const tipo of TIPOS.filter((t) => i.cambios[t])) {
        lineas.push('', `**${tipo}** (${i.cambios[tipo].length})`, '');
        lineas.push(...i.cambios[tipo].map((c) => `- ${c.fecha} · \`${c.hash}\` · ${celda(c.asunto)}`));
      }
    }
  }
  return lineas;
}

// ------------------------------------------------------------------ principal

const reporte = {
  fecha: hoy(),
  semana: periodo(`Del ${DESDE} al ${HASTA}`, { desde: DESDE, hasta: HASTA }),
  total: periodo('Total del proyecto', null),
};

// En el total se omite la lista de commits para que el issue no crezca sin límite; está en cambios.json
const md = [
  `# Reporte de cambios — ${reporte.fecha}`,
  '',
  'Identidades unificadas con `.mailmap`. Áreas: backend, frontend, e2e, carga (load-tests), ci (.github, scripts, ' +
    'docker-compose, render.yaml), docs (docs/ y .md) y otros. Tipos según el formato `tipo(alcance): descripción`.',
  '',
  ...markdownPeriodo(reporte.semana, true),
  '',
  ...markdownPeriodo(reporte.total, false),
  '',
].join('\n');

mkdirSync(SALIDA, { recursive: true });
writeFileSync(path.join(SALIDA, 'cambios.json'), JSON.stringify(reporte, null, 2), 'utf8');
writeFileSync(path.join(SALIDA, 'cambios.md'), md, 'utf8');

console.log(
  `Cambios del ${DESDE} al ${HASTA}: ${reporte.semana.total.commits} commits; ` +
    `total del proyecto: ${reporte.total.total.commits} commits de ${reporte.total.integrantes.length} integrantes.`,
);
console.log(`Reporte en ${relativa(SALIDA)}/cambios.md`);
