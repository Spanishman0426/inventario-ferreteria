/**
 * Nombre: auditoriaService.js
 * Descripción: Servicio de consulta de la auditoría (RF-SEG-002).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/** Ruta base del recurso. */
const RUTA = '/api/v1/auditoria';

/**
 * Lista registros de auditoría con filtros y paginación.
 *
 * @param {object} [params] usuarioId, entidad, accion, desde, hasta, page, size
 * @returns {Promise<object>} página de registros
 */
export async function listarAuditoria(params = {}) {
  const { data } = await apiClient.get(RUTA, { params });
  return data;
}

/**
 * Consulta un registro de auditoría con sus valores antes y después.
 *
 * @param {number} id id del registro
 * @returns {Promise<object>} registro
 */
export async function obtenerRegistroAuditoria(id) {
  const { data } = await apiClient.get(`${RUTA}/${id}`);
  return data;
}
