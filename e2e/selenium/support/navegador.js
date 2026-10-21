/**
 * Nombre: navegador.js
 * Descripción: Crea el Chrome sin interfaz gráfica de Selenium WebDriver, lo abre y cierra en cada
 *              escenario y guarda una captura de pantalla cuando un escenario falla.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { Builder } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

/** Carpeta de las capturas de los escenarios fallidos. */
export const CARPETA_CAPTURAS = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  'reportes',
  'capturas',
);

/**
 * Crea un Chrome sin interfaz (salvo con E2E_CON_INTERFAZ=1), en español de Colombia y con tamaño fijo.
 * Selenium Manager descarga el chromedriver que corresponde al Chrome instalado.
 *
 * @returns {Promise<import('selenium-webdriver').WebDriver>} navegador
 */
export async function crearNavegador() {
  const opciones = new chrome.Options().addArguments(
    '--window-size=1366,900',
    '--lang=es-CO',
    '--no-sandbox',
    '--disable-dev-shm-usage',
    '--disable-search-engine-choice-screen',
  );
  if (process.env.E2E_CON_INTERFAZ !== '1') {
    opciones.addArguments('--headless=new');
  }
  if (process.env.CHROME_BIN) {
    opciones.setChromeBinaryPath(process.env.CHROME_BIN);
  }
  return new Builder().forBrowser('chrome').setChromeOptions(opciones).build();
}

/**
 * Guarda una captura PNG del navegador con el título del escenario.
 *
 * @param {import('selenium-webdriver').WebDriver|null} driver navegador
 * @param {string} titulo título completo del escenario
 * @returns {Promise<string|null>} ruta del archivo, o null si no se pudo capturar
 */
export async function guardarCaptura(driver, titulo) {
  if (!driver) return null;
  try {
    mkdirSync(CARPETA_CAPTURAS, { recursive: true });
    const nombre = titulo.replace(/[^\p{L}\p{N}-]+/gu, '_').slice(0, 120);
    const ruta = path.join(CARPETA_CAPTURAS, `${nombre}.png`);
    writeFileSync(ruta, await driver.takeScreenshot(), 'base64');
    return ruta;
  } catch {
    return null;
  }
}

/**
 * Registra los hooks que abren un Chrome nuevo antes de cada escenario y lo cierran después.
 * Debe llamarse dentro de un describe.
 *
 * @returns {{driver: import('selenium-webdriver').WebDriver|null}} estado con el navegador actual
 */
export function usarNavegador() {
  const estado = { driver: null };
  beforeEach(async function () {
    estado.driver = await crearNavegador();
  });
  afterEach(async function () {
    await estado.driver?.quit();
    estado.driver = null;
  });
  return estado;
}

/**
 * Declara un escenario de Mocha que, si falla, guarda la captura y la adjunta al reporte JUnit.
 *
 * @param {{driver: import('selenium-webdriver').WebDriver|null}} estado estado de usarNavegador()
 * @param {string} titulo nombre visible, con el id del escenario y sus criterios
 * @param {(driver: import('selenium-webdriver').WebDriver) => Promise<void>} cuerpo pasos del escenario
 * @returns {void}
 */
export function escenario(estado, titulo, cuerpo) {
  it(titulo, async function () {
    try {
      await cuerpo(estado.driver);
    } catch (error) {
      const ruta = await guardarCaptura(estado.driver, this.test.fullTitle());
      if (ruta) {
        this.test.attachments = [ruta];
        error.message = `${error.message}\n      Captura: ${ruta}`;
      }
      throw error;
    }
  });
}
