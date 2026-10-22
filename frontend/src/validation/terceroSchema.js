/**
 * Nombre: terceroSchema.js
 * Descripción: Esquema Yup del formulario de tercero, con las mismas reglas que TerceroPeticion y
 *              TerceroCoherente en el backend (RF-TER-001, RN-26, RNF-INT-002), y conversión de los
 *              valores del formulario al cuerpo de la API.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import * as yup from 'yup';

import { calcularDigitoVerificacion, esDigitoValido } from '../utils/nit.js';
import { mensaje } from './traducirError.js';

/** Tipos de persona. */
export const TIPOS_PERSONA = ['NATURAL', 'JURIDICA'];

/** Tipos de identificación. */
export const TIPOS_IDENTIFICACION = ['CC', 'NIT'];

/** Roles de un tercero (filtro de búsqueda). */
export const ROLES_TERCERO = ['CLIENTE', 'PROVEEDOR'];

/** Estados de un tercero. */
export const ESTADOS_TERCERO = ['ACTIVO', 'INACTIVO'];

/** Límites de TerceroPeticion. */
export const LIMITES_TERCERO = {
  identificacionMin: 5,
  identificacionMax: 15,
  nombreMax: 100,
  razonSocialMax: 150,
  direccionMax: 200,
  telefonoMax: 10,
  correoMax: 150,
};

/** Número de identificación: de 5 a 15 dígitos. */
const IDENTIFICACION = /^[0-9]{5,15}$/;

/** Teléfono: de 7 a 10 dígitos. */
const TELEFONO = /^[0-9]{7,10}$/;

/**
 * Valores del formulario de tercero.
 *
 * @typedef {object} ValoresTercero
 * @property {'NATURAL'|'JURIDICA'} tipoPersona tipo de persona
 * @property {'CC'|'NIT'} tipoIdentificacion tipo de identificación
 * @property {string} numeroIdentificacion número
 * @property {string} digitoVerificacion dígito del NIT ('' si no aplica)
 * @property {string} nombre nombre
 * @property {string} apellido apellido
 * @property {string} razonSocial razón social
 * @property {string} direccion dirección
 * @property {string} telefono teléfono
 * @property {string} correo correo
 * @property {boolean} esCliente rol cliente
 * @property {boolean} esProveedor rol proveedor
 */

/**
 * Texto obligatorio que no puede quedar en blanco.
 *
 * @param {number} max longitud máxima
 * @returns {yup.StringSchema} esquema
 */
const textoObligatorio = (max) =>
  yup
    .string()
    .required('validacion:requerido')
    .test('no-en-blanco', 'validacion:requerido', (valor) => !valor || valor.trim() !== '')
    .max(max, mensaje('validacion:maximo'));

/**
 * Esquema del formulario. Los campos que no aplican al tipo de persona no se validan.
 */
