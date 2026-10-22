/**
 * Nombre: PaginaTerceros.js
 * Descripción: Page Object de clientes y proveedores (/terceros) para Playwright: búsqueda, filas de la
 *              tabla y formulario dinámico de tercero (tipo de persona, identificación con dígito de
 *              verificación, nombres o razón social, contacto y roles).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { expect } from '@playwright/test';

/** Página de terceros con su formulario. */
export class PaginaTerceros {
  /**
   * @param {import('@playwright/test').Page} page página del navegador
   */
  constructor(page) {
    this.page = page;
    this.titulo = page.getByTestId('terceros-titulo');
    this.tabla = page.getByTestId('terceros-tabla');
    this.notificacion = page.getByTestId('notificacion');
    this.formulario = page.getByTestId('tercero-form');
    this.botonGuardar = page.getByTestId('tercero-guardar');
    this.botonCancelar = page.getByTestId('tercero-cancelar');
    this.soloLectura = page.getByTestId('tercero-solo-lectura');
    this.casillaCliente = page.getByTestId('tercero-campo-es-cliente');
    this.casillaProveedor = page.getByTestId('tercero-campo-es-proveedor');
    this.avisoRol = page.getByTestId('tercero-aviso-rol');
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
   * Busca por nombre, razón social o identificación.
   *
   * @param {string} texto texto a buscar
   * @returns {Promise<void>}
   */
  async buscar(texto) {
    await this.page.getByTestId('terceros-buscar').fill(texto);
    await this.page.getByTestId('terceros-buscar-boton').click();
    await this.esperarCarga();
  }

  /**
   * Abre el formulario de un tercero nuevo.
   *
   * @returns {Promise<void>}
   */
  async nuevo() {
    await this.page.getByTestId('terceros-nuevo').click();
    await expect(this.formulario).toBeVisible();
  }

  /**
   * Campo de texto del formulario.
   *
   * @param {'numero'|'dv'|'nombre'|'apellido'|'razon-social'|'direccion'|'telefono'|'correo'} campo sufijo del data-testid
   * @returns {import('@playwright/test').Locator} campo
   */
  campo(campo) {
    return this.page.getByTestId(`tercero-campo-${campo}`);
  }

  /**
   * Mensaje de error bajo un campo.
   *
   * @param {string} campo sufijo del data-testid
   * @returns {import('@playwright/test').Locator} mensaje
   */
  error(campo) {
    return this.page.getByTestId(`tercero-error-${campo}`);
  }

  /**
   * Elige el tipo de persona en el selector.
   *
   * @param {string} texto texto visible de la opción (por ejemplo «Persona jurídica»)
   * @returns {Promise<void>}
   */
  async elegirTipoPersona(texto) {
    await this.page.getByTestId('tercero-campo-tipo-persona').click();
    await this.page.getByRole('listbox').getByRole('option', { name: texto, exact: true }).click();
    await expect(this.page.getByRole('listbox')).toBeHidden();
  }

  /**
   * Pulsa la etiqueta de una casilla de rol.
   *
   * @param {'es-cliente'|'es-proveedor'} rol casilla
   * @returns {Promise<void>}
   */
  async marcarRol(rol) {
    await this.page.getByTestId(`tercero-opcion-${rol}`).click();
  }

  /**
   * Llena dirección y teléfono.
   *
   * @returns {Promise<void>}
   */
  async llenarContacto() {
    await this.campo('direccion').fill('Carrera 5 # 10-30, Neiva');
    await this.campo('telefono').fill('3001234567');
  }

  /**
   * Fila de la tabla que contiene un texto (por ejemplo, el número de identificación).
   *
   * @param {string} texto texto de la fila
   * @returns {import('@playwright/test').Locator} fila
   */
  fila(texto) {
    return this.page.getByTestId(/^tercero-fila-\d+$/).filter({ hasText: texto });
  }

  /**
   * Elemento de una fila por id (identificacion, nombre, estado, editar, ver, desactivar, rol-cliente...).
   *
   * @param {string} parte parte de la fila
   * @param {number} id id del tercero
   * @returns {import('@playwright/test').Locator} elemento
   */
  deFila(parte, id) {
    return this.page.getByTestId(`tercero-${parte}-${id}`);
  }
}
