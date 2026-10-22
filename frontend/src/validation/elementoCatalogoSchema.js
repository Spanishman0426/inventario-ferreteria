/**
 * Nombre: elementoCatalogoSchema.js
 * Descripción: Esquema Yup del formulario de marca o categoría, con los mismos límites que
 *              ElementoCatalogoPeticion en el backend (RF-CFG-001, RNF-INT-002).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import * as yup from 'yup';

import { mensaje } from './traducirError.js';

/** Estados de un elemento de catálogo. */
export const ESTADOS_CATALOGO = ['ACTIVO', 'INACTIVO'];

/** Límites del nombre según ElementoCatalogoPeticion (NotBlank, Size de 2 a 100). */
export const LIMITES_CATALOGO = { nombreMin: 2, nombreMax: 100 };

/**
 * Esquema del formulario. La longitud se mide sobre el texto sin recortar, como Size en el backend;
 * un nombre de solo espacios cuenta como vacío (NotBlank).
 */
const elementoCatalogoSchema = yup.object({
  nombre: yup
    .string()
    .required('validacion:requerido')
    .test('no-en-blanco', 'validacion:requerido', (valor) => !valor || valor.trim() !== '')
    .min(LIMITES_CATALOGO.nombreMin, mensaje('validacion:minimo'))
    .max(LIMITES_CATALOGO.nombreMax, mensaje('validacion:maximo')),
});

/**
 * Convierte los valores del formulario en el cuerpo de POST o PUT.
 *
 * @param {{nombre: string}} valores valores de Formik
 * @returns {{nombre: string}} petición con el nombre recortado
 */
export function aPeticionCatalogo(valores) {
  return { nombre: valores.nombre.trim() };
}

export default elementoCatalogoSchema;
