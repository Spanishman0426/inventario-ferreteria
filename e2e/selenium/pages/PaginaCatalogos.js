/**
 * Nombre: PaginaCatalogos.js
 * Descripción: Page Object de la administración de catálogos (/catalogos) para Selenium WebDriver:
 *              pestañas, búsqueda, formulario de alta y desactivación con confirmación.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { By } from 'selenium-webdriver';

import {
  clic,
  esperarAusente,
  esperarQue,
  esperarVisible,
  escribir,
  porTestId,
  textoDe,
} from '../support/esperas.js';

/** Prefijo singular de los data-testid de las filas, por catálogo. */
const SINGULAR = { marcas: 'marca', categorias: 'categoria' };

/** Localizadores de la página. */
export const CATALOGOS = {
  titulo: porTestId('catalogos-titulo'),
  progreso: By.css('main [role="progressbar"]'),
  formulario: porTestId('catalogo-form'),
  campoNombre: porTestId('catalogo-campo-nombre'),
  guardar: porTestId('catalogo-guardar'),
  notificacion: porTestId('notificacion'),
  accesoDenegado: porTestId('acceso-denegado'),
  /**
   * Elemento propio de un catálogo (tabla, buscar, buscar-boton, nuevo...).
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} parte sufijo del data-testid
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  deCatalogo: (tipo, parte) => porTestId(`catalogo-${tipo}-${parte}`),
  /**
   * Elemento de una fila (nombre, estado, editar, desactivar, activar).
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} parte parte de la fila
   * @param {string} id id del elemento
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  deElemento: (tipo, parte, id) => porTestId(`catalogo-${SINGULAR[tipo]}-${parte}-${id}`),
  /**
   * Diálogo o botón de la confirmación de cambio de estado.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {'dialogo'|'aceptar'|'cancelar'} parte parte del diálogo
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  confirmacion: (tipo, parte) =>
    porTestId(`catalogo-${SINGULAR[tipo]}-estado-confirmacion-${parte}`),
};

/** Página de catálogos. */
export class PaginaCatalogos {
  /**
   * @param {import('selenium-webdriver').WebDriver} driver navegador
   */
  constructor(driver) {
    this.driver = driver;
  }

  /**
   * Espera a que la tabla de un catálogo no muestre la barra de progreso.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @returns {Promise<void>}
   */
  async esperarCarga(tipo) {
    await esperarVisible(this.driver, CATALOGOS.deCatalogo(tipo, 'tabla'));
    await esperarAusente(this.driver, CATALOGOS.progreso);
  }

  /**
   * Busca por nombre en un catálogo.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} texto texto a buscar
   * @returns {Promise<void>}
   */
  async buscar(tipo, texto) {
    await escribir(await esperarVisible(this.driver, CATALOGOS.deCatalogo(tipo, 'buscar')), texto);
    await clic(this.driver, CATALOGOS.deCatalogo(tipo, 'buscar-boton'));
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
    await clic(this.driver, CATALOGOS.deCatalogo(tipo, 'nuevo'));
    await escribir(await esperarVisible(this.driver, CATALOGOS.campoNombre), nombre);
    await clic(this.driver, CATALOGOS.guardar);
    await esperarAusente(this.driver, CATALOGOS.formulario);
  }

  /**
   * Espera la fila con un nombre exacto y devuelve el id del elemento.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} nombre nombre exacto
   * @returns {Promise<string>} id del elemento
   */
  async idDe(tipo, nombre) {
    const prefijo = `catalogo-${SINGULAR[tipo]}-nombre-`;
    let id = null;
    await esperarQue(
      this.driver,
      async () => {
        for (const celda of await this.driver.findElements(By.css(`[data-testid^="${prefijo}"]`))) {
          if ((await textoDe(celda)) === nombre) {
            id = (await celda.getAttribute('data-testid')).replace(prefijo, '');
            return true;
          }
        }
        return false;
      },
      () => `No aparece en la tabla "${nombre}"`,
    );
    return id;
  }

  /**
   * Pulsa Desactivar en una fila y responde la confirmación.
   *
   * @param {'marcas'|'categorias'} tipo catálogo
   * @param {string} id id del elemento
   * @param {boolean} aceptar true confirma; false cancela
   * @returns {Promise<void>}
   */
  async desactivar(tipo, id, aceptar) {
    await clic(this.driver, CATALOGOS.deElemento(tipo, 'desactivar', id));
    await esperarVisible(this.driver, CATALOGOS.confirmacion(tipo, 'dialogo'));
    await clic(this.driver, CATALOGOS.confirmacion(tipo, aceptar ? 'aceptar' : 'cancelar'));
    await esperarAusente(this.driver, CATALOGOS.confirmacion(tipo, 'dialogo'));
  }
}
