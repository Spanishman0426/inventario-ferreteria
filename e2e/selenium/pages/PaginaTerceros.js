/**
 * Nombre: PaginaTerceros.js
 * Descripción: Page Object de clientes y proveedores (/terceros) para Selenium WebDriver: búsqueda, filas
 *              de la tabla y formulario dinámico de tercero (tipo de persona, identificación con dígito de
 *              verificación, nombres o razón social, contacto y roles).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { By } from 'selenium-webdriver';

import {
  clic,
  esperarAusente,
  esperarQue,
  esperarQueContenga,
  esperarVisible,
  escribir,
  porTestId,
  textoDe,
} from '../support/esperas.js';

/** Localizadores de la página y del formulario. */
export const TERCEROS = {
  titulo: porTestId('terceros-titulo'),
  tabla: porTestId('terceros-tabla'),
  progreso: By.css('main [role="progressbar"]'),
  buscar: porTestId('terceros-buscar'),
  botonBuscar: porTestId('terceros-buscar-boton'),
  nuevo: porTestId('terceros-nuevo'),
  notificacion: porTestId('notificacion'),
  sinResultados: porTestId('terceros-sin-resultados'),
  formulario: porTestId('tercero-form'),
  guardar: porTestId('tercero-guardar'),
  cancelar: porTestId('tercero-cancelar'),
  soloLectura: porTestId('tercero-solo-lectura'),
  avisoRol: porTestId('tercero-aviso-rol'),
  tipoPersona: porTestId('tercero-campo-tipo-persona'),
  tipoIdentificacion: porTestId('tercero-campo-tipo-identificacion'),
  listado: By.css('ul[role="listbox"]'),
  /**
   * Campo del formulario (también las casillas es-cliente y es-proveedor).
   *
   * @param {string} campo sufijo del data-testid
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  campo: (campo) => porTestId(`tercero-campo-${campo}`),
  /**
   * Etiqueta clicable de una casilla de rol.
   *
   * @param {'es-cliente'|'es-proveedor'} rol casilla
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  opcionRol: (rol) => porTestId(`tercero-opcion-${rol}`),
  /**
   * Mensaje de error bajo un campo.
   *
   * @param {string} campo sufijo del data-testid
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  error: (campo) => porTestId(`tercero-error-${campo}`),
  /**
   * Elemento de una fila (identificacion, nombre, predefinido, editar, ver, desactivar...).
   *
   * @param {string} parte parte de la fila
   * @param {number|string} id id del tercero
   * @returns {import('selenium-webdriver').Locator} localizador
   */
  deFila: (parte, id) => porTestId(`tercero-${parte}-${id}`),
};

/** Página de terceros con su formulario. */
export class PaginaTerceros {
  /**
   * @param {import('selenium-webdriver').WebDriver} driver navegador
   */
  constructor(driver) {
    this.driver = driver;
  }

  /**
   * Espera a que la tabla no muestre la barra de progreso.
   *
   * @returns {Promise<void>}
   */
  async esperarCarga() {
    await esperarVisible(this.driver, TERCEROS.tabla);
    await esperarAusente(this.driver, TERCEROS.progreso);
  }

  /**
   * Busca por nombre, razón social o identificación.
   *
   * @param {string} texto texto a buscar
   * @returns {Promise<void>}
   */
  async buscar(texto) {
    await escribir(await esperarVisible(this.driver, TERCEROS.buscar), texto);
    await clic(this.driver, TERCEROS.botonBuscar);
    await this.esperarCarga();
  }

  /**
   * Abre el formulario de un tercero nuevo.
   *
   * @returns {Promise<void>}
   */
  async nuevo() {
    await clic(this.driver, TERCEROS.nuevo);
    await esperarVisible(this.driver, TERCEROS.formulario);
  }

  /**
   * Escribe en un campo de texto del formulario.
   *
   * @param {string} campo sufijo del data-testid
   * @param {string} texto texto nuevo
   * @returns {Promise<void>}
   */
  async escribirCampo(campo, texto) {
    await escribir(await esperarVisible(this.driver, TERCEROS.campo(campo)), texto);
  }

  /**
   * Elige el tipo de persona en el selector.
   *
   * @param {string} texto texto visible de la opción
   * @returns {Promise<void>}
   */
  async elegirTipoPersona(texto) {
    await clic(this.driver, TERCEROS.tipoPersona);
    await clic(
      this.driver,
      By.xpath(`//ul[@role="listbox"]/li[@role="option" and normalize-space(.)="${texto}"]`),
    );
    await esperarAusente(this.driver, TERCEROS.listado);
    await esperarQueContenga(this.driver, TERCEROS.tipoPersona, texto);
  }

  /**
   * Llena dirección y teléfono.
   *
   * @returns {Promise<void>}
   */
  async llenarContacto() {
    await this.escribirCampo('direccion', 'Carrera 5 # 10-30, Neiva');
    await this.escribirCampo('telefono', '3001234567');
  }

  /**
   * Valor actual de un campo de texto.
   *
   * @param {string} campo sufijo del data-testid
   * @returns {Promise<string>} valor
   */
  async valorDe(campo) {
    return (await this.driver.findElement(TERCEROS.campo(campo))).getAttribute('value');
  }

  /**
   * Casilla de rol (el input real, que MUI dibuja transparente): sirve para leer su estado.
   *
   * @param {'es-cliente'|'es-proveedor'} rol casilla
   * @returns {Promise<import('selenium-webdriver').WebElement>} input de la casilla
   */
  async casilla(rol) {
    await esperarVisible(this.driver, TERCEROS.opcionRol(rol));
    return this.driver.findElement(TERCEROS.campo(rol));
  }

  /**
   * Espera la fila que contiene un texto y devuelve el id del tercero.
   *
   * @param {string} texto texto de la identificación (por ejemplo el número)
   * @returns {Promise<string>} id del tercero
   */
  async idDe(texto) {
    const prefijo = 'tercero-identificacion-';
    let id = null;
    await esperarQue(
      this.driver,
      async () => {
        for (const celda of await this.driver.findElements(By.css(`[data-testid^="${prefijo}"]`))) {
          if ((await textoDe(celda)).includes(texto)) {
            id = (await celda.getAttribute('data-testid')).replace(prefijo, '');
            return true;
          }
        }
        return false;
      },
      () => `No aparece en la tabla un tercero con "${texto}"`,
    );
    return id;
  }
}
