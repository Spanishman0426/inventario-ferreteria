/**
 * Nombre: authService.js
 * Descripción: Servicio de autenticación: inicio de sesión y usuario actual (RF-SEG-001).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/**
 * Usuario autenticado con sus roles y permisos.
 *
 * @typedef {object} UsuarioSesion
 * @property {number} id identificador del usuario
 * @property {string} nombreCompleto nombre de la persona
 * @property {string[]} roles códigos de rol (ADMIN, VENDEDOR, …)
 * @property {string[]} permisos códigos de permiso (CATALOGO_CONSULTAR, COSTOS_VER, …)
 */

/**
 * Respuesta del inicio de sesión.
 *
 * @typedef {object} RespuestaInicioSesion
 * @property {string} accessToken token JWT
 * @property {number} expiresIn segundos de validez del token
 * @property {UsuarioSesion} usuario usuario autenticado
 */

/**
 * Inicia sesión. Rechaza con ApiError CREDENCIALES_INVALIDAS (401) o CUENTA_BLOQUEADA (423).
 *
 * @param {string} usuario nombre de usuario
 * @param {string} contrasena contraseña
 * @returns {Promise<RespuestaInicioSesion>} token y usuario
 */
export async function iniciarSesion(usuario, contrasena) {
  const { data } = await apiClient.post('/api/v1/auth/login', { usuario, contrasena });
  return data;
}

/**
 * Consulta el usuario autenticado con el token vigente (roles y permisos actuales).
 *
 * @returns {Promise<UsuarioSesion>} usuario autenticado
 */
export async function obtenerUsuarioActual() {
  const { data } = await apiClient.get('/api/v1/auth/me');
  return data;
}
