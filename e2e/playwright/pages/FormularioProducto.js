/**
 * Nombre: FormularioProducto.js
 * Descripción: Page Object del diálogo para crear o editar un producto (ProductoFormDialog) en Playwright.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { expect } from '@playwright/test';

/**
 * Valores que se pueden llenar en el formulario; los ausentes no se tocan.
 *
 * @typedef {object} DatosFormulario
 * @property {string} [nombre] nombre
 * @property {string} [descripcion] descripción
 * @property {string} [categoria] nombre visible de la categoría
 * @property {string} [marca] nombre visible de la marca
 * @property {string} [unidad] código con que empieza la opción de unidad (UND, MTR...)
 * @property {string} [iva] nombre visible de la tarifa de IVA
 * @property {string} [porcentaje] porcentaje de incremento
 * @property {string} [control] texto visible del tipo de control (Ninguno, Vencimiento, Garantía)
 * @property {string} [plazo] plazo de garantía en meses
 */

/** Diálogo del formulario de producto. */
export class FormularioProducto {
  /**
   * @param {import('@playwright/test').Page} page página del navegador
   */
  constructor(page) {
    this.page = page;
    this.formulario = page.getByTestId('producto-form');
    this.nombre = page.getByTestId('producto-campo-nombre');
    this.descripcion = page.getByTestId('producto-campo-descripcion');
    this.porcentaje = page.getByTestId('producto-campo-porcentaje');
    this.plazo = page.getByTestId('producto-campo-plazo');
    this.botonGuardar = page.getByTestId('producto-guardar');
    this.botonCancelar = page.getByTestId('producto-cancelar');
  }

  /**
   * Espera a que el diálogo esté visible y sus catálogos cargados.
   *
   * @returns {Promise<void>}
   */
  async esperarAbierto() {
    await expect(this.formulario).toBeVisible();
    await expect(this.formulario.getByRole('progressbar')).toHaveCount(0);
  }

  /**
   * Elige una opción de un selector de MUI.
   *
   * @param {'categoria'|'marca'|'unidad'|'iva'|'control'|'estado'} campo sufijo del data-testid
   * @param {string|RegExp} opcion nombre visible de la opción
   * @returns {Promise<void>}
   */
  async elegir(campo, opcion) {
    await this.page.getByTestId(`producto-campo-${campo}`).click();
    const listado = this.page.getByRole('listbox');
    await listado.getByRole('option', { name: opcion, exact: typeof opcion === 'string' }).click();
    await expect(listado).toBeHidden();
  }

  /**
   * Llena los campos indicados.
   *
   * @param {DatosFormulario} datos valores a escribir o elegir
   * @returns {Promise<void>}
   */
  async llenar(datos) {
    if (datos.nombre !== undefined) await this.nombre.fill(datos.nombre);
    if (datos.descripcion !== undefined) await this.descripcion.fill(datos.descripcion);
    if (datos.categoria) await this.elegir('categoria', datos.categoria);
    if (datos.marca) await this.elegir('marca', datos.marca);
    if (datos.unidad) await this.elegir('unidad', new RegExp(`^${datos.unidad} `));
    if (datos.iva) await this.elegir('iva', datos.iva);
    if (datos.porcentaje !== undefined) await this.porcentaje.fill(datos.porcentaje);
    if (datos.control) await this.elegir('control', datos.control);
    if (datos.plazo !== undefined) await this.plazo.fill(datos.plazo);
  }

  /**
   * Pulsa Guardar.
   *
   * @returns {Promise<void>}
   */
  async guardar() {
    await this.botonGuardar.click();
  }

  /**
   * Etiqueta visible de un campo (MUI la identifica como <id del campo>-label).
   *
   * @param {string} campo sufijo del id del campo (nombre, categoria, marca, unidad, iva, porcentaje...)
   * @returns {import('@playwright/test').Locator} etiqueta
   */
  etiqueta(campo) {
    return this.page.locator(`#producto-${campo}-label`);
  }

  /**
   * Mensaje de error bajo un campo.
   *
   * @param {string} campo sufijo del data-testid (nombre, categoria, marca, unidad, iva, porcentaje, plazo...)
   * @returns {import('@playwright/test').Locator} texto de ayuda del campo
   */
  error(campo) {
    return this.page.getByTestId(`producto-error-${campo}`);
  }
}
