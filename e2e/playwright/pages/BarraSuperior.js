/**
 * Nombre: BarraSuperior.js
 * Descripción: Page Object de la barra superior de las páginas con sesión y del selector de idioma
 *              (que también aparece en el login) para Playwright.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */

/** Barra superior: menú, usuario actual, idioma y cierre de sesión. */
export class BarraSuperior {
  /**
   * @param {import('@playwright/test').Page} page página del navegador
   */
  constructor(page) {
    this.page = page;
    this.usuarioActual = page.getByTestId('usuario-actual');
    this.menuProductos = page.getByTestId('menu-productos');
    /** Entrada del menú que solo ve quien tiene CONFIG_GESTIONAR. */
    this.menuCatalogos = page.getByTestId('menu-catalogos');
    /** Entrada del menú que ve quien tiene TERCERO_CLIENTE_EDITAR o TERCERO_PROVEEDOR_EDITAR. */
    this.menuTerceros = page.getByTestId('menu-terceros');
    this.selectorIdioma = page.getByTestId('idioma-selector');
    this.botonSalir = page.getByTestId('sesion-salir');
  }

  /**
   * Cambia el idioma de la interfaz con el selector.
   *
   * @param {'es'|'en'} idioma código del idioma
   * @returns {Promise<void>}
   */
  async elegirIdioma(idioma) {
    await this.selectorIdioma.click();
    await this.page.getByTestId(`idioma-opcion-${idioma}`).click();
    await this.page.getByTestId(`idioma-opcion-${idioma}`).waitFor({ state: 'hidden' });
  }

  /**
   * Cierra la sesión confirmando el diálogo.
   *
   * @returns {Promise<void>}
   */
  async cerrarSesion() {
    await this.botonSalir.click();
    await this.page.getByTestId('sesion-salir-confirmacion-aceptar').click();
  }
}
