/**
 * Nombre: reporte-pendientes.mjs
 * Descripción: Reporte de pendientes del proyecto en Markdown y JSON: issues abiertos de GitHub agrupados por
 *              etiqueta, responsable y antigüedad; criterios de aceptación en FALLA o SIN PRUEBA según el último
 *              reporte de criterios (criterios.json de scripts/criterios.mjs); y marcas TODO/FIXME en el código
 *              con archivo y línea. Lo usa el workflow .github/workflows/reportes.yml y se puede ejecutar en local.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Uso (desde la raíz): node scripts/reporte-pendientes.mjs [--salida reportes/seguimiento]
 *                        [--criterios reportes/criterios/criterios.json] [--repo dueño/repositorio]
 * Requiere haber instalado la dependencia una vez: cd scripts && npm ci
 * Token de GitHub (opcional): GITHUB_TOKEN o GH_TOKEN; si no hay, se intenta "gh auth token". Sin token o sin
 * red, la sección de issues se marca como no disponible y el reporte se genera igual.
 * Escribe pendientes.md y pendientes.json en la carpeta de salida. Código de salida 0 salvo error inesperado.
 */
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';

import { RAIZ, celda, hoy, relativa } from './comun.mjs';

const { values: opciones } = parseArgs({
  options: {
    salida: { type: 'string', default: 'reportes/seguimiento' },
    criterios: { type: 'string' },
    repo: { type: 'string' },
  },
});
const SALIDA = path.resolve(RAIZ, opciones.salida);
// Por defecto el de la raíz; una ruta indicada se toma relativa a donde se ejecutó el comando (INIT_CWD con npm run)
opciones.criterios ??= 'reportes/criterios/criterios.json';
const ARCHIVO_CRITERIOS = process.argv.some((a) => a.startsWith('--criterios'))
  ? path.resolve(process.env.INIT_CWD ?? process.cwd(), opciones.criterios)
  : path.resolve(RAIZ, opciones.criterios);

/** Etiqueta del issue fijo del reporte; ese issue no cuenta como pendiente. */
const ETIQUETA_REPORTE = 'reporte-semanal';

/** Rangos de antigüedad de los issues, en días (límite superior incluido). */
const RANGOS_ANTIGUEDAD = [
  { nombre: 'Menos de 7 días', hasta: 6 },
  { nombre: '7 a 30 días', hasta: 30 },
  { nombre: '31 a 90 días', hasta: 90 },
  { nombre: 'Más de 90 días', hasta: Infinity },
];

/** Máximo de números de issue que se listan por fila en las tablas de conteo. */
const MAXIMO_NUMEROS = 20;

/** Extensiones de archivos de código y configuración donde se buscan TODO/FIXME. */
const EXTENSIONES_CODIGO = new Set([
  '.java', '.js', '.jsx', '.mjs', '.cjs', '.ts', '.tsx', '.sql', '.yml', '.yaml', '.properties', '.xml',
  '.html', '.css', '.sh', '.ps1', '.py', '.cmd',
]);

