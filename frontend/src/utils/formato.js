/**
 * Nombre: formato.js
 * Descripción: Formatos colombianos para la interfaz (RNF-INT-003): moneda COP sin decimales
 *              ($ 12.350) y fechas dd/mm/aaaa en la zona horaria America/Bogota.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */

/** Zona horaria de la operación (RNF-INT-003). */
export const ZONA_HORARIA = 'America/Bogota';

const formatoMoneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', {
  timeZone: ZONA_HORARIA,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

/** Fecha sin hora, como la envía la API para campos LocalDate (AAAA-MM-DD). */
const PATRON_FECHA_SIMPLE = /^(\d{4})-(\d{2})-(\d{2})$/;

/**
 * Formatea un valor en pesos colombianos, sin decimales (RN-06).
 *
 * @param {number|string|null|undefined} valor monto en COP
 * @returns {string} por ejemplo "$ 12.350"; cadena vacía si el valor no es numérico
 */
export function formatearMoneda(valor) {
  if (valor === null || valor === undefined || valor === '' || Number.isNaN(Number(valor))) {
    return '';
  }
  return formatoMoneda.format(Number(valor));
}

/**
 * Obtiene las partes de fecha y hora de un instante en la hora de Colombia.
 *
 * @param {Date} fecha instante a descomponer
 * @returns {Record<string, string>} partes day, month, year, hour y minute
 */
function partesBogota(fecha) {
  return Object.fromEntries(
    formatoFechaHora.formatToParts(fecha).map((parte) => [parte.type, parte.value]),
  );
}

/**
 * Convierte la entrada en Date si es válida.
 *
 * @param {Date|string|number|null|undefined} valor fecha a convertir
 * @returns {Date|null} fecha válida o null
 */
function aFecha(valor) {
  if (valor === null || valor === undefined || valor === '') return null;
  const fecha = valor instanceof Date ? valor : new Date(valor);
  return Number.isNaN(fecha.getTime()) ? null : fecha;
}

/**
 * Formatea una fecha como dd/mm/aaaa en la hora de Colombia. Una fecha simple AAAA-MM-DD se
 * reordena sin convertir de zona, para que no cambie de día.
 *
 * @param {Date|string|number|null|undefined} valor fecha, instante ISO o milisegundos
 * @returns {string} por ejemplo "20/10/2026"; cadena vacía si no es una fecha válida
 */
export function formatearFecha(valor) {
  const simple = typeof valor === 'string' ? PATRON_FECHA_SIMPLE.exec(valor) : null;
  if (simple) return `${simple[3]}/${simple[2]}/${simple[1]}`;
  const fecha = aFecha(valor);
  if (!fecha) return '';
  const p = partesBogota(fecha);
  return `${p.day}/${p.month}/${p.year}`;
}

/**
 * Formatea un instante como dd/mm/aaaa hh:mm (24 horas) en la hora de Colombia.
 *
 * @param {Date|string|number|null|undefined} valor instante ISO, Date o milisegundos
 * @returns {string} por ejemplo "20/10/2026 10:30"; cadena vacía si no es válido
 */
export function formatearFechaHora(valor) {
  const fecha = aFecha(valor);
  if (!fecha) return '';
  const p = partesBogota(fecha);
  return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}`;
}
