/**
 * Nombre: productosService.js
 * Descripción: Servicio del catálogo de productos (RF-PRO-001, RF-PRO-003).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/** Ruta base del recurso. */
const RUTA = '/api/v1/productos';

/**
 * Filtros y paginación de la búsqueda de productos.
 *
 * @typedef {object} FiltroProductos
 * @property {string} [q] texto contenido en el nombre
 * @property {number} [categoriaId] id de la categoría
 * @property {'ACTIVO'|'INACTIVO'} [estado] estado del producto
 * @property {number} [page] página, desde 0
 * @property {number} [size] tamaño de página, de 1 a 100
 * @property {string[]} [sort] criterios "campo,asc|desc"
 */

/**
 * Página de resultados de la API.
 *
 * @template T
 * @typedef {object} Pagina
 * @property {T[]} contenido elementos de la página
 * @property {number} pagina número de página, desde 0
 * @property {number} tamano tamaño de página
 * @property {number} totalElementos total de elementos
 * @property {number} totalPaginas total de páginas
 */

/**
 * Busca productos con paginación.
 *
 * @param {FiltroProductos} [filtro] filtros y paginación
 * @returns {Promise<Pagina<object>>} página de productos
 */
export async function listarProductos(filtro = {}) {
  const params = Object.fromEntries(
    Object.entries(filtro).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  );
  const { data } = await apiClient.get(RUTA, { params });
  return data;
}

/**
 * Consulta un producto por id (también si está inactivo).
 *
 * @param {number} id id del producto
 * @returns {Promise<object>} producto
 */
export async function obtenerProducto(id) {
  const { data } = await apiClient.get(`${RUTA}/${id}`);
  return data;
}

/**
 * Registra un producto.
 *
 * @param {object} producto datos según ProductoPeticion del contrato OpenAPI
 * @returns {Promise<object>} producto creado
 */
export async function crearProducto(producto) {
  const { data } = await apiClient.post(RUTA, producto);
  return data;
}

/**
 * Reemplaza los datos de un producto.
 *
 * @param {number} id id del producto
 * @param {object} producto datos según ProductoPeticion del contrato OpenAPI
 * @returns {Promise<object>} producto actualizado
 */
export async function actualizarProducto(id, producto) {
  const { data } = await apiClient.put(`${RUTA}/${id}`, producto);
  return data;
}

/**
 * Inactiva un producto (borrado lógico, RF-CFG-001).
 *
 * @param {number} id id del producto
 * @returns {Promise<void>} se resuelve al inactivarlo
 */
export async function inactivarProducto(id) {
  await apiClient.delete(`${RUTA}/${id}`);
}
