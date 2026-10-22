/**
 * Nombre: catalogos.spec.js
 * Descripción: Escenarios E2E-11 (el administrador crea una marca, aparece en el formulario de producto y
 *              al desactivarla deja de aparecer) y E2E-12 (el vendedor no ve el menú Catálogos) con
 *              Playwright.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { estadoDe, iniciarSesionApi } from '../../comun/api.js';
import { TEXTOS, nombreMarcaUnico } from '../../comun/datos.js';
import { expect, test } from './fixtures.js';

/** Prefijo de las marcas que crea esta herramienta. */
const HERRAMIENTA = 'PW';

test.describe('Administración de catálogos', () => {
  test('E2E-11 [CA-RF-CFG-001-3] [CA-RF-CFG-001-D1] el administrador crea una marca, aparece en el formulario de producto y al desactivarla deja de aparecer', async ({
    page,
    paginaLogin,
    paginaProductos,
    paginaCatalogos,
    barra,
  }) => {
    const marca = nombreMarcaUnico(HERRAMIENTA);
    const opcionMarca = () =>
      page.getByRole('listbox').getByRole('option', { name: marca, exact: true });

    /**
     * Abre el selector de marca de un producto nuevo y comprueba si ofrece la marca.
     *
     * @param {number} cantidad 1 si debe aparecer; 0 si no
     * @returns {Promise<void>}
     */
    const revisarSelectorDeMarca = async (cantidad) => {
      await barra.menuProductos.click();
      await paginaProductos.esperarCarga();
      const formulario = await paginaProductos.nuevo();
      await page.getByTestId('producto-campo-marca').click();
      await expect(page.getByRole('listbox')).toBeVisible();
      await expect(opcionMarca()).toHaveCount(cantidad);
      await page.keyboard.press('Escape');
      await expect(page.getByRole('listbox')).toBeHidden();
      await formulario.botonCancelar.click();
      await expect(formulario.formulario).toBeHidden();
    };

    await paginaLogin.iniciarSesionComo('admin');
    await paginaProductos.esperarCarga();

    await test.step('crea la marca desde el menú Catálogos', async () => {
      await barra.menuCatalogos.click();
      await expect(paginaCatalogos.titulo).toHaveText(TEXTOS.es.catalogos.titulo);
      await paginaCatalogos.esperarCarga('marcas');
      await paginaCatalogos.crear('marcas', marca);
      await expect(paginaCatalogos.notificacion).toHaveText(TEXTOS.es.catalogos.marcaCreada(marca));
      await paginaCatalogos.buscar('marcas', marca);
      await expect(paginaCatalogos.estado('marcas', marca)).toHaveText(TEXTOS.es.estadoActivo);
    });

    await test.step('la marca aparece en el selector del formulario de producto', async () => {
      await revisarSelectorDeMarca(1);
    });

    await test.step('la desactiva con confirmación: cancelar no cambia nada; confirmar la deja Inactiva', async () => {
      await barra.menuCatalogos.click();
      await paginaCatalogos.esperarCarga('marcas');
      await paginaCatalogos.buscar('marcas', marca);
      const dialogo = await paginaCatalogos.desactivar('marcas', marca, false);
      await expect(dialogo).toBeHidden();
      await expect(paginaCatalogos.estado('marcas', marca)).toHaveText(TEXTOS.es.estadoActivo);

      await paginaCatalogos.desactivar('marcas', marca, true);
      await expect(paginaCatalogos.notificacion).toHaveText(
        TEXTOS.es.catalogos.marcaDesactivada(marca),
      );
      await expect(paginaCatalogos.estado('marcas', marca)).toHaveText(TEXTOS.es.estadoInactivo);
    });

    await test.step('la marca desactivada sigue registrada pero ya no aparece al crear un producto', async () => {
      await revisarSelectorDeMarca(0);
    });
  });

  test('E2E-12 [CA-RF-SEG-001-2] el vendedor no ve el menú Catálogos ni puede administrarlos', async ({
    page,
    paginaLogin,
    paginaProductos,
    paginaCatalogos,
    barra,
  }) => {
    await paginaLogin.iniciarSesionComo('vendedor');
    await paginaProductos.esperarCarga();

    await test.step('el menú muestra Productos pero no Catálogos', async () => {
      await expect(barra.menuProductos).toBeVisible();
      await expect(barra.menuCatalogos).toHaveCount(0);
    });

    await test.step('abrir /catalogos directamente muestra acceso denegado', async () => {
      await page.goto('/catalogos');
      await expect(page.getByTestId('acceso-denegado')).toBeVisible();
      await expect(paginaCatalogos.titulo).toHaveCount(0);
    });

    await test.step('la API le niega crear y desactivar marcas (403)', async () => {
      const token = await iniciarSesionApi('vendedor');
      const body = { nombre: nombreMarcaUnico(HERRAMIENTA) };
      expect(await estadoDe('/api/v1/marcas', { method: 'POST', token, body })).toBe(403);
      expect(
        await estadoDe('/api/v1/marcas/1/estado', {
          method: 'PATCH',
          token,
          body: { estado: 'INACTIVO' },
        }),
      ).toBe(403);
    });
  });
});
