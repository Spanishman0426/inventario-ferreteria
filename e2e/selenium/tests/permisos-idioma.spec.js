/**
 * Nombre: permisos-idioma.spec.js
 * Descripción: Escenarios E2E-08 (el vendedor no ve acciones de edición) y E2E-10 (cambio de idioma de
 *              la interfaz) con Selenium WebDriver y Mocha.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import assert from 'node:assert/strict';

import { By } from 'selenium-webdriver';

import { estadoDe, iniciarSesionApi } from '../../comun/api.js';
import { MARTILLO, TEXTOS, nombreUnico, peticionProducto } from '../../comun/datos.js';
import { BARRA, BarraSuperior } from '../pages/BarraSuperior.js';
import { LOGIN, PaginaLogin } from '../pages/PaginaLogin.js';
import { PRODUCTOS, PaginaProductos } from '../pages/PaginaProductos.js';
import { contar, esperarQue, esperarTexto, esperarVisible } from '../support/esperas.js';
import { escenario, usarNavegador } from '../support/navegador.js';

describe('Permisos por rol', function () {
  const navegador = usarNavegador();

  escenario(
    navegador,
    'E2E-08 [CA-RF-SEG-001-2] [CA-RF-PRO-003-3] el vendedor no ve acciones de edición',
    async (driver) => {
      await new PaginaLogin(driver).iniciarSesionComo('vendedor');
      const productos = new PaginaProductos(driver);
      await productos.esperarCarga();

      // Consulta el catálogo sin botones de crear, editar ni desactivar.
      await productos.buscar(MARTILLO.nombre);
      const id = await productos.idDe(MARTILLO.nombre);
      assert.equal(await contar(driver, PRODUCTOS.nuevo), 0, 'no debe ver "Nuevo producto"');
      assert.equal(
        await contar(driver, PRODUCTOS.botonesEditar),
        0,
        'no debe ver botones de editar',
      );
      assert.equal(
        await contar(driver, PRODUCTOS.botonesDesactivar),
        0,
        'no debe ver botones de desactivar',
      );

      // No ve la columna de costos.
      assert.equal(
        await contar(driver, PRODUCTOS.columnaCosto),
        0,
        'no debe ver la columna de costos',
      );
      assert.equal(
        await contar(driver, PRODUCTOS.deProducto('costo', id)),
        0,
        'no debe ver el costo',
      );

      // La API también le niega crear y desactivar (403).
      const token = await iniciarSesionApi('vendedor');
      const body = peticionProducto(nombreUnico('SE', 'Prohibido'));
      assert.equal(await estadoDe('/api/v1/productos', { method: 'POST', token, body }), 403);
      assert.equal(await estadoDe('/api/v1/productos/1', { method: 'DELETE', token }), 403);
    },
  );
});

describe('Internacionalización', function () {
  const navegador = usarNavegador();

  escenario(
    navegador,
    'E2E-10 [CA-RNF-INT-003-D1] [CA-RNF-INT-003-1] cambio de idioma de la interfaz',
    async (driver) => {
      const login = new PaginaLogin(driver);
      const barra = new BarraSuperior(driver);
      const productos = new PaginaProductos(driver);
      const idiomaDocumento = async () => driver.findElement(By.css('html')).getAttribute('lang');

      // La interfaz arranca en español.
      await login.abrir();
      await esperarTexto(driver, LOGIN.titulo, TEXTOS.es.loginTitulo);
      assert.equal(await idiomaDocumento(), 'es');

      // Moneda con formato colombiano.
      await login.iniciarSesionComo('admin');
      await productos.buscar(MARTILLO.nombre);
      const id = await productos.idDe(MARTILLO.nombre);
      await esperarTexto(driver, PRODUCTOS.deProducto('costo', id), MARTILLO.costoFormateado);

      // Al elegir inglés cambian los textos.
      await barra.elegirIdioma('en');
      await esperarTexto(driver, PRODUCTOS.titulo, TEXTOS.en.productosTitulo);
      await esperarTexto(driver, PRODUCTOS.nuevo, TEXTOS.en.nuevoProducto);
      await esperarTexto(driver, BARRA.menuProductos, TEXTOS.en.menuProductos);
      await esperarTexto(driver, PRODUCTOS.deProducto('estado', id), TEXTOS.en.estadoActivo);
      await esperarQue(
        driver,
        async () => (await idiomaDocumento()) === 'en',
        () => 'lang debía ser en',
      );

      // La elección se conserva al recargar y al cerrar sesión.
      await driver.navigate().refresh();
      await esperarTexto(driver, PRODUCTOS.titulo, TEXTOS.en.productosTitulo);
      await barra.cerrarSesion();
      await esperarTexto(driver, LOGIN.titulo, TEXTOS.en.loginTitulo);

      // Se puede volver a español.
      await barra.elegirIdioma('es');
      await esperarTexto(driver, LOGIN.titulo, TEXTOS.es.loginTitulo);
      await esperarVisible(driver, LOGIN.entrar);
    },
  );
});
