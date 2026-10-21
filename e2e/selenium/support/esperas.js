/**
 * Nombre: esperas.js
 * Descripción: Localizadores por data-testid y esperas explícitas de Selenium WebDriver (visible,
 *              ausente, texto, lista de textos, URL) con mensajes de error legibles.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { By, Key, until } from 'selenium-webdriver';

/** Espera máxima por condición, igual a la de Playwright. */
export const ESPERA_MS = 10000;

/**
 * Localizador por data-testid exacto.
 *
 * @param {string} id valor del data-testid
 * @returns {import('selenium-webdriver').Locator} localizador CSS
 */
export const porTestId = (id) => By.css(`[data-testid="${id}"]`);

/**
 * Localizador por prefijo de data-testid.
 *
 * @param {string} prefijo inicio del data-testid
 * @returns {import('selenium-webdriver').Locator} localizador CSS
 */
export const porPrefijoTestId = (prefijo) => By.css(`[data-testid^="${prefijo}"]`);

/** Errores pasajeros mientras React vuelve a pintar o MUI anima un menú. */
const ERRORES_REINTENTABLES = new Set([
  'StaleElementReferenceError',
  'NoSuchElementError',
  'ElementClickInterceptedError',
  'ElementNotInteractableError',
]);

/**
 * Texto de un elemento tal como está en el DOM (textContent), sin el text-transform de CSS que sí
 * aplica getText(); así se compara igual que toHaveText de Playwright.
 *
 * @param {import('selenium-webdriver').WebElement} elemento elemento
 * @returns {Promise<string>} texto sin espacios al inicio ni al final
 */
export async function textoDe(elemento) {
  return ((await elemento.getAttribute('textContent')) ?? '').trim();
}

/**
 * Repite una condición hasta que sea verdadera; ignora elementos obsoletos por un nuevo render.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {() => Promise<boolean>} condicion condición a evaluar
 * @param {() => string} mensaje descripción del fallo
 * @param {number} [ms] espera máxima
 * @returns {Promise<void>}
 */
export async function esperarQue(driver, condicion, mensaje, ms = ESPERA_MS) {
  try {
    await driver.wait(async () => {
      try {
        return await condicion();
      } catch (error) {
        if (ERRORES_REINTENTABLES.has(error.name)) {
          return false;
        }
        throw error;
      }
    }, ms);
  } catch (error) {
    if (error.name === 'TimeoutError') {
      throw new Error(mensaje());
    }
    throw error;
  }
}

/**
 * Espera a que un elemento exista y sea visible.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elemento
 * @param {number} [ms] espera máxima
 * @returns {Promise<import('selenium-webdriver').WebElement>} elemento
 */
export async function esperarVisible(driver, localizador, ms = ESPERA_MS) {
  const elemento = await driver.wait(
    until.elementLocated(localizador),
    ms,
    `No apareció ${localizador}`,
  );
  await driver.wait(until.elementIsVisible(elemento), ms, `No se hizo visible ${localizador}`);
  return elemento;
}

/**
 * Hace clic en un elemento visible y reintenta si otro elemento lo tapa (por ejemplo, un menú de
 * MUI que todavía se está abriendo).
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elemento
 * @returns {Promise<void>}
 */
export async function clic(driver, localizador) {
  await esperarVisible(driver, localizador);
  // Selenium hace clic en coordenadas: si el elemento todavía se mueve (animación de apertura de un
  // menú), el clic puede caer en la opción vecina. Se espera a que su posición deje de cambiar.
  let anterior = null;
  await esperarQue(
    driver,
    async () => {
      const r = await driver.findElement(localizador).getRect();
      const quieto =
        anterior !== null &&
        r.x === anterior.x &&
        r.y === anterior.y &&
        r.width === anterior.width &&
        r.height === anterior.height;
      anterior = r;
      if (!quieto) await driver.sleep(50);
      return quieto;
    },
    () => `${localizador} no dejó de moverse`,
  );
  await esperarQue(
    driver,
    async () => {
      await driver.findElement(localizador).click();
      return true;
    },
    () => `No se pudo hacer clic en ${localizador}`,
  );
}

