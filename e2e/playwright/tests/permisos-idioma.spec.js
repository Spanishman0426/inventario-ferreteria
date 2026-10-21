/**
 * Nombre: permisos-idioma.spec.js
 * Descripción: Escenarios E2E-08 (el vendedor no ve acciones de edición) y E2E-10 (cambio de idioma de
 *              la interfaz) con Playwright.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { estadoDe, iniciarSesionApi } from '../../comun/api.js';
import { MARTILLO, TEXTOS, nombreUnico, peticionProducto } from '../../comun/datos.js';
import { expect, test } from './fixtures.js';

test.describe('Permisos por rol', () => {
  test('E2E-08 [CA-RF-SEG-001-2] [CA-RF-PRO-003-3] el vendedor no ve acciones de edición', async ({
    paginaLogin,
    paginaProductos,
  }) => {
    await paginaLogin.iniciarSesionComo('vendedor');
    await paginaProductos.esperarCarga();

    await test.step('consulta el catálogo sin botones de crear, editar ni desactivar', async () => {
      await paginaProductos.buscar(MARTILLO.nombre);
      await expect(paginaProductos.fila(MARTILLO.nombre)).toBeVisible();
      await expect(paginaProductos.botonNuevo).toHaveCount(0);
      await expect(paginaProductos.botonesEditar).toHaveCount(0);
      await expect(paginaProductos.botonesDesactivar).toHaveCount(0);
    });

    await test.step('no ve la columna de costos', async () => {
      await expect(paginaProductos.columnaCosto).toHaveCount(0);
      await expect(
        paginaProductos.fila(MARTILLO.nombre).getByTestId(/^producto-costo-/),
      ).toHaveCount(0);
    });

    await test.step('la API también le niega crear y desactivar (403)', async () => {
      const token = await iniciarSesionApi('vendedor');
      const body = peticionProducto(nombreUnico('PW', 'Prohibido'));
      expect(await estadoDe('/api/v1/productos', { method: 'POST', token, body })).toBe(403);
      expect(await estadoDe('/api/v1/productos/1', { method: 'DELETE', token })).toBe(403);
    });
  });
});

test.describe('Internacionalización', () => {
  test('E2E-10 [CA-RNF-INT-003-D1] [CA-RNF-INT-003-1] cambio de idioma de la interfaz', async ({
    page,
    paginaLogin,
    paginaProductos,
    barra,
  }) => {
    await test.step('la interfaz arranca en español', async () => {
      await paginaLogin.abrir();
      await expect(paginaLogin.titulo).toHaveText(TEXTOS.es.loginTitulo);
      await expect(page.locator('html')).toHaveAttribute('lang', 'es');
    });

    await test.step('moneda con formato colombiano', async () => {
      await paginaLogin.iniciarSesionComo('admin');
      await paginaProductos.buscar(MARTILLO.nombre);
      await expect(
        paginaProductos.fila(MARTILLO.nombre).getByTestId(/^producto-costo-/),
      ).toHaveText(MARTILLO.costoFormateado);
    });

    await test.step('al elegir inglés cambian los textos', async () => {
      await barra.elegirIdioma('en');
      await expect(paginaProductos.titulo).toHaveText(TEXTOS.en.productosTitulo);
      await expect(paginaProductos.botonNuevo).toHaveText(TEXTOS.en.nuevoProducto);
      await expect(barra.menuProductos).toHaveText(TEXTOS.en.menuProductos);
      await expect(paginaProductos.estado(MARTILLO.nombre)).toHaveText(TEXTOS.en.estadoActivo);
      await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    });

    await test.step('la elección se conserva al recargar y al cerrar sesión', async () => {
      await page.reload();
      await expect(paginaProductos.titulo).toHaveText(TEXTOS.en.productosTitulo);
      await barra.cerrarSesion();
      await expect(paginaLogin.titulo).toHaveText(TEXTOS.en.loginTitulo);
    });

    await test.step('se puede volver a español', async () => {
      await barra.elegirIdioma('es');
      await expect(paginaLogin.titulo).toHaveText(TEXTOS.es.loginTitulo);
    });
  });
});
