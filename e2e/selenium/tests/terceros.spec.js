/**
 * Nombre: terceros.spec.js
 * Descripción: Escenarios E2E-13 a E2E-17 de clientes y proveedores (RF-TER-001) con Selenium WebDriver y
 *              Mocha: crear persona natural, crear persona jurídica con NIT y dígito correcto, rechazar un
 *              dígito incorrecto, identificación duplicada y un vendedor que no puede marcar Proveedor.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import assert from 'node:assert/strict';

import { estadoDe, iniciarSesionApi } from '../../comun/api.js';
import { BASE_URL } from '../../comun/entorno.js';
import {
  TERCEROS as DATOS,
  TEXTOS,
  cedulaUnica,
  nitUnico,
  peticionJuridica,
} from '../../comun/datos.js';
import { BARRA } from '../pages/BarraSuperior.js';
import { PaginaLogin } from '../pages/PaginaLogin.js';
import { PaginaProductos } from '../pages/PaginaProductos.js';
import { PaginaTerceros, TERCEROS } from '../pages/PaginaTerceros.js';
import {
  clic,
  contar,
  esperarAusente,
  esperarQueContenga,
  esperarTexto,
  esperarVisible,
} from '../support/esperas.js';
import { escenario, usarNavegador } from '../support/navegador.js';

/** Prefijo de los datos que crea esta herramienta. */
const HERRAMIENTA = 'SE';

/** Textos esperados en español. */
const T = TEXTOS.es.terceros;

/**
 * Inicia sesión y abre la página de terceros.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @param {'admin'|'vendedor'} rol usuario de prueba
 * @returns {Promise<PaginaTerceros>} página lista
 */
async function abrirTerceros(driver, rol) {
  await new PaginaLogin(driver).iniciarSesionComo(rol);
  await new PaginaProductos(driver).esperarCarga();
  await driver.get(`${BASE_URL}/terceros`);
  const pagina = new PaginaTerceros(driver);
  await pagina.esperarCarga();
  return pagina;
}

