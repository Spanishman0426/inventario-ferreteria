/**
 * Nombre: entorno.js
 * Descripción: Configuración común de las pruebas E2E (Playwright y Selenium): URL base del frontend y
 *              usuarios de prueba con sus contraseñas, leídas de variables de entorno o del .env de la raíz.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/** Carpeta raíz del repositorio (e2e/comun → ../..). */
export const RAIZ_REPOSITORIO = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  '..',
);

// Las variables ya definidas (por ejemplo en CI) tienen prioridad: loadEnvFile no las reemplaza.
const archivoEnv = path.join(RAIZ_REPOSITORIO, '.env');
if (existsSync(archivoEnv)) {
  process.loadEnvFile(archivoEnv);
}

/** Puerto local donde docker-compose.e2e.yml publica el frontend. */
export const PUERTO = process.env.E2E_PUERTO || '8088';

/** URL base del frontend compilado; /api la reenvía nginx al backend. */
export const BASE_URL = (process.env.E2E_BASE_URL || `http://127.0.0.1:${PUERTO}`).replace(
  /\/$/,
  '',
);

/**
 * Usuario de prueba de database/02_datos_prueba.sql.
 *
 * @typedef {object} UsuarioPrueba
 * @property {string} usuario nombre de usuario para iniciar sesión
 * @property {string} nombreCompleto nombre que muestra la barra superior
 * @property {string} variable variable de entorno con la contraseña
 */

/** @type {Record<'admin'|'jefe'|'vendedor', UsuarioPrueba>} */
export const USUARIOS = {
  admin: { usuario: 'admin', nombreCompleto: 'Ana Administradora', variable: 'E2E_ADMIN_PASSWORD' },
  jefe: { usuario: 'jefe', nombreCompleto: 'Julia Jefe Inventario', variable: 'E2E_JEFE_PASSWORD' },
  vendedor: {
    usuario: 'vendedor',
    nombreCompleto: 'Valeria Vendedora',
    variable: 'E2E_VENDEDOR_PASSWORD',
  },
};

/**
 * Devuelve la contraseña de un usuario de prueba desde su variable de entorno.
 *
 * @param {keyof typeof USUARIOS} rol clave del usuario (admin, jefe o vendedor)
 * @returns {string} contraseña
 * @throws {Error} si la variable no está definida
 */
export function contrasenaDe(rol) {
  const { variable } = USUARIOS[rol];
  const valor = process.env[variable];
  if (!valor) {
    throw new Error(
      `Falta la variable ${variable}: copie .env.example a .env en la raíz del repositorio.`,
    );
  }
  return valor;
}
