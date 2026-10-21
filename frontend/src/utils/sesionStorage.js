/**
 * Nombre: sesionStorage.js
 * Descripción: Guarda, lee y borra el token JWT y su vencimiento en sessionStorage.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 *
 * Por qué sessionStorage y no localStorage:
 * - sessionStorage se borra al cerrar la pestaña; localStorage persiste indefinidamente, de modo
 *   que un token olvidado en un equipo compartido del mostrador seguiría siendo válido.
 * - sessionStorage no se comparte entre pestañas ni ventanas: cerrar sesión en una no deja otra
 *   sesión viva escondida.
 * - El token tiene vencimiento corto (RNF-SEG-006); persistirlo más allá de la pestaña no aporta
 *   nada y solo alarga la ventana en que podría robarse.
 * Ninguno de los dos almacenamientos protege contra un XSS en la misma pestaña; esa defensa
 * está en no insertar HTML sin escapar (React lo escapa por defecto) y en las cabeceras del
 * backend. El costo es que el usuario debe iniciar sesión de nuevo al abrir otra pestaña, lo que
 * es aceptable para un sistema de mostrador.
 */

/** Clave del token en sessionStorage. */
const CLAVE_TOKEN = 'ferreteria.token';

/** Clave del instante de vencimiento (milisegundos desde época) en sessionStorage. */
const CLAVE_EXPIRA = 'ferreteria.tokenExpira';

/**
 * Sesión guardada en el navegador.
 *
 * @typedef {object} SesionGuardada
 * @property {string} token token JWT de acceso
 * @property {number} expiraEn instante de vencimiento en milisegundos desde época
 */

/**
 * Guarda el token y calcula su vencimiento.
 *
 * @param {string} token token JWT emitido por /api/v1/auth/login
 * @param {number} expiresInSegundos segundos de validez informados por la API
 * @param {number} [ahora] instante actual en milisegundos (inyectable en pruebas)
 * @returns {SesionGuardada} sesión guardada
 */
export function guardarSesion(token, expiresInSegundos, ahora = Date.now()) {
  const expiraEn = ahora + Number(expiresInSegundos) * 1000;
  sessionStorage.setItem(CLAVE_TOKEN, token);
  sessionStorage.setItem(CLAVE_EXPIRA, String(expiraEn));
  return { token, expiraEn };
}

/**
 * Lee la sesión guardada si existe y no ha vencido; si venció, la borra.
 *
 * @param {number} [ahora] instante actual en milisegundos (inyectable en pruebas)
 * @returns {SesionGuardada|null} sesión vigente o null
 */
export function leerSesion(ahora = Date.now()) {
  const token = sessionStorage.getItem(CLAVE_TOKEN);
  const expiraEn = Number(sessionStorage.getItem(CLAVE_EXPIRA));
  if (!token || !Number.isFinite(expiraEn) || expiraEn <= ahora) {
    borrarSesion();
    return null;
  }
  return { token, expiraEn };
}

/**
 * Devuelve el token vigente, o null si no hay sesión.
 *
 * @returns {string|null} token JWT
 */
export function obtenerToken() {
  return leerSesion()?.token ?? null;
}

/**
 * Borra el token y su vencimiento.
 *
 * @returns {void}
 */
export function borrarSesion() {
  sessionStorage.removeItem(CLAVE_TOKEN);
  sessionStorage.removeItem(CLAVE_EXPIRA);
}
