/**
 * Nombre: tercerosService.js
 * Descripción: Llamadas a la API de clientes y proveedores (/api/v1/terceros, RF-TER-001).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import apiClient from './apiClient.js';

/** Permiso que exige registrar o editar un cliente (matriz 2.2). */
export const PERMISO_CLIENTES = 'TERCERO_CLIENTE_EDITAR';

/** Permiso que exige registrar o editar un proveedor (matriz 2.2). */
export const PERMISO_PROVEEDORES = 'TERCERO_PROVEEDOR_EDITAR';

/** Permisos de los que basta uno para abrir la página de terceros. */
export const PERMISOS_TERCEROS = [PERMISO_CLIENTES, PERMISO_PROVEEDORES];

/** Ruta base de la API de terceros. */
const RUTA = '/api/v1/terceros';

/**
 * Tercero tal como lo devuelve la API.
 *
 * @typedef {object} Tercero
 * @property {number} id identificador
 * @property {'NATURAL'|'JURIDICA'} tipoPersona tipo de persona
 * @property {'CC'|'NIT'} tipoIdentificacion tipo de identificación
 * @property {string} numeroIdentificacion número sin dígito de verificación
 * @property {number|null} digitoVerificacion dígito del NIT
 * @property {string|null} nombre nombre (persona natural)
 * @property {string|null} apellido apellido (persona natural)
 * @property {string|null} razonSocial razón social (persona jurídica)
 * @property {string} nombreMostrado razón social o nombre y apellido
 * @property {string} direccion dirección
 * @property {string} telefono teléfono
 * @property {string|null} correo correo
 * @property {boolean} esCliente rol cliente
 * @property {boolean} esProveedor rol proveedor
 * @property {boolean} esConsumidorFinal es el Consumidor final predefinido
 * @property {'ACTIVO'|'INACTIVO'} estado estado
 */

/**
 * Filtros de la búsqueda.
 *
 * @typedef {object} FiltroTerceros
 * @property {string} [q] nombre, razón social o inicio de la identificación
 * @property {''|'CLIENTE'|'PROVEEDOR'} [rol] rol
 * @property {''|'NATURAL'|'JURIDICA'} [tipoPersona] tipo de persona
 * @property {''|'ACTIVO'|'INACTIVO'} [estado] estado
 * @property {number} [page] página desde 0
 * @property {number} [size] tamaño de página
 */

/**
 * Busca terceros paginados; omite los filtros vacíos.
 *
 * @param {FiltroTerceros} [filtro] filtros y paginación
 * @returns {Promise<{contenido: Tercero[], totalElementos: number}>} página de terceros
 */
export async function buscarTerceros(filtro = {}) {
  const params = Object.fromEntries(
    Object.entries(filtro).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  );
  const { data } = await apiClient.get(RUTA, { params });
  return data;
}

/**
 * Consulta un tercero.
 *
 * @param {number} id id del tercero
 * @returns {Promise<Tercero>} tercero
 */
export async function obtenerTercero(id) {
  const { data } = await apiClient.get(`${RUTA}/${id}`);
  return data;
}

/**
 * Registra un tercero.
 *
 * @param {object} datos cuerpo TerceroPeticion
 * @returns {Promise<Tercero>} tercero creado
 */
export async function crearTercero(datos) {
  const { data } = await apiClient.post(RUTA, datos);
  return data;
}

/**
 * Reemplaza los datos de un tercero.
 *
 * @param {number} id id del tercero
 * @param {object} datos cuerpo TerceroPeticion
 * @returns {Promise<Tercero>} tercero actualizado
 */
export async function actualizarTercero(id, datos) {
  const { data } = await apiClient.put(`${RUTA}/${id}`, datos);
  return data;
}

/**
 * Activa o desactiva un tercero.
 *
 * @param {number} id id del tercero
 * @param {'ACTIVO'|'INACTIVO'} estado estado nuevo
 * @returns {Promise<Tercero>} tercero con su estado
 */
export async function cambiarEstadoTercero(id, estado) {
  const { data } = await apiClient.patch(`${RUTA}/${id}/estado`, { estado });
  return data;
}
