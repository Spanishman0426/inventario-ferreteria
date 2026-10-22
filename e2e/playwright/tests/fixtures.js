/**
 * Nombre: fixtures.js
 * Descripción: Extiende el test de Playwright con los Page Objects de la aplicación, para que cada
 *              prueba los reciba ya creados.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { test as base, expect } from '@playwright/test';

import { BarraSuperior } from '../pages/BarraSuperior.js';
import { PaginaCatalogos } from '../pages/PaginaCatalogos.js';
import { PaginaLogin } from '../pages/PaginaLogin.js';
import { PaginaProductos } from '../pages/PaginaProductos.js';
import { PaginaTerceros } from '../pages/PaginaTerceros.js';

/**
 * Page Objects disponibles en cada prueba.
 *
 * @typedef {object} PaginasApp
 * @property {PaginaLogin} paginaLogin página de inicio de sesión
 * @property {PaginaProductos} paginaProductos página de productos
 * @property {PaginaCatalogos} paginaCatalogos página de catálogos (marcas y categorías)
 * @property {PaginaTerceros} paginaTerceros página de clientes y proveedores
 * @property {BarraSuperior} barra barra superior y selector de idioma
 */

/** Test de Playwright con los Page Objects como fixtures. */
export const test = base.extend(
  /** @type {import('@playwright/test').Fixtures<PaginasApp>} */ ({
    paginaLogin: async ({ page }, usar) => usar(new PaginaLogin(page)),
    paginaProductos: async ({ page }, usar) => usar(new PaginaProductos(page)),
    paginaCatalogos: async ({ page }, usar) => usar(new PaginaCatalogos(page)),
    paginaTerceros: async ({ page }, usar) => usar(new PaginaTerceros(page)),
    barra: async ({ page }, usar) => usar(new BarraSuperior(page)),
  }),
);

export { expect };
