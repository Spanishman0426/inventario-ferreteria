/**
 * Nombre: rolesService.js
 * Descripción: Servicio de roles con sus permisos (matriz 2.2, RN-27).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/**
 * Lista los roles con sus permisos.
 *
 * @returns {Promise<{codigo: string, nombre: string, permisos: string[]}[]>} roles
 */
export async function listarRoles() {
  const { data } = await apiClient.get('/api/v1/roles');
  return data;
}
