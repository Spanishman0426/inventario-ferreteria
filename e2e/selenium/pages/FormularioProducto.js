/**
 * Nombre: FormularioProducto.js
 * Descripción: Page Object del diálogo para crear o editar un producto (ProductoFormDialog) en
 *              Selenium WebDriver.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { By } from 'selenium-webdriver';

import {
  clic,
  esperarAusente,
  esperarQueContenga,
  esperarVisible,
  escribir,
  porTestId,
} from '../support/esperas.js';

/** Localizadores del formulario. */
export const FORMULARIO = {
  formulario: porTestId('producto-form'),
  progreso: By.css('[data-testid="producto-form"] [role="progressbar"]'),
  listado: By.css('ul[role="listbox"]'),
  guardar: porTestId('producto-guardar'),
  cancelar: porTestId('producto-cancelar'),
  /**
   * Campo del formulario (input o selector).
   *
   * @param {string} campo sufijo del data-testid
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  campo: (campo) => porTestId(`producto-campo-${campo}`),
  /**
   * Etiqueta visible de un campo (MUI la identifica como <id del campo>-label).
   *
   * @param {string} campo sufijo del id del campo
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  etiqueta: (campo) => By.id(`producto-${campo}-label`),
  /**
   * Mensaje de error bajo un campo.
   *
   * @param {string} campo sufijo del data-testid
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  error: (campo) => porTestId(`producto-error-${campo}`),
};

/**
 * Opción de un listado de MUI por texto exacto o por inicio del texto.
 *
 * @param {string} texto texto visible
 * @param {boolean} prefijo true si basta con que el texto empiece así
 * @returns {import('selenium-webdriver').Locator} localizador XPath
 */
function opcion(texto, prefijo) {
  const condicion = prefijo
    ? `starts-with(normalize-space(.), "${texto} ")`
    : `normalize-space(.)="${texto}"`;
  return By.xpath(`//ul[@role="listbox"]/li[@role="option" and ${condicion}]`);
}

/** Diálogo del formulario de producto. */
export class FormularioProducto {
  /**
   * @param {import('selenium-webdriver').WebDriver} driver navegador
   */
  constructor(driver) {
    this.driver = driver;
  }

  /**
   * Espera a que el diálogo esté visible y sus catálogos cargados.
   *
   * @returns {Promise<void>}
   */
  async esperarAbierto() {
    await esperarVisible(this.driver, FORMULARIO.formulario);
    await esperarAusente(this.driver, FORMULARIO.progreso);
  }

  /**
   * Elige una opción de un selector de MUI.
   *
   * @param {'categoria'|'marca'|'unidad'|'iva'|'control'|'estado'} campo sufijo del data-testid
   * @param {string} texto texto visible de la opción
   * @param {boolean} [prefijo] true si basta con que la opción empiece por el texto
   * @returns {Promise<void>}
   */
  async elegir(campo, texto, prefijo = false) {
    await clic(this.driver, FORMULARIO.campo(campo));
    await clic(this.driver, opcion(texto, prefijo));
    await esperarAusente(this.driver, FORMULARIO.listado);
    await esperarQueContenga(this.driver, FORMULARIO.campo(campo), texto);
  }

  /**
   * Escribe en un campo de texto.
   *
   * @param {string} campo sufijo del data-testid
   * @param {string} texto texto nuevo
   * @returns {Promise<void>}
   */
  async escribir(campo, texto) {
    await escribir(await esperarVisible(this.driver, FORMULARIO.campo(campo)), texto);
  }

  /**
   * Valor actual de un campo de texto.
   *
   * @param {string} campo sufijo del data-testid
   * @returns {Promise<string>} valor
   */
  async valor(campo) {
    return (await this.driver.findElement(FORMULARIO.campo(campo))).getAttribute('value');
  }

  /**
   * Llena los campos indicados; los ausentes no se tocan.
   *
   * @param {object} datos valores (mismas claves que el Page Object de Playwright)
   * @param {string} [datos.nombre] nombre
   * @param {string} [datos.descripcion] descripción
   * @param {string} [datos.categoria] categoría
   * @param {string} [datos.marca] marca
   * @param {string} [datos.unidad] código de unidad (UND, MTR...)
   * @param {string} [datos.iva] tarifa de IVA
   * @param {string} [datos.porcentaje] porcentaje de incremento
   * @param {string} [datos.control] tipo de control visible
   * @param {string} [datos.plazo] plazo de garantía
   * @returns {Promise<void>}
   */
  async llenar(datos) {
    if (datos.nombre !== undefined) await this.escribir('nombre', datos.nombre);
    if (datos.descripcion !== undefined) await this.escribir('descripcion', datos.descripcion);
    if (datos.categoria) await this.elegir('categoria', datos.categoria);
    if (datos.marca) await this.elegir('marca', datos.marca);
    if (datos.unidad) await this.elegir('unidad', datos.unidad, true);
    if (datos.iva) await this.elegir('iva', datos.iva);
    if (datos.porcentaje !== undefined) await this.escribir('porcentaje', datos.porcentaje);
    if (datos.control) await this.elegir('control', datos.control);
    if (datos.plazo !== undefined) await this.escribir('plazo', datos.plazo);
  }

  /**
   * Pulsa Guardar.
   *
   * @returns {Promise<void>}
   */
  async guardar() {
    await clic(this.driver, FORMULARIO.guardar);
  }
}