/**
 * Espera a que no exista ningún elemento con el localizador.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elemento
 * @param {number} [ms] espera máxima
 * @returns {Promise<void>}
 */
export async function esperarAusente(driver, localizador, ms = ESPERA_MS) {
  await esperarQue(
    driver,
    async () => (await driver.findElements(localizador)).length === 0,
    () => `Sigue presente ${localizador}`,
    ms,
  );
}

/**
 * Cuenta los elementos que coinciden con el localizador.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elementos
 * @returns {Promise<number>} cantidad
 */
export async function contar(driver, localizador) {
  return (await driver.findElements(localizador)).length;
}

/**
 * Espera a que haya exactamente una cantidad de elementos.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elementos
 * @param {number} cantidad cantidad esperada
 * @returns {Promise<void>}
 */
export async function esperarCantidad(driver, localizador, cantidad) {
  let ultima = -1;
  await esperarQue(
    driver,
    async () => (ultima = await contar(driver, localizador)) === cantidad,
    () => `${localizador}: se esperaban ${cantidad} elementos y hay ${ultima}`,
  );
}

/**
 * Espera a que el primer elemento tenga un texto (exacto o que cumpla una expresión regular).
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elemento
 * @param {string|RegExp} esperado texto esperado
 * @returns {Promise<void>}
 */
export async function esperarTexto(driver, localizador, esperado) {
  let ultimo;
  await esperarQue(
    driver,
    async () => {
      const [elemento] = await driver.findElements(localizador);
      if (!elemento) return false;
      ultimo = await textoDe(elemento);
      return esperado instanceof RegExp ? esperado.test(ultimo) : ultimo === esperado;
    },
    () => `${localizador}: se esperaba "${esperado}" y se obtuvo "${ultimo ?? '(no existe)'}"`,
  );
}

/**
 * Espera a que el primer elemento contenga un texto.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elemento
 * @param {string} parte texto contenido
 * @returns {Promise<void>}
 */
export async function esperarQueContenga(driver, localizador, parte) {
  let ultimo;
  await esperarQue(
    driver,
    async () => {
      const [elemento] = await driver.findElements(localizador);
      if (!elemento) return false;
      ultimo = await textoDe(elemento);
      return ultimo.includes(parte);
    },
    () =>
      `${localizador}: se esperaba que contuviera "${parte}" y tiene "${ultimo ?? '(no existe)'}"`,
  );
}

/**
 * Espera a que los textos de todos los elementos sean, en orden, los esperados.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {import('selenium-webdriver').Locator} localizador elementos
 * @param {string[]} esperados textos esperados
 * @returns {Promise<void>}
 */
export async function esperarTextos(driver, localizador, esperados) {
  let ultimos = [];
  await esperarQue(
    driver,
    async () => {
      const elementos = await driver.findElements(localizador);
      ultimos = await Promise.all(elementos.map(textoDe));
      return JSON.stringify(ultimos) === JSON.stringify(esperados);
    },
    () =>
      `${localizador}: se esperaba ${JSON.stringify(esperados)} y se obtuvo ${JSON.stringify(ultimos)}`,
  );
}

/**
 * Espera a que la URL actual cumpla una expresión regular.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {RegExp} patron patrón de la URL
 * @returns {Promise<void>}
 */
export async function esperarUrl(driver, patron) {
  let ultima;
  await esperarQue(
    driver,
    async () => patron.test((ultima = await driver.getCurrentUrl())),
    () => `La URL debía cumplir ${patron} y es ${ultima}`,
  );
}

/**
 * Reemplaza el contenido de un campo controlado por React: selecciona todo, lo borra y escribe.
 * (WebElement.clear() no dispara el evento que React necesita.)
 *
 * @param {import('selenium-webdriver').WebElement} elemento campo de texto
 * @param {string} texto texto nuevo; vacío solo borra
 * @returns {Promise<void>}
 */
export async function escribir(elemento, texto) {
  await elemento.sendKeys(Key.chord(Key.CONTROL, 'a'), Key.BACK_SPACE);
  if (texto) {
    await elemento.sendKeys(texto);
  }
}
