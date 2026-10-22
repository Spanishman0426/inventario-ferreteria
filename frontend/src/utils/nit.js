/**
 * Nombre: nit.js
 * Descripción: Dígito de verificación del NIT colombiano, con el mismo algoritmo de fn_dv_nit (base de
 *              datos) y de DigitoVerificacionNit (backend), para calcularlo en vivo en el formulario.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */

/** Pesos de la DIAN, del dígito menos significativo al más significativo. */
const PESOS = [3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71];

/** Entre 1 y 15 dígitos, igual que fn_dv_nit. */
const NUMERO = /^[0-9]{1,15}$/;

/**
 * Calcula el dígito de verificación de un NIT (RN-26): cada dígito, de derecha a izquierda, se
 * multiplica por su peso; el residuo de la suma entre 11 es el dígito si vale 0 o 1, y en otro caso el
 * dígito es 11 menos el residuo.
 *
 * @param {string|null|undefined} numero NIT sin dígito de verificación ni separadores
 * @returns {number|null} dígito de 0 a 9, o null si el número no tiene entre 1 y 15 dígitos
 */
export function calcularDigitoVerificacion(numero) {
  if (typeof numero !== 'string' || !NUMERO.test(numero)) return null;
  let suma = 0;
  for (let i = 0; i < numero.length; i += 1) {
    suma += Number(numero[numero.length - 1 - i]) * PESOS[i];
  }
  const residuo = suma % 11;
  return residuo <= 1 ? residuo : 11 - residuo;
}

/**
 * Indica si el dígito corresponde al NIT.
 *
 * @param {string|null|undefined} numero NIT sin dígito de verificación
 * @param {number|string|null|undefined} digito dígito a comprobar
 * @returns {boolean} verdadero si el número es válido y el dígito coincide
 */
export function esDigitoValido(numero, digito) {
  const esperado = calcularDigitoVerificacion(numero);
  if (esperado === null || digito === null || digito === undefined || digito === '') return false;
  return Number(digito) === esperado;
}
