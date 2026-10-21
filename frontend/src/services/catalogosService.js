/**
 * Nombre: catalogosService.js
 * Descripción: Servicio de catálogos de referencia para selectores: marcas, categorías,
 *              unidades de medida y tarifas de IVA.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/**
 * Lista las marcas activas, ordenadas por nombre.
 *
 * @returns {Promise<{id: number, nombre: string}[]>} marcas
 */
export async function listarMarcas() {
  const { data } = await apiClient.get('/api/v1/marcas');
  return data;
}

/**
 * Lista las categorías activas, ordenadas por nombre.
 *
 * @returns {Promise<{id: number, nombre: string}[]>} categorías
 */
export async function listarCategorias() {
  const { data } = await apiClient.get('/api/v1/categorias');
  return data;
}

/**
 * Lista las unidades de medida (RN-05).
 *
 * @returns {Promise<{codigo: string, nombre: string, esDiscreta: boolean}[]>} unidades
 */
export async function listarUnidadesMedida() {
  const { data } = await apiClient.get('/api/v1/unidades-medida');
  return data;
}

/**
 * Lista las tarifas de IVA activas (RN-08).
 *
 * @returns {Promise<{id: number, nombre: string, porcentaje: number}[]>} tarifas
 */
export async function listarTarifasIva() {
  const { data } = await apiClient.get('/api/v1/tarifas-iva');
  return data;
}