const terceroSchema = yup.object({
  tipoPersona: yup
    .string()
    .oneOf(TIPOS_PERSONA, 'validacion:opcionInvalida')
    .required('validacion:requerido'),
  tipoIdentificacion: yup
    .string()
    .oneOf(TIPOS_IDENTIFICACION, 'validacion:opcionInvalida')
    .required('validacion:requerido')
    .test('juridica-solo-nit', 'validacion:juridicaSoloNit', function soloNitEnJuridica(valor) {
      return this.parent.tipoPersona !== 'JURIDICA' || valor === 'NIT';
    }),
  numeroIdentificacion: yup
    .string()
    .required('validacion:requerido')
    .matches(IDENTIFICACION, 'validacion:identificacionFormato'),
  digitoVerificacion: yup.string().when('tipoIdentificacion', {
    is: 'NIT',
    then: (s) =>
      s.required('validacion:requerido').test('digito-nit', function validarDigito(valor) {
        const esperado = calcularDigitoVerificacion(this.parent.numeroIdentificacion);
        // Vacío lo informa required; si el número no es válido, el error va en el número.
        if (
          valor === undefined ||
          valor === '' ||
          esperado === null ||
          !IDENTIFICACION.test(this.parent.numeroIdentificacion)
        )
          return true;
        return (
          esDigitoValido(this.parent.numeroIdentificacion, valor) ||
          this.createError({
            message: { clave: 'validacion:digitoInvalido', valores: { esperado } },
          })
        );
      }),
  }),
  nombre: yup.string().when('tipoPersona', {
    is: 'NATURAL',
    then: () => textoObligatorio(LIMITES_TERCERO.nombreMax),
  }),
  apellido: yup.string().when('tipoPersona', {
    is: 'NATURAL',
    then: () => textoObligatorio(LIMITES_TERCERO.nombreMax),
  }),
  razonSocial: yup.string().when('tipoPersona', {
    is: 'JURIDICA',
    then: () => textoObligatorio(LIMITES_TERCERO.razonSocialMax),
  }),
  direccion: textoObligatorio(LIMITES_TERCERO.direccionMax),
  telefono: yup
    .string()
    .required('validacion:requerido')
    .matches(TELEFONO, 'validacion:telefonoFormato'),
  correo: yup
    .string()
    .email('validacion:correo')
    .max(LIMITES_TERCERO.correoMax, mensaje('validacion:maximo')),
  esCliente: yup
    .boolean()
    .test('al-menos-un-rol', 'validacion:rolObligatorio', function alMenosUnRol(valor) {
      return Boolean(valor) || Boolean(this.parent.esProveedor);
    }),
  esProveedor: yup.boolean(),
});

/**
 * Valores iniciales del formulario: los del tercero a editar o, al crear, una persona natural con
 * cédula y el rol que el usuario puede registrar.
 *
 * @param {import('../services/tercerosService.js').Tercero|null} tercero tercero a editar
 * @param {{cliente: boolean, proveedor: boolean}} permisos permisos del usuario por rol
 * @returns {ValoresTercero} valores de Formik
 */
export function valoresInicialesTercero(tercero, permisos) {
  if (!tercero) {
    return {
      tipoPersona: 'NATURAL',
      tipoIdentificacion: 'CC',
      numeroIdentificacion: '',
      digitoVerificacion: '',
      nombre: '',
      apellido: '',
      razonSocial: '',
      direccion: '',
      telefono: '',
      correo: '',
      esCliente: permisos.cliente,
      esProveedor: !permisos.cliente && permisos.proveedor,
    };
  }
  return {
    tipoPersona: tercero.tipoPersona,
    tipoIdentificacion: tercero.tipoIdentificacion,
    numeroIdentificacion: tercero.numeroIdentificacion,
    digitoVerificacion:
      tercero.digitoVerificacion == null ? '' : String(tercero.digitoVerificacion),
    nombre: tercero.nombre ?? '',
    apellido: tercero.apellido ?? '',
    razonSocial: tercero.razonSocial ?? '',
    direccion: tercero.direccion ?? '',
    telefono: tercero.telefono ?? '',
    correo: tercero.correo ?? '',
    esCliente: tercero.esCliente,
    esProveedor: tercero.esProveedor,
  };
}

/**
 * Convierte los valores del formulario en el cuerpo de POST o PUT: recorta textos y omite los campos
 * que no aplican al tipo de persona o de identificación.
 *
 * @param {ValoresTercero} v valores de Formik
 * @returns {object} cuerpo TerceroPeticion
 */
export function aPeticionTercero(v) {
  const natural = v.tipoPersona === 'NATURAL';
  const nit = v.tipoIdentificacion === 'NIT';
  const correo = v.correo.trim();
  return {
    tipoPersona: v.tipoPersona,
    tipoIdentificacion: v.tipoIdentificacion,
    numeroIdentificacion: v.numeroIdentificacion.trim(),
    digitoVerificacion: nit ? Number(v.digitoVerificacion) : null,
    nombre: natural ? v.nombre.trim() : null,
    apellido: natural ? v.apellido.trim() : null,
    razonSocial: natural ? null : v.razonSocial.trim(),
    direccion: v.direccion.trim(),
    telefono: v.telefono.trim(),
    correo: correo === '' ? null : correo,
    esCliente: v.esCliente,
    esProveedor: v.esProveedor,
  };
}

export default terceroSchema;
