/**
 * Nombre: ApiError.js
 * Descripción: Error legible construido a partir de una respuesta Problem Details (RFC 9457) de la API.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import i18n from '../i18n/index.js';

/**
 * Error de un campo informado por la API en la propiedad `errores`.
 *
 * @typedef {object} ErrorCampo
 * @property {string} campo nombre del campo
 * @property {string} mensaje causa traducida por el backend
 */

/**
 * Datos para construir un {@link ApiError}.
 *
 * @typedef {object} DatosApiError
 * @property {number} status estado HTTP (0 si no hubo respuesta)
 * @property {string} codigo código estable de la API (VALIDACION, CUENTA_BLOQUEADA, …) o RED
 * @property {string} [titulo] title del Problem Details, traducido según Accept-Language
 * @property {string} [detalle] detail del Problem Details, traducido según Accept-Language
 * @property {ErrorCampo[]} [errores] errores por campo
 * @property {string} [restriccion] restricción de la base de datos violada, si aplica
 */

/**
 * Error de la API con código estable, mensajes del servidor y errores por campo. El mensaje
 * (`message`) prefiere el detalle del servidor, luego su título y por último el texto del
 * catálogo de errores del frontend para el código.
 */
export default class ApiError extends Error {
  /**
   * Crea el error.
   *
   * @param {DatosApiError} datos datos del error
   */
  constructor({ status, codigo, titulo, detalle, errores = [], restriccion }) {
    const codigoFinal = codigo || 'DESCONOCIDO';
    const respaldo = i18n.t(`errores:${codigoFinal}`, {
      defaultValue: i18n.t('errores:DESCONOCIDO'),
    });
    super(detalle || titulo || respaldo);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigoFinal;
    this.titulo = titulo || respaldo;
    this.detalle = detalle || '';
    this.errores = errores;
    this.restriccion = restriccion;
  }

  /**
   * Agrupa los errores por campo, útil para pasarlos a Formik con setErrors.
   *
   * @returns {Record<string, string>} mensaje por nombre de campo
   */
  erroresPorCampo() {
    return Object.fromEntries(this.errores.map((e) => [e.campo, e.mensaje]));
  }
}
