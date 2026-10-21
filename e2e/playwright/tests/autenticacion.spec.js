/**
 * Nombre: autenticacion.spec.js
 * Descripción: Escenarios E2E-01 (inicio de sesión correcto e incorrecto) y E2E-09 (ruta protegida sin
 *              sesión) con Playwright.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { estadoDe } from '../../comun/api.js';
import { USUARIOS, contrasenaDe } from '../../comun/entorno.js';
import { TEXTOS } from '../../comun/datos.js';
import { expect, test } from './fixtures.js';

test.describe('Autenticación', () => {
  test('E2E-01 [CA-RF-SEG-001-D1] inicio de sesión correcto e incorrecto', async ({
    page,
    paginaLogin,
    barra,
  }) => {
    await paginaLogin.abrir();

    await test.step('con contraseña incorrecta muestra el aviso y no entra', async () => {
      await paginaLogin.iniciarSesion(USUARIOS.jefe.usuario, 'contrasena-incorrecta-1');
      await expect(paginaLogin.aviso).toHaveText(TEXTOS.es.credencialesInvalidas);
      await expect(page).toHaveURL(/\/login$/);
      await expect(barra.usuarioActual).toHaveCount(0);
    });

    await test.step('con credenciales válidas entra a productos con su nombre', async () => {
      await paginaLogin.iniciarSesion(USUARIOS.jefe.usuario, contrasenaDe('jefe'));
      await expect(page).toHaveURL(/\/productos$/);
      await expect(barra.usuarioActual).toHaveText(USUARIOS.jefe.nombreCompleto);
      await expect(page.getByTestId('productos-titulo')).toHaveText(TEXTOS.es.productosTitulo);
    });
  });

  test('E2E-09 [CA-RNF-SEG-001-D1] [CA-RNF-SEG-001-1] una ruta protegida sin sesión redirige al login', async ({
    page,
    paginaLogin,
    paginaProductos,
  }) => {
    await test.step('la página de productos sin sesión lleva al login', async () => {
      await page.goto('/productos');
      await expect(page).toHaveURL(/\/login$/);
      await expect(paginaLogin.titulo).toHaveText(TEXTOS.es.loginTitulo);
      await expect(paginaProductos.tabla).toHaveCount(0);
    });

    await test.step('la API protegida responde 401 sin token', async () => {
      expect(await estadoDe('/api/v1/productos')).toBe(401);
    });

    await test.step('tras iniciar sesión vuelve a la página pedida', async () => {
      await paginaLogin.iniciarSesion(USUARIOS.vendedor.usuario, contrasenaDe('vendedor'));
      await expect(page).toHaveURL(/\/productos$/);
      await paginaProductos.esperarCarga();
    });
  });
});
