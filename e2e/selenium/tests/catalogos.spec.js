/**
 * Nombre: catalogos.spec.js
 * Descripción: Escenarios E2E-11 (el administrador crea una marca, aparece en el formulario de producto y
 *              al desactivarla deja de aparecer) y E2E-12 (el vendedor no ve el menú Catálogos) con
 *              Selenium WebDriver y Mocha.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import assert from 'node:assert/strict';

import { By, Key } from 'selenium-webdriver';

import { estadoDe, iniciarSesionApi } from '../../comun/api.js';
import { BASE_URL } from '../../comun/entorno.js';
import { TEXTOS, nombreMarcaUnico } from '../../comun/datos.js';
import { BARRA } from '../pages/BarraSuperior.js';
import { FORMULARIO } from '../pages/FormularioProducto.js';
import { PaginaLogin } from '../pages/PaginaLogin.js';
import { CATALOGOS, PaginaCatalogos } from '../pages/PaginaCatalogos.js';
import { PaginaProductos } from '../pages/PaginaProductos.js';
import { clic, contar, esperarAusente, esperarTexto, esperarVisible } from '../support/esperas.js';
import { escenario, usarNavegador } from '../support/navegador.js';

/** Prefijo de las marcas que crea esta herramienta. */
const HERRAMIENTA = 'SE';

/**
 * Abre el selector de marca de un producto nuevo y cuenta las opciones con un nombre exacto.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {string} marca nombre de la marca
 * @returns {Promise<number>} cantidad de opciones con ese nombre
 */
async function opcionesDeMarcaAlCrear(driver, marca) {
  await clic(driver, BARRA.menuProductos);
  const productos = new PaginaProductos(driver);
  await productos.esperarCarga();
  await productos.nuevo();
  await clic(driver, FORMULARIO.campo('marca'));
  await esperarVisible(driver, FORMULARIO.listado);
  const cantidad = await contar(
    driver,
    By.xpath(`//ul[@role="listbox"]/li[@role="option" and normalize-space(.)="${marca}"]`),
  );
  await driver.actions().sendKeys(Key.ESCAPE).perform();
  await esperarAusente(driver, FORMULARIO.listado);
  await clic(driver, FORMULARIO.cancelar);
  await esperarAusente(driver, FORMULARIO.formulario);
  return cantidad;
}

describe('Administración de catálogos', function () {
  const navegador = usarNavegador();

  escenario(
    navegador,
    'E2E-11 [CA-RF-CFG-001-3] [CA-RF-CFG-001-D1] el administrador crea una marca, aparece en el formulario de producto y al desactivarla deja de aparecer',
    async (driver) => {
      const marca = nombreMarcaUnico(HERRAMIENTA);
      await new PaginaLogin(driver).iniciarSesionComo('admin');
      await new PaginaProductos(driver).esperarCarga();
      const catalogos = new PaginaCatalogos(driver);

      // Crea la marca desde el menú Catálogos.
      await clic(driver, BARRA.menuCatalogos);
      await esperarTexto(driver, CATALOGOS.titulo, TEXTOS.es.catalogos.titulo);
      await catalogos.esperarCarga('marcas');
      await catalogos.crear('marcas', marca);
      await esperarTexto(driver, CATALOGOS.notificacion, TEXTOS.es.catalogos.marcaCreada(marca));
      await catalogos.buscar('marcas', marca);
      const id = await catalogos.idDe('marcas', marca);
      await esperarTexto(
        driver,
        CATALOGOS.deElemento('marcas', 'estado', id),
        TEXTOS.es.estadoActivo,
      );

      // La marca aparece en el selector del formulario de producto.
      assert.equal(await opcionesDeMarcaAlCrear(driver, marca), 1, 'la marca nueva debe ofrecerse');

      // Desactivar: cancelar no cambia nada; confirmar la deja Inactiva.
      await clic(driver, BARRA.menuCatalogos);
      await catalogos.esperarCarga('marcas');
      await catalogos.buscar('marcas', marca);
      await catalogos.desactivar('marcas', id, false);
      await esperarTexto(
        driver,
        CATALOGOS.deElemento('marcas', 'estado', id),
        TEXTOS.es.estadoActivo,
      );
      await catalogos.desactivar('marcas', id, true);
      await esperarTexto(
        driver,
        CATALOGOS.notificacion,
        TEXTOS.es.catalogos.marcaDesactivada(marca),
      );
      await esperarTexto(
        driver,
        CATALOGOS.deElemento('marcas', 'estado', id),
        TEXTOS.es.estadoInactivo,
      );

      // Sigue registrada, pero ya no aparece al crear un producto.
      assert.equal(
        await opcionesDeMarcaAlCrear(driver, marca),
        0,
        'la marca desactivada no debe ofrecerse en productos nuevos',
      );
    },
  );

  escenario(
    navegador,
    'E2E-12 [CA-RF-SEG-001-2] el vendedor no ve el menú Catálogos ni puede administrarlos',
    async (driver) => {
      await new PaginaLogin(driver).iniciarSesionComo('vendedor');
      await new PaginaProductos(driver).esperarCarga();

      // El menú muestra Productos pero no Catálogos.
      await esperarVisible(driver, BARRA.menuProductos);
      assert.equal(await contar(driver, BARRA.menuCatalogos), 0, 'no debe ver el menú Catálogos');

      // Abrir /catalogos directamente muestra acceso denegado.
      await driver.get(`${BASE_URL}/catalogos`);
      await esperarVisible(driver, CATALOGOS.accesoDenegado);
      assert.equal(await contar(driver, CATALOGOS.titulo), 0, 'no debe ver la página de catálogos');

      // La API le niega crear y desactivar marcas (403).
      const token = await iniciarSesionApi('vendedor');
      const body = { nombre: nombreMarcaUnico(HERRAMIENTA) };
      assert.equal(await estadoDe('/api/v1/marcas', { method: 'POST', token, body }), 403);
      assert.equal(
        await estadoDe('/api/v1/marcas/1/estado', {
          method: 'PATCH',
          token,
          body: { estado: 'INACTIVO' },
        }),
        403,
      );
    },
  );
});
