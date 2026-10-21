/**
 * Nombre: loginSchema.js
 * Descripción: Esquema Yup del formulario de inicio de sesión (RF-SEG-001, RNF-INT-002).
 *              Los límites coinciden con SolicitudInicioSesion del contrato OpenAPI.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import * as yup from 'yup';

import { mensaje } from './traducirError.js';

/** Longitud máxima del usuario según la API. */
const MAX_USUARIO = 100;

/** Longitud máxima de la contraseña según la API (límite de bcrypt). */
const MAX_CONTRASENA = 72;

/** Esquema de validación: usuario y contraseña obligatorios. */
const loginSchema = yup.object({
  usuario: yup
    .string()
    .trim()
    .required('validacion:requerido')
    .max(MAX_USUARIO, mensaje('validacion:maximo')),
  contrasena: yup
    .string()
    .required('validacion:requerido')
    .max(MAX_CONTRASENA, mensaje('validacion:maximo')),
});

export default loginSchema;
