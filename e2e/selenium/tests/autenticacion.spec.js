/**
 * Nombre: autenticacion.spec.js
 * Descripción: Escenarios E2E-01 (inicio de sesión correcto e incorrecto) y E2E-09 (ruta protegida sin
 *              sesión) con Selenium WebDriver y Mocha.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import assert from 'node:assert/strict';

import { estadoDe } from '../../comun/api.js';
import { BASE_URL, USUARIOS, contrasenaDe } from '../../comun/entorno.js';
import { TEXTOS } from '../../comun/datos.js';
import { BARRA } from '../pages/BarraSuperior.js';
import { LOGIN, PaginaLogin } from '../pages/PaginaLogin.js';
import { PRODUCTOS, PaginaProductos } from '../pages/PaginaProductos.js';
import { contar, esperarTexto, esperarUrl } from '../support/esperas.js';
import { escenario, usarNavegador } from '../support/navegador.js';

describe('Autenticación', function () {
  const navegador = usarNavegador();

  escenario(
    navegador,
    'E2E-01 [CA-RF-SEG-001-D1] inicio de sesión correcto e incorrecto',
    async (driver) => {
      const login = new PaginaLogin(driver);
      await login.abrir();

      // Con contraseña incorrecta muestra el aviso y no entra.
      await login.iniciarSesion(USUARIOS.jefe.usuario, 'contrasena-incorrecta-1');
      await esperarTexto(driver, LOGIN.aviso, TEXTOS.es.credencialesInvalidas);
      await esperarUrl(driver, /\/login$/);
      assert.equal(await contar(driver, BARRA.usuarioActual), 0, 'no debe haber sesión');

      // Con credenciales válidas entra a productos con su nombre.
      await login.iniciarSesion(USUARIOS.jefe.usuario, contrasenaDe('jefe'));
      await esperarUrl(driver, /\/productos$/);
      await esperarTexto(driver, BARRA.usuarioActual, USUARIOS.jefe.nombreCompleto);
      await esperarTexto(driver, PRODUCTOS.titulo, TEXTOS.es.productosTitulo);
    },
  );

  escenario(
    navegador,
    'E2E-09 [CA-RNF-SEG-001-D1] [CA-RNF-SEG-001-1] una ruta protegida sin sesión redirige al login',
    async (driver) => {
      // La página de productos sin sesión lleva al login.
      await driver.get(`${BASE_URL}/productos`);
      await esperarUrl(driver, /\/login$/);
      await esperarTexto(driver, LOGIN.titulo, TEXTOS.es.loginTitulo);
      assert.equal(
        await contar(driver, PRODUCTOS.tabla),
        0,
        'la tabla no debe mostrarse sin sesión',
      );

      // La API protegida responde 401 sin token.
      assert.equal(await estadoDe('/api/v1/productos'), 401);

      // Tras iniciar sesión muestra la página pedida.
      await new PaginaLogin(driver).iniciarSesion(
        USUARIOS.vendedor.usuario,
        contrasenaDe('vendedor'),
      );
      await esperarUrl(driver, /\/productos$/);
      await new PaginaProductos(driver).esperarCarga();
    },
  );
});
