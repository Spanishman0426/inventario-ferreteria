/**
 * Nombre: productoSchema.js
 * Descripción: Esquema Yup del formulario de producto (RF-PRO-001, RNF-INT-002). Replica las reglas
 *              de ProductoPeticion y GarantiaConPlazoValidator del backend, y convierte los valores
 *              del formulario en la petición de la API.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import * as yup from 'yup';

import { mensaje } from './traducirError.js';

/** Tipos de control del producto (RN-18). */
export const CONTROLES = ['NINGUNO', 'VENCIMIENTO', 'GARANTIA'];

/** Estados del producto. */
export const ESTADOS = ['ACTIVO', 'INACTIVO'];

/** Límites de los campos según ProductoPeticion. */
export const LIMITES = {
  nombreMin: 3,
  nombreMax: 150,
  descripcionMax: 500,
  unidadBaseMax: 4,
  porcentajeMin: 0,
  porcentajeMax: 500,
  porcentajeDecimales: 2,
  plazoMin: 1,
  plazoMax: 32767,
};

/**
 * Mensaje de rango fijo, con mínimo y máximo, para min() y max() de Yup.
 *
 * @param {number} min límite inferior
 * @param {number} max límite superior
 * @returns {() => import('./traducirError.js').MensajeValidacion} función de mensaje para Yup
 */
function rango(min, max) {
  return () => ({ clave: 'validacion:rango', valores: { min, max } });
}

/**
 * Convierte el texto vacío de un campo del formulario en undefined, para que cuente como ausente.
 *
 * @param {unknown} valor valor ya convertido por Yup
 * @param {unknown} original valor original del campo
 * @returns {unknown} valor o undefined
 */
function vacioAAusente(valor, original) {
  return original === '' || original === null ? undefined : valor;
}

/**
 * Indica si un número tiene como máximo los decimales indicados.
 *
 * @param {number} valor número a revisar
 * @param {number} decimales decimales permitidos
 * @returns {boolean} verdadero si cumple
 */
function maximoDecimales(valor, decimales) {
  const escalado = valor * 10 ** decimales;
  return Math.abs(escalado - Math.round(escalado)) < 1e-9;
}

/**
 * Referencia obligatoria a un catálogo (id numérico elegido en un selector).
 *
 * @returns {yup.NumberSchema} esquema del id
 */
const referencia = () =>
  yup
    .number()
    .transform(vacioAAusente)
    .typeError('validacion:opcionInvalida')
    .required('validacion:requerido');

/**
 * Esquema de validación del producto. Las longitudes se miden sobre el texto sin recortar, como
 * hace `@Size` en el backend; un nombre de solo espacios cuenta como vacío (`@NotBlank`).
 */
const productoSchema = yup.object({
  nombre: yup
    .string()
    .required('validacion:requerido')
    .test('no-en-blanco', 'validacion:requerido', (valor) => !valor || valor.trim() !== '')
    .min(LIMITES.nombreMin, mensaje('validacion:minimo'))
    .max(LIMITES.nombreMax, mensaje('validacion:maximo')),
  descripcion: yup.string().nullable().max(LIMITES.descripcionMax, mensaje('validacion:maximo')),
  categoriaId: referencia(),
  marcaId: referencia(),
  unidadBase: yup
    .string()
    .required('validacion:requerido')
    .max(LIMITES.unidadBaseMax, mensaje('validacion:maximo')),
  tarifaIvaId: referencia(),
  porcentajeIncremento: yup
    .number()
    .transform(vacioAAusente)
    .typeError('validacion:numero')
    .required('validacion:requerido')
    .min(LIMITES.porcentajeMin, rango(LIMITES.porcentajeMin, LIMITES.porcentajeMax))
    .max(LIMITES.porcentajeMax, rango(LIMITES.porcentajeMin, LIMITES.porcentajeMax))
    .test(
      'decimales',
      () => ({ clave: 'validacion:decimales', valores: { max: LIMITES.porcentajeDecimales } }),
      (valor) => valor === undefined || maximoDecimales(valor, LIMITES.porcentajeDecimales),
    ),
  control: yup
    .string()
    .transform(vacioAAusente)
    .oneOf(CONTROLES, 'validacion:opcionInvalida')
    .required('validacion:requerido'),
  // Obligatorio si y solo si control = GARANTIA (GarantiaConPlazoValidator); en otro caso se descarta.
  plazoGarantiaMeses: yup
    .number()
    .transform(vacioAAusente)
    .typeError('validacion:entero')
    .when('control', {
      is: 'GARANTIA',
      then: (s) =>
        s
          .required('validacion:requerido')
          .integer('validacion:entero')
          .min(LIMITES.plazoMin, rango(LIMITES.plazoMin, LIMITES.plazoMax))
          .max(LIMITES.plazoMax, rango(LIMITES.plazoMin, LIMITES.plazoMax)),
      otherwise: (s) => s.strip(),
    }),
  estado: yup.string().transform(vacioAAusente).oneOf(ESTADOS, 'validacion:opcionInvalida'),
});

export default productoSchema;

/**
 * Valores del formulario: los selectores y números se manejan como texto ('' = sin elegir).
 *
 * @typedef {object} ValoresProducto
 * @property {string} nombre nombre
 * @property {string} descripcion descripción
 * @property {number|string} categoriaId id de categoría
 * @property {number|string} marcaId id de marca
 * @property {string} unidadBase código de unidad base
 * @property {number|string} tarifaIvaId id de tarifa de IVA
 * @property {number|string} porcentajeIncremento margen
 * @property {string} control tipo de control
 * @property {number|string} plazoGarantiaMeses meses de garantía
 * @property {string} estado estado (solo al editar)
 */

/**
 * Valores iniciales del formulario, vacíos o tomados de un producto de la API.
 *
 * @param {object|null} [producto] producto a editar (ProductoRespuesta)
 * @returns {ValoresProducto} valores del formulario
 */
export function valoresIniciales(producto) {
  return {
    nombre: producto?.nombre ?? '',
    descripcion: producto?.descripcion ?? '',
    categoriaId: producto?.categoria?.id ?? '',
    marcaId: producto?.marca?.id ?? '',
    unidadBase: producto?.unidadBase?.codigo ?? '',
    tarifaIvaId: producto?.tarifaIva?.id ?? '',
    porcentajeIncremento: producto?.porcentajeIncremento ?? '',
    control: producto?.control ?? 'NINGUNO',
    plazoGarantiaMeses: producto?.plazoGarantiaMeses ?? '',
    estado: producto?.estado ?? '',
  };
}

/**
 * Convierte los valores del formulario en la petición ProductoPeticion. El nombre se envía tal cual
 * (el backend mide la longitud sin recortar y recorta al guardar); el plazo va en null si el control
 * no es GARANTIA y el estado solo se envía si viene informado.
 *
 * @param {ValoresProducto} valores valores validados del formulario
 * @returns {object} petición para crear o actualizar
 */
export function aPeticion(valores) {
  const descripcion = valores.descripcion?.trim() ? valores.descripcion : null;
  const peticion = {
    nombre: valores.nombre,
    descripcion,
    categoriaId: Number(valores.categoriaId),
    marcaId: Number(valores.marcaId),
    unidadBase: valores.unidadBase,
    tarifaIvaId: Number(valores.tarifaIvaId),
    porcentajeIncremento: Number(valores.porcentajeIncremento),
    control: valores.control,
    plazoGarantiaMeses: valores.control === 'GARANTIA' ? Number(valores.plazoGarantiaMeses) : null,
  };
  if (valores.estado) {
    peticion.estado = valores.estado;
  }
  return peticion;
}
