/**
 * Nombre: PaginaProductos.js
 * Descripción: Page Object de la página de productos (/productos) para Selenium WebDriver: búsqueda,
 *              filtros, filas de la tabla, acciones de edición y confirmación de desactivación.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { By } from 'selenium-webdriver';

import { BASE_URL } from '../../comun/entorno.js';
import {
  clic,
  esperarAusente,
  esperarQue,
  esperarVisible,
  escribir,
  porPrefijoTestId,
  porTestId,
  textoDe,
} from '../support/esperas.js';
import { FormularioProducto } from './FormularioProducto.js';

/** Localizadores de la página. */
export const PRODUCTOS = {
  titulo: porTestId('productos-titulo'),
  nuevo: porTestId('productos-nuevo'),
  buscar: porTestId('productos-buscar'),
  botonBuscar: porTestId('productos-buscar-boton'),
  tabla: porTestId('productos-tabla'),
  progreso: By.css('main [role="progressbar"]'),
  filas: porPrefijoTestId('producto-fila-'),
  nombres: porPrefijoTestId('producto-nombre-'),
  sinResultados: porTestId('productos-sin-resultados'),
  columnaCosto: porTestId('productos-columna-costo'),
  costos: porPrefijoTestId('producto-costo-'),
  botonesEditar: porPrefijoTestId('producto-editar-'),
  botonesDesactivar: By.css(
    '[data-testid^="producto-desactivar-"]:not([data-testid^="producto-desactivar-confirmacion"])',
  ),
  dialogoDesactivar: porTestId('producto-desactivar-confirmacion-dialogo'),
  notificacion: porTestId('notificacion'),
  listado: By.css('ul[role="listbox"]'),
  /**
   * Elemento de una fila identificado por el id del producto.
   *
   * @param {string} parte nombre, estado, costo, categoria, editar o desactivar
   * @param {string} id id del producto
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  deProducto: (parte, id) => porTestId(`producto-${parte}-${id}`),
};

/** Página de gestión de productos. */
export class PaginaProductos {
  /**
   * @param {import('selenium-webdriver').WebDriver} driver navegador
   */
  constructor(driver) {
    this.driver = driver;
    this.formulario = new FormularioProducto(driver);
  }

  /**
   * Abre la página y espera la primera carga.
   *
   * @returns {Promise<void>}
   */
  async abrir() {
    await this.driver.get(`${BASE_URL}/productos`);
    await this.esperarCarga();
  }

  /**
   * Espera a que la tabla no muestre la barra de progreso.
   *
   * @returns {Promise<void>}
   */
  async esperarCarga() {
    await esperarVisible(this.driver, PRODUCTOS.tabla);
    await esperarAusente(this.driver, PRODUCTOS.progreso);
  }

  /**
   * Busca por nombre desde la primera página.
   *
   * @param {string} texto texto a buscar
   * @returns {Promise<void>}
   */
  async buscar(texto) {
    await escribir(await esperarVisible(this.driver, PRODUCTOS.buscar), texto);
    await this.driver.findElement(PRODUCTOS.botonBuscar).click();
    await this.esperarCarga();
  }

  /**
   * Elige una opción de un filtro (categoría o estado).
   *
   * @param {'categoria'|'estado'} filtro filtro a cambiar
   * @param {string} texto texto visible de la opción
   * @returns {Promise<void>}
   */
  async filtrar(filtro, texto) {
    await clic(this.driver, porTestId(`productos-filtro-${filtro}`));
    const opcion = By.xpath(
      `//ul[@role="listbox"]/li[@role="option" and normalize-space(.)="${texto}"]`,
    );
    await clic(this.driver, opcion);
    await esperarAusente(this.driver, PRODUCTOS.listado);
    await this.esperarCarga();
  }

  /**
   * Espera la fila con un nombre exacto y devuelve el id del producto (estable entre renders).
   *
   * @param {string} nombre nombre del producto
   * @returns {Promise<string>} id del producto
   */
  async idDe(nombre) {
    let id = null;
    await esperarQue(
      this.driver,
      async () => {
        for (const celda of await this.driver.findElements(PRODUCTOS.nombres)) {
          if ((await textoDe(celda)) === nombre) {
            id = (await celda.getAttribute('data-testid')).replace('producto-nombre-', '');
            return true;
          }
        }
        return false;
      },
      () => `No aparece en la tabla el producto "${nombre}"`,
    );
    return id;
  }

  /**
   * Abre el formulario para crear un producto.
   *
   * @returns {Promise<FormularioProducto>} formulario abierto
   */
  async nuevo() {
    await clic(this.driver, PRODUCTOS.nuevo);
    await this.formulario.esperarAbierto();
    return this.formulario;
  }

  /**
   * Abre el formulario para editar un producto.
   *
   * @param {string} id id del producto
   * @returns {Promise<FormularioProducto>} formulario abierto
   */
  async editar(id) {
    await clic(this.driver, PRODUCTOS.deProducto('editar', id));
    await this.formulario.esperarAbierto();
    return this.formulario;
  }

  /**
   * Pulsa Desactivar en una fila (abre la confirmación).
   *
   * @param {string} id id del producto
   * @returns {Promise<void>}
   */
  async pedirDesactivar(id) {
    await clic(this.driver, PRODUCTOS.deProducto('desactivar', id));
    await esperarVisible(this.driver, PRODUCTOS.dialogoDesactivar);
  }

  /**
   * Responde el diálogo de desactivación.
   *
   * @param {boolean} aceptar true confirma; false cancela
   * @returns {Promise<void>}
   */
  async responderDesactivar(aceptar) {
    const boton = porTestId(`producto-desactivar-confirmacion-${aceptar ? 'aceptar' : 'cancelar'}`);
    await clic(this.driver, boton);
    await esperarAusente(this.driver, PRODUCTOS.dialogoDesactivar);
  }
}