describe('Clientes y proveedores', function () {
  const navegador = usarNavegador();

  escenario(
    navegador,
    'E2E-13 [CA-RF-TER-001-1] [CA-RF-TER-001-4] el vendedor registra una persona natural con cédula y el Consumidor final no tiene acciones',
    async (driver) => {
      const cedula = cedulaUnica(HERRAMIENTA);
      await new PaginaLogin(driver).iniciarSesionComo('vendedor');
      await new PaginaProductos(driver).esperarCarga();

      // Abre Terceros desde el menú.
      await clic(driver, BARRA.menuTerceros);
      await esperarTexto(driver, TERCEROS.titulo, T.titulo);
      const terceros = new PaginaTerceros(driver);
      await terceros.esperarCarga();

      // Registra la persona natural con cédula, nombre y apellido.
      await terceros.nuevo();
      assert.equal(await contar(driver, TERCEROS.campo('razon-social')), 0, 'sin razón social');
      assert.equal(await contar(driver, TERCEROS.campo('dv')), 0, 'sin dígito con cédula');
      await terceros.escribirCampo('numero', cedula);
      await terceros.escribirCampo('nombre', 'Ana');
      await terceros.escribirCampo('apellido', `Rojas ${HERRAMIENTA}`);
      await terceros.llenarContacto();
      await clic(driver, TERCEROS.guardar);
      await esperarAusente(driver, TERCEROS.formulario);
      await esperarTexto(driver, TERCEROS.notificacion, T.creado(`Ana Rojas ${HERRAMIENTA}`));

      // La busca por cédula y aparece como cliente.
      await terceros.buscar(cedula);
      const id = await terceros.idDe(cedula);
      await esperarTexto(driver, TERCEROS.deFila('identificacion', id), `CC ${cedula}`);
      await esperarVisible(driver, TERCEROS.deFila('rol-cliente', id));

      // El Consumidor final aparece predefinido, sin editar ni desactivar.
      const consumidor = DATOS.consumidorFinal.id;
      await terceros.buscar(DATOS.consumidorFinal.cc);
      await esperarTexto(driver, TERCEROS.deFila('predefinido', consumidor), T.predefinido);
      assert.equal(await contar(driver, TERCEROS.deFila('editar', consumidor)), 0, 'sin editar');
      assert.equal(
        await contar(driver, TERCEROS.deFila('desactivar', consumidor)),
        0,
        'sin desactivar',
      );
      await clic(driver, TERCEROS.deFila('ver', consumidor));
      await esperarVisible(driver, TERCEROS.soloLectura);
      assert.equal(await contar(driver, TERCEROS.guardar), 0, 'sin botón Guardar');
    },
  );

  escenario(
    navegador,
    'E2E-14 [CA-RF-TER-001-1] [CA-RF-TER-001-2] el administrador registra una persona jurídica con NIT, dígito calculado en vivo y los dos roles',
    async (driver) => {
      const { nit, dv } = nitUnico(HERRAMIENTA);
      const razonSocial = `Ferretería Mayorista ${HERRAMIENTA} ${nit} S.A.S.`;
      const terceros = await abrirTerceros(driver, 'admin');

      // Al elegir persona jurídica cambian los campos y el dígito se calcula al escribir el NIT.
      await terceros.nuevo();
      await terceros.elegirTipoPersona(T.personaJuridica);
      assert.equal(await contar(driver, TERCEROS.campo('nombre')), 0, 'sin nombre en jurídica');
      await esperarQueContenga(driver, TERCEROS.tipoIdentificacion, 'NIT');
      await terceros.escribirCampo('numero', nit);
      assert.equal(await terceros.valorDe('dv'), String(dv), 'dígito calculado en vivo');

      // Lo registra como cliente y proveedor.
      await terceros.escribirCampo('razon-social', razonSocial);
      await terceros.llenarContacto();
      assert.ok(await (await terceros.casilla('es-cliente')).isSelected(), 'cliente marcado');
      await clic(driver, TERCEROS.opcionRol('es-proveedor'));
      assert.ok(await (await terceros.casilla('es-proveedor')).isSelected(), 'proveedor marcado');
      await clic(driver, TERCEROS.guardar);
      await esperarAusente(driver, TERCEROS.formulario);
      await esperarTexto(driver, TERCEROS.notificacion, T.creado(razonSocial));

      // Aparece con NIT y dígito, y con los dos roles.
      await terceros.buscar(nit);
      const id = await terceros.idDe(nit);
      await esperarTexto(driver, TERCEROS.deFila('identificacion', id), `NIT ${nit}-${dv}`);
      await esperarVisible(driver, TERCEROS.deFila('rol-cliente', id));
      await esperarVisible(driver, TERCEROS.deFila('rol-proveedor', id));
    },
  );

  escenario(
    navegador,
    'E2E-15 [CA-RF-TER-001-3] un NIT con dígito de verificación incorrecto se rechaza en el formulario y en la API',
    async (driver) => {
      const { nit, dv } = nitUnico(HERRAMIENTA);
      const incorrecto = (dv + 1) % 10;
      const terceros = await abrirTerceros(driver, 'admin');

      // El usuario corrige el dígito con uno erróneo y el formulario lo rechaza sin guardar.
      await terceros.nuevo();
      await terceros.elegirTipoPersona(T.personaJuridica);
      await terceros.escribirCampo('numero', nit);
      await terceros.escribirCampo('dv', String(incorrecto));
      await terceros.escribirCampo('razon-social', `Dígito erróneo ${HERRAMIENTA} ${nit}`);
      await terceros.llenarContacto();
      await clic(driver, TERCEROS.guardar);
      await esperarTexto(driver, TERCEROS.error('dv'), T.digitoIncorrecto(dv));
      await esperarVisible(driver, TERCEROS.formulario);

      // La API también lo rechaza (400) y no lo registra.
      const token = await iniciarSesionApi('admin');
      const body = peticionJuridica({ nit, dv: incorrecto, razonSocial: `Dígito erróneo ${nit}` });
      assert.equal(await estadoDe('/api/v1/terceros', { method: 'POST', token, body }), 400);
      await clic(driver, TERCEROS.cancelar);
      await esperarAusente(driver, TERCEROS.formulario);
      await terceros.buscar(nit);
      await esperarVisible(driver, TERCEROS.sinResultados);
    },
  );

  escenario(
    navegador,
    'E2E-16 [CA-RF-TER-001-3] una identificación duplicada muestra la causa bajo el número de identificación',
    async (driver) => {
      const terceros = await abrirTerceros(driver, 'vendedor');

      await terceros.nuevo();
      await terceros.escribirCampo('numero', DATOS.juanPerez.cc);
      await terceros.escribirCampo('nombre', 'Juan');
      await terceros.escribirCampo('apellido', 'Duplicado');
      await terceros.llenarContacto();
      await clic(driver, TERCEROS.guardar);

      await esperarTexto(driver, TERCEROS.error('numero'), T.duplicado('CC', DATOS.juanPerez.cc));
      await esperarVisible(driver, TERCEROS.formulario);
    },
  );

  escenario(
    navegador,
    'E2E-17 [CA-RF-SEG-001-2] el vendedor no puede marcar Proveedor ni editar un proveedor',
    async (driver) => {
      const { pavco } = DATOS;
      const terceros = await abrirTerceros(driver, 'vendedor');

      // En un tercero nuevo, la casilla Proveedor está deshabilitada con el motivo.
      await terceros.nuevo();
      assert.equal(
        await (await terceros.casilla('es-proveedor')).isEnabled(),
        false,
        'Proveedor deshabilitado',
      );
      assert.equal(
        await (await terceros.casilla('es-cliente')).isEnabled(),
        true,
        'Cliente habilitado',
      );
      await esperarTexto(driver, TERCEROS.avisoRol, T.sinPermisoProveedor);
      await clic(driver, TERCEROS.cancelar);
      await esperarAusente(driver, TERCEROS.formulario);

      // Un proveedor existente solo se puede ver, en solo lectura.
      await terceros.buscar(pavco.nit);
      await esperarVisible(driver, TERCEROS.deFila('ver', pavco.id));
      assert.equal(await contar(driver, TERCEROS.deFila('editar', pavco.id)), 0, 'sin editar');
      assert.equal(
        await contar(driver, TERCEROS.deFila('desactivar', pavco.id)),
        0,
        'sin desactivar',
      );
      await clic(driver, TERCEROS.deFila('ver', pavco.id));
      await esperarTexto(driver, TERCEROS.soloLectura, T.soloLecturaProveedor);
      assert.equal(
        await (await driver.findElement(TERCEROS.campo('razon-social'))).isEnabled(),
        false,
        'razón social deshabilitada',
      );
      assert.equal(await contar(driver, TERCEROS.guardar), 0, 'sin botón Guardar');

      // La API le niega registrar un proveedor y desactivar uno existente (403).
      const token = await iniciarSesionApi('vendedor');
      const { nit, dv } = nitUnico(HERRAMIENTA);
      const body = peticionJuridica({ nit, dv, razonSocial: `Proveedor no permitido ${nit}` });
      assert.equal(await estadoDe('/api/v1/terceros', { method: 'POST', token, body }), 403);
      assert.equal(
        await estadoDe(`/api/v1/terceros/${pavco.id}/estado`, {
          method: 'PATCH',
          token,
          body: { estado: 'INACTIVO' },
        }),
        403,
      );
    },
  );
});
