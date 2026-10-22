/**
 * Nombre: BarraSuperior.js
 * Descripción: Page Object de la barra superior de las páginas con sesión y del selector de idioma
 *              (que también aparece en el login) para Selenium WebDriver.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { clic, esperarAusente, porTestId } from '../support/esperas.js';

/** Localizadores de la barra. */
export const BARRA = {
  usuarioActual: porTestId('usuario-actual'),
  menuProductos: porTestId('menu-productos'),
  /** Entrada del menú que solo ve quien tiene CONFIG_GESTIONAR. */
  menuCatalogos: porTestId('menu-catalogos'),
  /** Entrada del menú que ve quien tiene TERCERO_CLIENTE_EDITAR o TERCERO_PROVEEDOR_EDITAR. */
  menuTerceros: porTestId('menu-terceros'),
  selectorIdioma: porTestId('idioma-selector'),
  salir: porTestId('sesion-salir'),
  confirmarSalir: porTestId('sesion-salir-confirmacion-aceptar'),
};

/** Barra superior: usuario actual, idioma y cierre de sesión. */
export class BarraSuperior {
  /**
   * @param {import('selenium-webdriver').WebDriver} driver navegador
   */
  constructor(driver) {
    this.driver = driver;
  }

  /**
   * Cambia el idioma de la interfaz con el selector.
   *
   * @param {'es'|'en'} idioma código del idioma
   * @returns {Promise<void>}
   */
  async elegirIdioma(idioma) {
    await clic(this.driver, BARRA.selectorIdioma);
    const opcion = porTestId(`idioma-opcion-${idioma}`);
    await clic(this.driver, opcion);
    await esperarAusente(this.driver, opcion);
  }

  /**
   * Cierra la sesión confirmando el diálogo.
   *
   * @returns {Promise<void>}
   */
  async cerrarSesion() {
    await clic(this.driver, BARRA.salir);
    await clic(this.driver, BARRA.confirmarSalir);
  }
}
