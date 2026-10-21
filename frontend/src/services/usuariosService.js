/**
 * Nombre: usuariosService.js
 * Descripción: Servicio de gestión de usuarios (RF-SEG-001); requiere USUARIOS_GESTIONAR.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/** Ruta base del recurso. */
const RUTA = '/api/v1/usuarios';

/**
 * Lista usuarios con paginación.
 *
 * @param {object} [params] filtros y paginación (page, size, sort)
 * @returns {Promise<object>} página de usuarios
 */
export async function listarUsuarios(params = {}) {
  const { data } = await apiClient.get(RUTA, { params });
  return data;
}

/**
 * Consulta un usuario.
 *
 * @param {number} id id del usuario
 * @returns {Promise<object>} usuario
 */
export async function obtenerUsuario(id) {
  const { data } = await apiClient.get(`${RUTA}/${id}`);
  return data;
}

/**
 * Crea un usuario.
 *
 * @param {object} usuario datos según UsuarioCreacion del contrato OpenAPI
 * @returns {Promise<object>} usuario creado
 */
export async function crearUsuario(usuario) {
  const { data } = await apiClient.post(RUTA, usuario);
  return data;
}

/**
 * Actualiza nombre y correo de un usuario.
 *
 * @param {number} id id del usuario
 * @param {{nombreCompleto: string, correo: string}} datos datos nuevos
 * @returns {Promise<object>} usuario actualizado
 */
export async function actualizarUsuario(id, datos) {
  const { data } = await apiClient.put(`${RUTA}/${id}`, datos);
  return data;
}

/**
 * Cambia la contraseña de un usuario; también levanta un bloqueo vigente.
 *
 * @param {number} id id del usuario
 * @param {string} contrasena contraseña nueva
 * @returns {Promise<void>} se resuelve al cambiarla
 */
export async function cambiarContrasena(id, contrasena) {
  await apiClient.put(`${RUTA}/${id}/contrasena`, { contrasena });
}

/**
 * Activa o desactiva un usuario (los usuarios no se eliminan).
 *
 * @param {number} id id del usuario
 * @param {boolean} activo estado nuevo
 * @returns {Promise<object>} usuario actualizado
 */
export async function cambiarEstadoUsuario(id, activo) {
  const { data } = await apiClient.patch(`${RUTA}/${id}/estado`, { activo });
  return data;
}

/**
 * Reemplaza los roles de un usuario.
 *
 * @param {number} id id del usuario
 * @param {string[]} roles códigos de rol (al menos uno)
 * @returns {Promise<object>} usuario actualizado
 */
export async function asignarRoles(id, roles) {
  const { data } = await apiClient.put(`${RUTA}/${id}/roles`, { roles });
  return data;
}
