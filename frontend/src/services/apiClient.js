/**
 * Nombre: apiClient.js
 * Descripción: Cliente Axios de la API: URL base desde VITE_API_URL, cabeceras Authorization y
 *              Accept-Language, cierre de sesión ante 401 y conversión de Problem Details en ApiError.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import axios from 'axios';

import i18n from '../i18n/index.js';
import { obtenerToken } from '../utils/sesionStorage.js';
import ApiError from './ApiError.js';

/** Ruta de inicio de sesión: su 401 significa credenciales inválidas, no sesión vencida. */
const RUTA_LOGIN = '/api/v1/auth/login';

/** Código de error que se asume cuando la respuesta no trae Problem Details. */
const CODIGO_POR_ESTADO = {
  400: 'SOLICITUD_INVALIDA',
  401: 'NO_AUTENTICADO',
  403: 'ACCESO_DENEGADO',
  404: 'NO_ENCONTRADO',
  405: 'METODO_NO_PERMITIDO',
  409: 'CONFLICTO_UNICIDAD',
  423: 'CUENTA_BLOQUEADA',
};

/** Función que la sesión registra para reaccionar a un 401; null mientras no haya ninguna. */
let manejadorNoAutorizado = null;

/**
 * Registra la función que se ejecuta cuando la API responde 401 fuera del login (token vencido o
 * revocado). La registra AuthProvider para cerrar la sesión y llevar al login.
 *
 * @param {(() => void)|null} manejador función a ejecutar, o null para quitarla
 * @returns {void}
 */
export function registrarManejadorNoAutorizado(manejador) {
  manejadorNoAutorizado = manejador;
}

/**
 * Convierte cualquier error de Axios en un {@link ApiError} legible. Si la respuesta es un
 * Problem Details usa su código, título, detalle y errores por campo; si no hubo respuesta,
 * devuelve un error de código RED.
 *
 * @param {unknown} error error lanzado por Axios
 * @returns {ApiError} error normalizado
 */
export function convertirError(error) {
  if (error instanceof ApiError) return error;
  const respuesta = /** @type {{response?: {status: number, data: unknown}}} */ (error)?.response;
  if (!respuesta) {
    return new ApiError({ status: 0, codigo: 'RED' });
  }
  const datos = /** @type {Record<string, unknown>} */ (
    respuesta.data && typeof respuesta.data === 'object' ? respuesta.data : {}
  );
  return new ApiError({
    status: respuesta.status,
    codigo: datos.codigo || CODIGO_POR_ESTADO[respuesta.status] || 'ERROR_INTERNO',
    titulo: datos.title,
    detalle: datos.detail,
    errores: Array.isArray(datos.errores) ? datos.errores : [],
    restriccion: datos.restriccion,
  });
}

/**
 * Indica si la URL de la solicitud es la de inicio de sesión.
 *
 * @param {string|undefined} url URL relativa o absoluta de la solicitud
 * @returns {boolean} verdadero si es el login
 */
function esLogin(url) {
  return typeof url === 'string' && url.endsWith(RUTA_LOGIN);
}

/** Instancia de Axios compartida por todos los servicios. */
const apiClient = axios.create({
  // Vacía en desarrollo: las rutas /api van al mismo origen y el proxy de Vite las reenvía.
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: { Accept: 'application/json, application/problem+json' },
  // Spring espera parámetros repetidos (sort=a&sort=b), sin corchetes.
  paramsSerializer: { indexes: null },
  timeout: 30000,
});

apiClient.interceptors.request.use((config) => {
  const token = obtenerToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['Accept-Language'] = i18n.resolvedLanguage || i18n.language || 'es';
  return config;
});

apiClient.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    const apiError = convertirError(error);
    if (apiError.status === 401 && !esLogin(error?.config?.url) && manejadorNoAutorizado) {
      manejadorNoAutorizado();
    }
    return Promise.reject(apiError);
  },
);

export default apiClient;