/** Marca TODO o FIXME dentro de un comentario (evita contar identificadores como TODOS o BORRAR_TODO). */
const PATRON_MARCA = /(?:\/\/|\/\*|^\s*\*|#|<!--|--)\s*(TODO|FIXME)\b[:\s-]*(.*)$/;

/** Este mismo archivo, que menciona las marcas en su documentación. */
const ESTE_ARCHIVO = 'scripts/reporte-pendientes.mjs';

// ------------------------------------------------------------------ utilidades

/**
 * Ejecuta git en la raíz del repositorio y devuelve su salida.
 *
 * @param {string[]} argumentos argumentos de git
 * @returns {string} salida estándar
 */
function git(argumentos) {
  return execFileSync('git', argumentos, { cwd: RAIZ, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
}

/**
 * Repositorio de GitHub "dueño/nombre": opción --repo, GITHUB_REPOSITORY o la URL del remoto origin.
 *
 * @returns {string|null} repositorio o null si no se puede deducir
 */
function repositorio() {
  if (opciones.repo) return opciones.repo;
  if (process.env.GITHUB_REPOSITORY) return process.env.GITHUB_REPOSITORY;
  try {
    const url = git(['remote', 'get-url', 'origin']).trim();
    const coincidencia = url.match(/github\.com[:/]([^/]+\/[^/]+?)(?:\.git)?$/);
    return coincidencia ? coincidencia[1] : null;
  } catch {
    return null;
  }
}

/**
 * Token para la API de GitHub: GITHUB_TOKEN, GH_TOKEN o el de la CLI gh si está autenticada.
 *
 * @returns {string|null} token o null
 */
function token() {
  const deEntorno = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
  if (deEntorno) return deEntorno;
  try {
    return execFileSync('gh', ['auth', 'token'], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim() || null;
  } catch {
    return null;
  }
}

/**
 * Días completos transcurridos desde una fecha ISO hasta ahora.
 *
 * @param {string} fechaIso fecha de creación
 * @returns {number} días
 */
function diasDesde(fechaIso) {
  return Math.floor((Date.now() - new Date(fechaIso).getTime()) / 86_400_000);
}

/**
 * Cuenta elementos por clave; un elemento puede aportar a varias claves (por ejemplo, varias etiquetas).
 *
 * @template T
 * @param {T[]} elementos elementos
 * @param {(e: T) => string[]} claves claves de cada elemento
 * @returns {Map<string, T[]>} clave → elementos, ordenado por cantidad descendente y luego por nombre
 */
function agrupar(elementos, claves) {
  const grupos = new Map();
  for (const e of elementos) {
    for (const clave of claves(e)) {
      if (!grupos.has(clave)) grupos.set(clave, []);
      grupos.get(clave).push(e);
    }
  }
  return new Map([...grupos].sort((a, b) => b[1].length - a[1].length || a[0].localeCompare(b[0])));
}

// ------------------------------------------------------------------ issues

/**
 * Issue abierto simplificado.
 *
 * @typedef {object} Issue
 * @property {number} numero número del issue
 * @property {string} titulo título
 * @property {string} url enlace
 * @property {string[]} etiquetas nombres de las etiquetas
 * @property {string[]} responsables usuarios asignados
 * @property {string} creado fecha de creación ISO
 * @property {number} dias antigüedad en días
 */

/**
 * Lee los issues abiertos del repositorio (sin pull requests ni el issue del reporte semanal).
 *
 * @param {string} repo dueño/nombre
 * @param {string} credencial token de GitHub
 * @returns {Promise<Issue[]>} issues ordenados del más antiguo al más reciente
 * @throws {Error} si la API responde con error
 */
async function leerIssues(repo, credencial) {
  const issues = [];
  for (let pagina = 1; ; pagina++) {
    const respuesta = await fetch(
      `https://api.github.com/repos/${repo}/issues?state=open&per_page=100&page=${pagina}`,
      {
        headers: {
          Accept: 'application/vnd.github+json',
          Authorization: `Bearer ${credencial}`,
          'X-GitHub-Api-Version': '2022-11-28',
          'User-Agent': 'inventario-ferreteria-reportes',
        },
      },
    );
    if (!respuesta.ok) throw new Error(`GitHub respondió ${respuesta.status} ${respuesta.statusText}`);
    const datos = await respuesta.json();
    for (const i of datos) {
      if (i.pull_request) continue;
      const etiquetas = i.labels.map((l) => (typeof l === 'string' ? l : l.name));
      if (etiquetas.includes(ETIQUETA_REPORTE)) continue;
      issues.push({
        numero: i.number,
        titulo: i.title,
        url: i.html_url,
        etiquetas,
        responsables: (i.assignees ?? []).map((a) => a.login),
        creado: i.created_at,
        dias: diasDesde(i.created_at),
      });
    }
    if (datos.length < 100) break;
  }
  return issues.sort((a, b) => b.dias - a.dias || a.numero - b.numero);
}

/**
 * Obtiene los issues o el motivo por el que no están disponibles.
 *
 * @returns {Promise<{disponible: boolean, motivo?: string, repo?: string, issues: Issue[]}>} resultado
 */
async function seccionIssues() {
  const repo = repositorio();
  if (!repo) return { disponible: false, motivo: 'no se pudo deducir el repositorio de GitHub (usa --repo).', issues: [] };
  const credencial = token();
  if (!credencial) {
    return {
      disponible: false,
      repo,
      motivo: 'no hay token de GitHub (define GITHUB_TOKEN o GH_TOKEN, o inicia sesión con "gh auth login").',
      issues: [],
    };
  }
  try {
    return { disponible: true, repo, issues: await leerIssues(repo, credencial) };
  } catch (error) {
    return { disponible: false, repo, motivo: `no se pudo consultar la API de GitHub: ${error.message}.`, issues: [] };
  }
}

// ------------------------------------------------------------------ criterios

/**
 * Criterios en FALLA o SIN PRUEBA del último reporte de criterios.
 *
 * @returns {{disponible: boolean, archivo: string, motivo?: string, generado?: string, total?: object,
 *   pendientes: {id: string, requisito: string, descripcion: string, estado: string, detalle: string}[]}} resultado
 */
function seccionCriterios() {
  const archivo = existsSync(ARCHIVO_CRITERIOS) ? relativa(ARCHIVO_CRITERIOS) : opciones.criterios;
  if (!existsSync(ARCHIVO_CRITERIOS)) {
    return {
      disponible: false,
      archivo,
      motivo: `no existe ${opciones.criterios}; genéralo con "node scripts/criterios.mjs" o descarga el artefacto criterios de la CI.`,
      pendientes: [],
    };
  }
  try {
    const reporte = JSON.parse(readFileSync(ARCHIVO_CRITERIOS, 'utf8'));
    const pendientes = (reporte.criterios ?? [])
      .filter((c) => c.estado === 'FALLA' || c.estado === 'SIN PRUEBA')
      .map((c) => ({
        id: c.id,
        requisito: c.requisito,
        descripcion: c.descripcion,
        estado: c.estado,
        detalle: (c.fallas ?? []).map((f) => `${f.fuente}: ${f.nombre}${f.mensaje ? ` — ${f.mensaje}` : ''}`).join('<br>'),
      }))
      .sort((a, b) => (a.estado === b.estado ? a.id.localeCompare(b.id) : a.estado === 'FALLA' ? -1 : 1));
    return { disponible: true, archivo, generado: reporte.generado, total: reporte.total, pendientes };
  } catch (error) {
    return { disponible: false, archivo, motivo: `no se pudo leer: ${error.message}.`, pendientes: [] };
  }
}

// ------------------------------------------------------------------ TODO / FIXME

/**
 * Busca marcas TODO y FIXME en comentarios de los archivos de código versionados.
 *
 * @returns {{archivo: string, linea: number, marca: string, texto: string}[]} marcas encontradas
 */
function seccionMarcas() {
  const archivos = git(['ls-files', '-z']).split('\0').filter(Boolean);
  const marcas = [];
  for (const archivo of archivos) {
    if (archivo === ESTE_ARCHIVO || !EXTENSIONES_CODIGO.has(path.extname(archivo).toLowerCase())) continue;
    const ruta = path.join(RAIZ, archivo);
    if (!existsSync(ruta)) continue;
    const lineas = readFileSync(ruta, 'utf8').split(/\r?\n/);
    lineas.forEach((linea, indice) => {
      const coincidencia = linea.match(PATRON_MARCA);
      if (coincidencia) {
        marcas.push({
          archivo,
          linea: indice + 1,
          marca: coincidencia[1],
          texto: coincidencia[2].replace(/\s*(\*\/|-->)\s*$/, '').trim(),
        });
      }
    });
  }
  return marcas;
}

// ------------------------------------------------------------------ Markdown

/**
 * Números de issue para una celda, recortados a MAXIMO_NUMEROS.
 *
 * @param {Issue[]} lista issues
 * @returns {string} "#1, #2, … y N más"
 */
function numeros(lista) {
  const visibles = lista.slice(0, MAXIMO_NUMEROS).map((i) => `#${i.numero}`).join(', ');
  return lista.length > MAXIMO_NUMEROS ? `${visibles} y ${lista.length - MAXIMO_NUMEROS} más` : visibles;
}

/**
 * Tabla Markdown de conteos de un agrupamiento.
 *
 * @param {string} titulo encabezado de la primera columna
 * @param {Map<string, Issue[]>} grupos grupos de issues
 * @returns {string[]} líneas
 */
function tablaConteo(titulo, grupos) {
  return [
    `| ${titulo} | Issues | Números |`,
    '| --- | --- | --- |',
    ...[...grupos].map(([clave, lista]) => `| ${celda(clave)} | ${lista.length} | ${numeros(lista)} |`),
  ];
}

/**
 * Arma el reporte completo en Markdown.
 *
 * @param {object} reporte datos del reporte
 * @returns {string} Markdown
 */
function markdown(reporte) {
  const { issues, criterios, marcas } = reporte;
  const lineas = [
    `# Reporte de pendientes — ${reporte.fecha}`,
    '',
    '| Pendiente | Cantidad |',
    '| --- | --- |',
    `| Issues abiertos | ${issues.disponible ? issues.issues.length : 'no disponible'} |`,
    `| Criterios en FALLA | ${criterios.disponible ? criterios.pendientes.filter((c) => c.estado === 'FALLA').length : 'no disponible'} |`,
    `| Criterios SIN PRUEBA | ${criterios.disponible ? criterios.pendientes.filter((c) => c.estado === 'SIN PRUEBA').length : 'no disponible'} |`,
    `| Marcas TODO/FIXME | ${marcas.length} |`,
    '',
    '## Issues abiertos',
    '',
  ];

  if (!issues.disponible) {
    lineas.push(`No disponible: ${issues.motivo}`);
  } else if (issues.issues.length === 0) {
    lineas.push(`No hay issues abiertos en ${issues.repo}.`);
  } else {
    const lista = issues.issues;
    const porAntiguedad = new Map(RANGOS_ANTIGUEDAD.map((r) => [r.nombre, []]));
    for (const i of lista) porAntiguedad.get(RANGOS_ANTIGUEDAD.find((r) => i.dias <= r.hasta).nombre).push(i);
    lineas.push(
      `${lista.length} issues abiertos en ${issues.repo} (sin contar pull requests ni el issue del reporte semanal).`,
      '',
      '### Por etiqueta',
      '',
      ...tablaConteo('Etiqueta', agrupar(lista, (i) => (i.etiquetas.length ? i.etiquetas : ['(sin etiqueta)']))),
      '',
      '### Por responsable',
      '',
      ...tablaConteo('Responsable', agrupar(lista, (i) => (i.responsables.length ? i.responsables.map((r) => `@${r}`) : ['(sin asignar)']))),
      '',
      '### Por antigüedad',
      '',
      '| Antigüedad | Issues | Números |',
      '| --- | --- | --- |',
      ...[...porAntiguedad].map(([nombre, l]) => `| ${nombre} | ${l.length} | ${numeros(l)} |`),
      '',
      '### Lista',
      '',
      '| Issue | Título | Etiquetas | Responsables | Días abierto |',
      '| --- | --- | --- | --- | --- |',
      ...lista.map(
        (i) =>
          `| [#${i.numero}](${i.url}) | ${celda(i.titulo)} | ${celda(i.etiquetas.join(', ') || '—')} | ` +
          `${celda(i.responsables.map((r) => `@${r}`).join(', ') || '—')} | ${i.dias} |`,
      ),
    );
  }

  lineas.push('', '## Criterios de aceptación pendientes', '');
  if (!criterios.disponible) {
    lineas.push(`No disponible: ${criterios.motivo}`);
  } else {
    const t = criterios.total ?? {};
    lineas.push(
      `Fuente: \`${criterios.archivo}\`${criterios.generado ? `, generado ${criterios.generado}` : ''}. ` +
        `Cumplimiento: **${t.porcentaje ?? '?'} %** (${t.cumple ?? '?'} de ${t.total ?? '?'} cumplen, ` +
        `${t.falla ?? '?'} fallan, ${t.sinPrueba ?? '?'} sin prueba).`,
      '',
    );
    if (criterios.pendientes.length === 0) {
      lineas.push('Todos los criterios cumplen.');
    } else {
      lineas.push(
        '| Criterio | Requisito | Estado | Descripción | Detalle |',
        '| --- | --- | --- | --- | --- |',
        ...criterios.pendientes.map(
          (c) =>
            `| ${c.id} | ${c.requisito} | ${c.estado === 'FALLA' ? '❌ FALLA' : '⚪ SIN PRUEBA'} | ` +
            `${celda(c.descripcion)} | ${celda(c.detalle) || '—'} |`,
        ),
      );
    }
  }

  lineas.push('', '## Marcas TODO y FIXME en el código', '');
  if (marcas.length === 0) {
    lineas.push('No hay marcas TODO ni FIXME en los archivos de código versionados.');
  } else {
    lineas.push(
      '| Archivo | Línea | Marca | Texto |',
      '| --- | --- | --- | --- |',
      ...marcas.map((m) => `| \`${m.archivo}\` | ${m.linea} | ${m.marca} | ${celda(m.texto) || '—'} |`),
    );
  }
  return `${lineas.join('\n')}\n`;
}

// ------------------------------------------------------------------ principal

const reporte = {
  fecha: hoy(),
  issues: await seccionIssues(),
  criterios: seccionCriterios(),
  marcas: seccionMarcas(),
};

mkdirSync(SALIDA, { recursive: true });
writeFileSync(path.join(SALIDA, 'pendientes.json'), JSON.stringify(reporte, null, 2), 'utf8');
writeFileSync(path.join(SALIDA, 'pendientes.md'), markdown(reporte), 'utf8');

console.log(
  `Pendientes: ${reporte.issues.disponible ? reporte.issues.issues.length : 'sin datos de'} issues, ` +
    `${reporte.criterios.pendientes.length} criterios en FALLA o SIN PRUEBA, ${reporte.marcas.length} marcas TODO/FIXME.`,
);
console.log(`Reporte en ${relativa(SALIDA)}/pendientes.md`);
