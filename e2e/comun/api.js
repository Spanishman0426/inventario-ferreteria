/**
 * Nombre: api.js
 * Descripción: Cliente mínimo de la API REST para preparar datos de las pruebas E2E (iniciar sesión,
 *              crear e inactivar productos, buscar catálogos activos) sin pasar por la interfaz.
 *              Usa fetch de Node.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { BASE_URL, USUARIOS, contrasenaDe } from './entorno.js';

/**
 * Ejecuta una petición contra la API y devuelve el cuerpo JSON (o null si no hay contenido).
 *
 * @param {string} ruta ruta que empieza con /api
 * @param {RequestInit & {token?: string}} [opciones] opciones de fetch y token JWT
 * @returns {Promise<any>} cuerpo de la respuesta
 * @throws {Error} si la respuesta no es 2xx
 */
async function pedir(ruta, { token, headers, ...opciones } = {}) {
  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    ...opciones,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });
  if (!respuesta.ok) {
    throw new Error(
      `${opciones.method ?? 'GET'} ${ruta} respondió ${respuesta.status}: ${await respuesta.text()}`,
    );
  }
  return respuesta.status === 204 ? null : respuesta.json();
}

/**
 * Inicia sesión por API con un usuario de prueba.
 *
 * @param {keyof typeof USUARIOS} rol admin, jefe o vendedor
 * @returns {Promise<string>} token JWT
 */
export async function iniciarSesionApi(rol) {
  const datos = await pedir('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ usuario: USUARIOS[rol].usuario, contrasena: contrasenaDe(rol) }),
  });
  return datos.accessToken;
}

/**
 * Ejecuta una petición y devuelve solo su código de estado, sin fallar en 4xx (pruebas de permisos).
 *
 * @param {string} ruta ruta que empieza con /api
 * @param {{method?: string, token?: string, body?: object}} [opciones] método, token y cuerpo
 * @returns {Promise<number>} código HTTP
 */
export async function estadoDe(ruta, { method = 'GET', token, body } = {}) {
  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    method,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  return respuesta.status;
}

/** Lista de activos de cada catálogo: marcas y categorías usan /activas (RF-CFG-001). */
const LISTA_ACTIVOS = {
  marcas: '/api/v1/marcas/activas',
  categorias: '/api/v1/categorias/activas',
  'tarifas-iva': '/api/v1/tarifas-iva',
};

/**
 * Busca el id de un elemento activo de catálogo por su nombre exacto.
 *
 * @param {string} token token JWT
 * @param {'marcas'|'categorias'|'tarifas-iva'} catalogo catálogo
 * @param {string} nombre nombre visible
 * @returns {Promise<number>} id
 */
export async function idDeCatalogo(token, catalogo, nombre) {
  const lista = await pedir(LISTA_ACTIVOS[catalogo], { token });
  const elemento = lista.find((e) => e.nombre === nombre);
  if (!elemento) {
    throw new Error(`No existe "${nombre}" activo en ${LISTA_ACTIVOS[catalogo]}`);
  }
  return elemento.id;
}

/**
 * Datos mínimos para crear un producto por API.
 *
 * @typedef {object} ProductoNuevo
 * @property {string} nombre nombre único
 * @property {string} categoria nombre de la categoría
 * @property {string} marca nombre de la marca
 */

/**
 * Crea un producto por API con unidad UND, IVA general, 30 % y sin control.
 *
 * @param {string} token token JWT de un usuario con CATALOGO_EDITAR
 * @param {ProductoNuevo} producto datos del producto
 * @returns {Promise<{id: number, nombre: string}>} producto creado
 */
export async function crearProductoApi(token, { nombre, categoria, marca }) {
  const [categoriaId, marcaId, tarifaIvaId] = await Promise.all([
    idDeCatalogo(token, 'categorias', categoria),
    idDeCatalogo(token, 'marcas', marca),
    idDeCatalogo(token, 'tarifas-iva', 'Tarifa general'),
  ]);
  return pedir('/api/v1/productos', {
    method: 'POST',
    token,
    body: JSON.stringify({
      nombre,
      descripcion: null,
      categoriaId,
      marcaId,
      unidadBase: 'UND',
      tarifaIvaId,
      porcentajeIncremento: 30,
      control: 'NINGUNO',
      plazoGarantiaMeses: null,
    }),
  });
}

/**
 * Inactiva un producto por API (borrado lógico).
 *
 * @param {string} token token JWT de un usuario con CATALOGO_EDITAR
 * @param {number} id id del producto
 * @returns {Promise<void>}
 */
export async function inactivarProductoApi(token, id) {
  await pedir(`/api/v1/productos/${id}`, { method: 'DELETE', token });
}

/**
 * Consulta un producto por API.
 *
 * @param {string} token token JWT
 * @param {number} id id del producto
 * @returns {Promise<object>} ProductoRespuesta
 */
export async function obtenerProductoApi(token, id) {
  return pedir(`/api/v1/productos/${id}`, { token });
}
