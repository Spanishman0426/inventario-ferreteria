/**
 * Nombre: traducirError.js
 * Descripción: Traduce los mensajes de validación de Yup, que se guardan como claves de
 *              i18n para que cambien de idioma sin volver a validar.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */

/**
 * Mensaje de validación con parámetros de interpolación.
 *
 * @typedef {object} MensajeValidacion
 * @property {string} clave clave de i18n, por ejemplo 'validacion:maximo'
 * @property {Record<string, unknown>} [valores] valores para interpolar, por ejemplo { max: 100 }
 */

/**
 * Crea un mensaje de validación con parámetros, para usar como mensaje de Yup.
 *
 * @param {string} clave clave de i18n
 * @returns {(parametros: Record<string, unknown>) => MensajeValidacion} función de mensaje para Yup
 */
export function mensaje(clave) {
  return (parametros) => ({ clave, valores: parametros });
}

/**
 * Traduce un error de validación (clave simple o {@link MensajeValidacion}).
 *
 * @param {(clave: string, valores?: object) => string} t función de traducción de i18next
 * @param {string|MensajeValidacion|undefined} error error de Formik para un campo
 * @returns {string|undefined} texto traducido, o undefined si no hay error
 */
export function traducirError(t, error) {
  if (!error) return undefined;
  if (typeof error === 'string') return t(error);
  return t(error.clave, error.valores);
}
