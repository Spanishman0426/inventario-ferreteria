/**
 * Nombre: PaginaCatalogos.js
 * Descripción: Page Object de la administración de catálogos (/catalogos) para Playwright: pestañas
 *              Marcas y Categorías, búsqueda, formulario de alta y activación o desactivación con
 *              confirmación.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { expect } from '@playwright/test';

/** Prefijo singular de los data-testid de las filas, por catálogo. */
const SINGULAR = { marcas: 'marca', categorias: 'categoria' };

/** Página de catálogos. */
export class PaginaCatalogos {
  /**
   * @param {import('@playwright/test').Page} page página del navegador
   */
  constructor(page) {
    this.page = page;
    this.titulo = page.getByTestId('catalogos-titulo');
    this.formulario = page.getByTestId('catalogo-form');
    this.campoNombre = page.getByTestId('catalogo-campo-nombre');
    this.botonGuardar = page.getByTestId('catalogo-guardar');
    this.notificacion = page.getByTestId('notificacion');
  }

  /**
   * Abre una pestaña y espera su tabla.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @returns {Promise<void>}
   */
  async pestana(tipo) {
    await this.page.getByTestId(`catalogos-pestana-${tipo}`).click();
    await this.esperarCarga(tipo);
  }

  /**
   * Espera a que la tabla de un catálogo no muestre la barra de progreso.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @returns {Promise<void>}
   */
  async esperarCarga(tipo) {
    await expect(this.page.getByTestId(`catalogo-${tipo}-tabla`)).toBeVisible();
    await expect(this.page.locator('main').getByRole('progressbar')).toHaveCount(0);
  }

  /**
   * Busca por nombre en un catálogo.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} texto texto a buscar
   * @returns {Promise<void>}
   */
  async buscar(tipo, texto) {
    await this.page.getByTestId(`catalogo-${tipo}-buscar`).fill(texto);
    await this.page.getByTestId(`catalogo-${tipo}-buscar-boton`).click();
    await this.esperarCarga(tipo);
  }

  /**
   * Registra un elemento con el formulario.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} nombre nombre del elemento
   * @returns {Promise<void>}
   */
  async crear(tipo, nombre) {
    await this.page.getByTestId(`catalogo-${tipo}-nuevo`).click();
    await expect(this.formulario).toBeVisible();
    await this.campoNombre.fill(nombre);
    await this.botonGuardar.click();
    await expect(this.formulario).toBeHidden();
  }

  /**
   * Fila de la tabla que contiene un nombre.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} nombre nombre exacto
   * @returns {import('@playwright/test').Locator} fila
   */
  fila(tipo, nombre) {
    const singular = SINGULAR[tipo];
    return this.page.getByTestId(new RegExp(`^catalogo-${singular}-fila-`)).filter({
      has: this.page
        .getByTestId(new RegExp(`^catalogo-${singular}-nombre-`))
        .getByText(nombre, { exact: true }),
    });
  }

  /**
   * Chip de estado de una fila.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} nombre nombre exacto
   * @returns {import('@playwright/test').Locator} chip
   */
  estado(tipo, nombre) {
    return this.fila(tipo, nombre).getByTestId(
      new RegExp(`^catalogo-${SINGULAR[tipo]}-estado-\\d+$`),
    );
  }

  /**
   * Pulsa Desactivar en una fila y responde la confirmación.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} nombre nombre exacto
   * @param {boolean} aceptar true confirma; false cancela
   * @returns {Promise<import('@playwright/test').Locator>} diálogo de confirmación (ya cerrado)
   */
  async desactivar(tipo, nombre, aceptar) {
    const singular = SINGULAR[tipo];
    await this.fila(tipo, nombre)
      .getByTestId(new RegExp(`^catalogo-${singular}-desactivar-\\d+$`))
      .click();
    const dialogo = this.page.getByTestId(`catalogo-${singular}-estado-confirmacion-dialogo`);
    await expect(dialogo).toBeVisible();
    await this.page
      .getByTestId(`catalogo-${singular}-estado-confirmacion-${aceptar ? 'aceptar' : 'cancelar'}`)
      .click();
    await expect(dialogo).toBeHidden();
    return dialogo;
  }
}
