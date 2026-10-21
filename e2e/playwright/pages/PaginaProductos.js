/**
 * Nombre: PaginaProductos.js
 * Descripción: Page Object de la página de productos (/productos) para Playwright: búsqueda, filtros,
 *              filas de la tabla, acciones de edición y confirmación de desactivación.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { expect } from '@playwright/test';

import { FormularioProducto } from './FormularioProducto.js';

/** Página de gestión de productos. */
export class PaginaProductos {
  /**
   * @param {import('@playwright/test').Page} page página del navegador
   */
  constructor(page) {
    this.page = page;
    this.titulo = page.getByTestId('productos-titulo');
    this.botonNuevo = page.getByTestId('productos-nuevo');
    this.campoBuscar = page.getByTestId('productos-buscar');
    this.botonBuscar = page.getByTestId('productos-buscar-boton');
    this.tabla = page.getByTestId('productos-tabla');
    this.filas = page.getByTestId(/^producto-fila-/);
    /** Celdas de nombre en el orden de la tabla; con toHaveText([...]) la comprobación reintenta. */
    this.nombres = page.getByTestId(/^producto-nombre-/);
    this.sinResultados = page.getByTestId('productos-sin-resultados');
    this.columnaCosto = page.getByTestId('productos-columna-costo');
    this.botonesEditar = page.getByTestId(/^producto-editar-/);
    this.botonesDesactivar = page.getByTestId(/^producto-desactivar-\d+$/);
    this.dialogoDesactivar = page.getByTestId('producto-desactivar-confirmacion-dialogo');
    this.notificacion = page.getByTestId('notificacion');
    this.formulario = new FormularioProducto(page);
  }

  /**
   * Abre la página y espera a que termine la primera carga.
   *
   * @returns {Promise<void>}
   */
  async abrir() {
    await this.page.goto('/productos');
    await this.esperarCarga();
  }

  /**
   * Espera a que la tabla no muestre la barra de progreso.
   *
   * @returns {Promise<void>}
   */
  async esperarCarga() {
    await expect(this.tabla).toBeVisible();
    await expect(this.page.locator('main').getByRole('progressbar')).toHaveCount(0);
  }

  /**
   * Busca por nombre desde la primera página.
   *
   * @param {string} texto texto a buscar
   * @returns {Promise<void>}
   */
  async buscar(texto) {
    await this.campoBuscar.fill(texto);
    await this.botonBuscar.click();
    await this.esperarCarga();
  }

  /**
   * Elige una opción de un filtro (categoría o estado).
   *
   * @param {'categoria'|'estado'} filtro filtro a cambiar
   * @param {string} opcion texto visible de la opción
   * @returns {Promise<void>}
   */
  async filtrar(filtro, opcion) {
    await this.page.getByTestId(`productos-filtro-${filtro}`).click();
    const listado = this.page.getByRole('listbox');
    await listado.getByRole('option', { name: opcion, exact: true }).click();
    await expect(listado).toBeHidden();
    await this.esperarCarga();
  }

  /**
   * Fila de la tabla que contiene un nombre.
   *
   * @param {string} nombre nombre del producto
   * @returns {import('@playwright/test').Locator} fila
   */
  fila(nombre) {
    return this.filas.filter({
      has: this.page.getByTestId(/^producto-nombre-/).getByText(nombre, { exact: true }),
    });
  }

  /**
   * Chip de estado de una fila.
   *
   * @param {string} nombre nombre del producto
   * @returns {import('@playwright/test').Locator} chip
   */
  estado(nombre) {
    return this.fila(nombre).getByTestId(/^producto-estado-/);
  }

  /**
   * Abre el formulario para crear un producto.
   *
   * @returns {Promise<FormularioProducto>} formulario abierto
   */
  async nuevo() {
    await this.botonNuevo.click();
    await this.formulario.esperarAbierto();
    return this.formulario;
  }

  /**
   * Abre el formulario para editar el producto de una fila.
   *
   * @param {string} nombre nombre del producto
   * @returns {Promise<FormularioProducto>} formulario abierto
   */
  async editar(nombre) {
    await this.fila(nombre)
      .getByTestId(/^producto-editar-/)
      .click();
    await this.formulario.esperarAbierto();
    return this.formulario;
  }

  /**
   * Pulsa Desactivar en una fila (abre la confirmación).
   *
   * @param {string} nombre nombre del producto
   * @returns {Promise<void>}
   */
  async pedirDesactivar(nombre) {
    await this.fila(nombre)
      .getByTestId(/^producto-desactivar-\d+$/)
      .click();
    await expect(this.dialogoDesactivar).toBeVisible();
  }

  /**
   * Responde el diálogo de desactivación.
   *
   * @param {boolean} aceptar true confirma; false cancela
   * @returns {Promise<void>}
   */
  async responderDesactivar(aceptar) {
    await this.page
      .getByTestId(`producto-desactivar-confirmacion-${aceptar ? 'aceptar' : 'cancelar'}`)
      .click();
    await expect(this.dialogoDesactivar).toBeHidden();
  }
}
