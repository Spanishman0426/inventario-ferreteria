/**
 * Nombre: catalogosService.js
 * Descripción: Servicio de catálogos: listas de activos para los selectores (marcas, categorías,
 *              unidades de medida y tarifas de IVA) y administración de marcas y categorías
 *              (búsqueda, alta, edición y cambio de estado, RF-CFG-001).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/** Catálogos administrables y su ruta en la API. */
export const TIPOS_CATALOGO = ['marcas', 'categorias'];

/**
 * Catálogo administrable: 'marcas' o 'categorias'.
 *
 * @typedef {'marcas'|'categorias'} TipoCatalogo
 */

/**
 * Marca o categoría con su estado.
 *
 * @typedef {object} ElementoCatalogo
 * @property {number} id identificador
 * @property {string} nombre nombre
 * @property {'ACTIVO'|'INACTIVO'} estado estado
 */

/**
 * Filtros y paginación de la búsqueda de un catálogo.
 *
 * @typedef {object} FiltroCatalogo
 * @property {string} [q] texto contenido en el nombre
 * @property {'ACTIVO'|'INACTIVO'} [estado] estado; sin valor devuelve ambos
 * @property {number} [page] página, desde 0
 * @property {number} [size] tamaño de página, de 1 a 100
 */

/**
 * Ruta del recurso de un catálogo; rechaza tipos desconocidos para no armar URL arbitrarias.
 *
 * @param {TipoCatalogo} tipo catálogo
 * @returns {string} ruta base
 * @throws {Error} si el tipo no es un catálogo administrable
 */
function ruta(tipo) {
  if (!TIPOS_CATALOGO.includes(tipo)) {
    throw new Error(`Catálogo desconocido: ${tipo}`);
  }
  return `/api/v1/${tipo}`;
}

/**
 * Lista las marcas activas, ordenadas por nombre (selector del formulario de producto).
 *
 * @returns {Promise<{id: number, nombre: string}[]>} marcas
 */
export async function listarMarcas() {
  const { data } = await apiClient.get('/api/v1/marcas/activas');
  return data;
}

/**
 * Lista las categorías activas, ordenadas por nombre (selectores y filtros).
 *
 * @returns {Promise<{id: number, nombre: string}[]>} categorías
 */
export async function listarCategorias() {
  const { data } = await apiClient.get('/api/v1/categorias/activas');
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

/**
 * Busca elementos de un catálogo con paginación, activos e inactivos.
 *
 * @param {TipoCatalogo} tipo catálogo
 * @param {FiltroCatalogo} [filtro] filtros y paginación
 * @returns {Promise<import('./productosService.js').Pagina<ElementoCatalogo>>} página de elementos
 */
export async function buscarElementosCatalogo(tipo, filtro = {}) {
  const params = Object.fromEntries(
    Object.entries(filtro).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  );
  const { data } = await apiClient.get(ruta(tipo), { params });
  return data;
}

/**
 * Registra un elemento activo (requiere CONFIG_GESTIONAR).
 *
 * @param {TipoCatalogo} tipo catálogo
 * @param {{nombre: string}} datos nombre del elemento
 * @returns {Promise<ElementoCatalogo>} elemento creado
 */
export async function crearElementoCatalogo(tipo, datos) {
  const { data } = await apiClient.post(ruta(tipo), datos);
  return data;
}

/**
 * Cambia el nombre de un elemento (requiere CONFIG_GESTIONAR).
 *
 * @param {TipoCatalogo} tipo catálogo
 * @param {number} id id del elemento
 * @param {{nombre: string}} datos nombre nuevo
 * @returns {Promise<ElementoCatalogo>} elemento actualizado
 */
export async function actualizarElementoCatalogo(tipo, id, datos) {
  const { data } = await apiClient.put(`${ruta(tipo)}/${id}`, datos);
  return data;
}

/**
 * Activa o desactiva un elemento; no existe borrado (RF-CFG-001).
 *
 * @param {TipoCatalogo} tipo catálogo
 * @param {number} id id del elemento
 * @param {'ACTIVO'|'INACTIVO'} estado estado nuevo
 * @returns {Promise<ElementoCatalogo>} elemento con su estado
 */
export async function cambiarEstadoElementoCatalogo(tipo, id, estado) {
  const { data } = await apiClient.patch(`${ruta(tipo)}/${id}/estado`, { estado });
  return data;
}
