/**
 * Nombre: terceros.spec.js
 * Descripción: Escenarios E2E-13 a E2E-17 de clientes y proveedores (RF-TER-001) con Playwright: crear
 *              persona natural, crear persona jurídica con NIT y dígito correcto, rechazar un dígito
 *              incorrecto, identificación duplicada y un vendedor que no puede marcar Proveedor.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { estadoDe, iniciarSesionApi } from '../../comun/api.js';
import { TERCEROS, TEXTOS, cedulaUnica, nitUnico, peticionJuridica } from '../../comun/datos.js';
import { expect, test } from './fixtures.js';

/** Prefijo de los datos que crea esta herramienta. */
const HERRAMIENTA = 'PW';

/** Textos esperados en español. */
const T = TEXTOS.es.terceros;

test.describe('Clientes y proveedores', () => {
  test('E2E-13 [CA-RF-TER-001-1] [CA-RF-TER-001-4] el vendedor registra una persona natural con cédula y el Consumidor final no tiene acciones', async ({
    paginaLogin,
    paginaProductos,
    paginaTerceros,
    barra,
  }) => {
    const cedula = cedulaUnica(HERRAMIENTA);
    await paginaLogin.iniciarSesionComo('vendedor');
    await paginaProductos.esperarCarga();

    await test.step('abre Terceros desde el menú', async () => {
      await barra.menuTerceros.click();
      await expect(paginaTerceros.titulo).toHaveText(T.titulo);
      await paginaTerceros.esperarCarga();
    });

    await test.step('registra la persona natural con cédula, nombre y apellido', async () => {
      await paginaTerceros.nuevo();
      await expect(paginaTerceros.campo('razon-social')).toHaveCount(0);
      await expect(paginaTerceros.campo('dv')).toHaveCount(0);
      await paginaTerceros.campo('numero').fill(cedula);
      await paginaTerceros.campo('nombre').fill('Ana');
      await paginaTerceros.campo('apellido').fill(`Rojas ${HERRAMIENTA}`);
      await paginaTerceros.llenarContacto();
      await paginaTerceros.botonGuardar.click();
      await expect(paginaTerceros.formulario).toBeHidden();
      await expect(paginaTerceros.notificacion).toHaveText(T.creado(`Ana Rojas ${HERRAMIENTA}`));
    });

    await test.step('la busca por cédula y aparece como cliente', async () => {
      await paginaTerceros.buscar(cedula);
      const fila = paginaTerceros.fila(cedula);
      await expect(fila).toHaveCount(1);
      await expect(fila).toContainText(`CC ${cedula}`);
      await expect(fila).toContainText(T.cliente);
    });

    await test.step('el Consumidor final aparece predefinido, sin editar ni desactivar', async () => {
      const id = TERCEROS.consumidorFinal.id;
      await paginaTerceros.buscar(TERCEROS.consumidorFinal.cc);
      await expect(paginaTerceros.deFila('predefinido', id)).toHaveText(T.predefinido);
      await expect(paginaTerceros.deFila('editar', id)).toHaveCount(0);
      await expect(paginaTerceros.deFila('desactivar', id)).toHaveCount(0);
      await paginaTerceros.deFila('ver', id).click();
      await expect(paginaTerceros.soloLectura).toBeVisible();
      await expect(paginaTerceros.botonGuardar).toHaveCount(0);
    });
  });

  test('E2E-14 [CA-RF-TER-001-1] [CA-RF-TER-001-2] el administrador registra una persona jurídica con NIT, dígito calculado en vivo y los dos roles', async ({
    page,
    paginaLogin,
    paginaProductos,
    paginaTerceros,
  }) => {
    const { nit, dv } = nitUnico(HERRAMIENTA);
    const razonSocial = `Ferretería Mayorista ${HERRAMIENTA} ${nit} S.A.S.`;
    await paginaLogin.iniciarSesionComo('admin');
    await paginaProductos.esperarCarga();
    await page.goto('/terceros');
    await paginaTerceros.esperarCarga();

    await test.step('al elegir persona jurídica cambian los campos y el dígito se calcula al escribir el NIT', async () => {
      await paginaTerceros.nuevo();
      await paginaTerceros.elegirTipoPersona(T.personaJuridica);
      await expect(paginaTerceros.campo('nombre')).toHaveCount(0);
      await expect(page.getByTestId('tercero-campo-tipo-identificacion')).toHaveText('NIT');
      await paginaTerceros.campo('numero').fill(nit);
      await expect(paginaTerceros.campo('dv')).toHaveValue(String(dv));
    });

    await test.step('lo registra como cliente y proveedor', async () => {
      await paginaTerceros.campo('razon-social').fill(razonSocial);
      await paginaTerceros.llenarContacto();
      await expect(paginaTerceros.casillaCliente).toBeChecked();
      await paginaTerceros.marcarRol('es-proveedor');
      await expect(paginaTerceros.casillaProveedor).toBeChecked();
      await paginaTerceros.botonGuardar.click();
      await expect(paginaTerceros.formulario).toBeHidden();
      await expect(paginaTerceros.notificacion).toHaveText(T.creado(razonSocial));
    });

    await test.step('aparece con NIT y dígito, y con los dos roles', async () => {
      await paginaTerceros.buscar(nit);
      const fila = paginaTerceros.fila(nit);
      await expect(fila).toContainText(`NIT ${nit}-${dv}`);
      await expect(fila).toContainText(T.cliente);
      await expect(fila).toContainText(T.proveedor);
    });
  });

  test('E2E-15 [CA-RF-TER-001-3] un NIT con dígito de verificación incorrecto se rechaza en el formulario y en la API', async ({
    page,
    paginaLogin,
    paginaProductos,
    paginaTerceros,
  }) => {
    const { nit, dv } = nitUnico(HERRAMIENTA);
    const incorrecto = (dv + 1) % 10;
    await paginaLogin.iniciarSesionComo('admin');
    await paginaProductos.esperarCarga();
    await page.goto('/terceros');
    await paginaTerceros.esperarCarga();

    await test.step('el usuario corrige el dígito con uno erróneo y el formulario lo rechaza sin guardar', async () => {
      await paginaTerceros.nuevo();
      await paginaTerceros.elegirTipoPersona(T.personaJuridica);
      await paginaTerceros.campo('numero').fill(nit);
      await paginaTerceros.campo('dv').fill(String(incorrecto));
      await paginaTerceros.campo('razon-social').fill(`Dígito erróneo ${HERRAMIENTA} ${nit}`);
      await paginaTerceros.llenarContacto();
      await paginaTerceros.botonGuardar.click();
      await expect(paginaTerceros.error('dv')).toHaveText(T.digitoIncorrecto(dv));
      await expect(paginaTerceros.formulario).toBeVisible();
    });

    await test.step('la API también lo rechaza (400) y no lo registra', async () => {
      const token = await iniciarSesionApi('admin');
      const body = peticionJuridica({ nit, dv: incorrecto, razonSocial: `Dígito erróneo ${nit}` });
      expect(await estadoDe('/api/v1/terceros', { method: 'POST', token, body })).toBe(400);
      await paginaTerceros.botonCancelar.click();
      await paginaTerceros.buscar(nit);
      await expect(paginaTerceros.fila(nit)).toHaveCount(0);
    });
  });

  test('E2E-16 [CA-RF-TER-001-3] una identificación duplicada muestra la causa bajo el número de identificación', async ({
    page,
    paginaLogin,
    paginaProductos,
    paginaTerceros,
  }) => {
    await paginaLogin.iniciarSesionComo('vendedor');
    await paginaProductos.esperarCarga();
    await page.goto('/terceros');
    await paginaTerceros.esperarCarga();

    await paginaTerceros.nuevo();
    await paginaTerceros.campo('numero').fill(TERCEROS.juanPerez.cc);
    await paginaTerceros.campo('nombre').fill('Juan');
    await paginaTerceros.campo('apellido').fill('Duplicado');
    await paginaTerceros.llenarContacto();
    await paginaTerceros.botonGuardar.click();

    await expect(paginaTerceros.error('numero')).toHaveText(
      T.duplicado('CC', TERCEROS.juanPerez.cc),
    );
    await expect(paginaTerceros.formulario).toBeVisible();
  });

  test('E2E-17 [CA-RF-SEG-001-2] el vendedor no puede marcar Proveedor ni editar un proveedor', async ({
    page,
    paginaLogin,
    paginaProductos,
    paginaTerceros,
  }) => {
    const { pavco } = TERCEROS;
    await paginaLogin.iniciarSesionComo('vendedor');
    await paginaProductos.esperarCarga();
    await page.goto('/terceros');
    await paginaTerceros.esperarCarga();

    await test.step('en un tercero nuevo, la casilla Proveedor está deshabilitada con el motivo', async () => {
      await paginaTerceros.nuevo();
      await expect(paginaTerceros.casillaProveedor).toBeDisabled();
      await expect(paginaTerceros.casillaCliente).toBeEnabled();
      await expect(paginaTerceros.avisoRol).toHaveText(T.sinPermisoProveedor);
      await paginaTerceros.botonCancelar.click();
      await expect(paginaTerceros.formulario).toBeHidden();
    });

    await test.step('un proveedor existente solo se puede ver, en solo lectura', async () => {
      await paginaTerceros.buscar(pavco.nit);
      await expect(paginaTerceros.deFila('editar', pavco.id)).toHaveCount(0);
      await expect(paginaTerceros.deFila('desactivar', pavco.id)).toHaveCount(0);
      await paginaTerceros.deFila('ver', pavco.id).click();
      await expect(paginaTerceros.soloLectura).toHaveText(T.soloLecturaProveedor);
      await expect(paginaTerceros.campo('razon-social')).toBeDisabled();
      await expect(paginaTerceros.botonGuardar).toHaveCount(0);
    });

    await test.step('la API le niega registrar un proveedor y desactivar uno existente (403)', async () => {
      const token = await iniciarSesionApi('vendedor');
      const { nit, dv } = nitUnico(HERRAMIENTA);
      const body = peticionJuridica({ nit, dv, razonSocial: `Proveedor no permitido ${nit}` });
      expect(await estadoDe('/api/v1/terceros', { method: 'POST', token, body })).toBe(403);
      expect(
        await estadoDe(`/api/v1/terceros/${pavco.id}/estado`, {
          method: 'PATCH',
          token,
          body: { estado: 'INACTIVO' },
        }),
      ).toBe(403);
    });
  });
});
