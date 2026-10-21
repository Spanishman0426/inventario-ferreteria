/**
 * Nombre: productos.spec.js
 * Descripción: Escenarios E2E-02 a E2E-07 de la gestión de productos con Selenium WebDriver y Mocha:
 *              crear, validar, editar, buscar y filtrar, desactivar con confirmación y rechazar duplicados.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import assert from 'node:assert/strict';

import { crearProductoApi, iniciarSesionApi, inactivarProductoApi } from '../../comun/api.js';
import { CATALOGO, LIMITES, TEXTOS, nombreUnico, prefijoUnico } from '../../comun/datos.js';
import { FORMULARIO } from '../pages/FormularioProducto.js';
import { PaginaLogin } from '../pages/PaginaLogin.js';
import { PRODUCTOS, PaginaProductos } from '../pages/PaginaProductos.js';
import {
  contar,
  esperarAusente,
  esperarCantidad,
  esperarQueContenga,
  esperarTexto,
  esperarTextos,
  esperarVisible,
} from '../support/esperas.js';
import { escenario, usarNavegador } from '../support/navegador.js';

/** Prefijo de los productos que crea esta herramienta. */
const HERRAMIENTA = 'SE';

/**
 * Inicia sesión como jefe de inventario y devuelve la página de productos cargada.
 *
 * @param {import('selenium-webdriver').WebDriver} driver navegador
 * @returns {Promise<PaginaProductos>} página de productos
 */
async function entrarComoJefe(driver) {
  await new PaginaLogin(driver).iniciarSesionComo('jefe');
  const pagina = new PaginaProductos(driver);
  await pagina.esperarCarga();
  return pagina;
}

describe('Gestión de productos (jefe de inventario)', function () {
  const navegador = usarNavegador();

  escenario(navegador, 'E2E-02 [CA-RF-PRO-001-1] crear producto', async (driver) => {
    const productos = await entrarComoJefe(driver);
    const nombre = nombreUnico(HERRAMIENTA, 'Taladro percutor');

    const formulario = await productos.nuevo();
    await formulario.llenar({
      nombre,
      descripcion: 'Creado por la prueba E2E-02',
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
      unidad: CATALOGO.unidadUnd,
      iva: CATALOGO.ivaGeneral,
      porcentaje: '30',
      control: 'Garantía',
      plazo: '12',
    });
    await formulario.guardar();

    await esperarTexto(driver, PRODUCTOS.notificacion, TEXTOS.es.creado(nombre));
    await esperarAusente(driver, FORMULARIO.formulario);

    // El producto registrado puede consultarse.
    await productos.buscar(nombre);
    await esperarCantidad(driver, PRODUCTOS.filas, 1);
    const id = await productos.idDe(nombre);
    const fila = PRODUCTOS.deProducto('fila', id);
    await esperarQueContenga(driver, fila, CATALOGO.marcas.stanley);
    await esperarQueContenga(driver, fila, CATALOGO.categorias.herramientas);
    await esperarQueContenga(driver, fila, 'UND');
    await esperarQueContenga(driver, fila, '19 %');
    await esperarTexto(driver, PRODUCTOS.deProducto('estado', id), TEXTOS.es.estadoActivo);
  });

  escenario(
    navegador,
    'E2E-03 [CA-RF-PRO-001-2] [CA-RNF-INT-002-1] validaciones del formulario',
    async (driver) => {
      const productos = await entrarComoJefe(driver);
      const formulario = await productos.nuevo();

      // Campos vacíos: cada obligatorio muestra su campo y su causa.
      await formulario.guardar();
      for (const [campo, etiqueta] of Object.entries(TEXTOS.es.etiquetas)) {
        await esperarQueContenga(driver, FORMULARIO.etiqueta(campo), etiqueta);
        await esperarTexto(driver, FORMULARIO.error(campo), TEXTOS.es.requerido);
      }
      await esperarVisible(driver, FORMULARIO.formulario);

      // Longitudes: nombre corto, nombre y descripción largos, porcentaje fuera de rango.
      await formulario.llenar({ nombre: 'ab', porcentaje: '501' });
      await formulario.guardar();
      await esperarTexto(driver, FORMULARIO.error('nombre'), TEXTOS.es.minimo3);
      await esperarTexto(driver, FORMULARIO.error('porcentaje'), TEXTOS.es.rangoPorcentaje);

      await formulario.llenar({ nombre: 'N'.repeat(LIMITES.nombreMax + 10) });
      assert.equal(await formulario.valor('nombre'), 'N'.repeat(LIMITES.nombreMax));
      await formulario.llenar({ descripcion: 'D'.repeat(LIMITES.descripcionMax + 10) });
      assert.equal(await formulario.valor('descripcion'), 'D'.repeat(LIMITES.descripcionMax));
      // Sin error, MUI no pinta el texto de ayuda del campo.
      await esperarAusente(driver, FORMULARIO.error('nombre'));

      // Garantía sin plazo: el plazo es obligatorio.
      await formulario.llenar({
        categoria: CATALOGO.categorias.herramientas,
        marca: CATALOGO.marcas.stanley,
        unidad: CATALOGO.unidadUnd,
        iva: CATALOGO.ivaGeneral,
        porcentaje: '30',
        control: 'Garantía',
      });
      await formulario.guardar();
      await esperarTexto(driver, FORMULARIO.error('plazo'), TEXTOS.es.requerido);
      await esperarVisible(driver, FORMULARIO.formulario);
      assert.equal(
        await contar(driver, PRODUCTOS.notificacion),
        0,
        'no debe haber respuesta del servidor',
      );
    },
  );

  escenario(navegador, 'E2E-04 [CA-RF-PRO-001-D1] editar producto', async (driver) => {
    const token = await iniciarSesionApi('jefe');
    const original = nombreUnico(HERRAMIENTA, 'Llave inglesa');
    await crearProductoApi(token, {
      nombre: original,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
    });
    const editado = nombreUnico(HERRAMIENTA, 'Llave ajustable 10 pulgadas');
    const productos = await entrarComoJefe(driver);

    await productos.buscar(original);
    const formulario = await productos.editar(await productos.idDe(original));
    assert.equal(await formulario.valor('nombre'), original);
    await formulario.llenar({ nombre: editado, marca: CATALOGO.marcas.truper, porcentaje: '45' });
    await formulario.guardar();

    await esperarTexto(driver, PRODUCTOS.notificacion, TEXTOS.es.actualizado(editado));
    await productos.buscar(editado);
    await esperarCantidad(driver, PRODUCTOS.filas, 1);
    const id = await productos.idDe(editado);
    await esperarQueContenga(driver, PRODUCTOS.deProducto('fila', id), CATALOGO.marcas.truper);
    await productos.buscar(original);
    await esperarVisible(driver, PRODUCTOS.sinResultados);
  });

  escenario(
    navegador,
    'E2E-05 [CA-RF-PRO-003-D1] buscar por nombre y filtrar por categoría y estado',
    async (driver) => {
      const token = await iniciarSesionApi('jefe');
      const prefijo = prefijoUnico(HERRAMIENTA);
      const alfa = `${prefijo} Alfa`;
      const beta = `${prefijo} Beta`;
      const gamma = `${prefijo} Gamma`;
      await crearProductoApi(token, {
        nombre: alfa,
        categoria: CATALOGO.categorias.herramientas,
        marca: CATALOGO.marcas.stanley,
      });
      await crearProductoApi(token, {
        nombre: beta,
        categoria: CATALOGO.categorias.plomeria,
        marca: CATALOGO.marcas.pavco,
      });
      const productoGamma = await crearProductoApi(token, {
        nombre: gamma,
        categoria: CATALOGO.categorias.herramientas,
        marca: CATALOGO.marcas.truper,
      });
      await inactivarProductoApi(token, productoGamma.id);
      const productos = await entrarComoJefe(driver);

      // La búsqueda por nombre, sin distinguir mayúsculas, trae los tres.
      await productos.buscar(prefijo.toLowerCase());
      await esperarTextos(driver, PRODUCTOS.nombres, [alfa, beta, gamma]);

      // Filtro por categoría Herramientas.
      await productos.filtrar('categoria', CATALOGO.categorias.herramientas);
      await esperarTextos(driver, PRODUCTOS.nombres, [alfa, gamma]);

      // Categoría Herramientas y estado Activo.
      await productos.filtrar('estado', TEXTOS.es.estadoActivo);
      await esperarTextos(driver, PRODUCTOS.nombres, [alfa]);

      // Categoría Herramientas y estado Inactivo.
      await productos.filtrar('estado', TEXTOS.es.estadoInactivo);
      await esperarTextos(driver, PRODUCTOS.nombres, [gamma]);
      await esperarTexto(
        driver,
        PRODUCTOS.deProducto('estado', String(productoGamma.id)),
        TEXTOS.es.estadoInactivo,
      );

      // Categoría Plomería y estado Activo.
      await productos.filtrar('categoria', CATALOGO.categorias.plomeria);
      await productos.filtrar('estado', TEXTOS.es.estadoActivo);
      await esperarTextos(driver, PRODUCTOS.nombres, [beta]);

      // Un texto sin coincidencias muestra el aviso de sin resultados.
      await productos.buscar(`${prefijo} inexistente`);
      await esperarCantidad(driver, PRODUCTOS.filas, 0);
      await esperarVisible(driver, PRODUCTOS.sinResultados);
    },
  );

  escenario(navegador, 'E2E-06 [CA-RF-CFG-001-3] desactivar con confirmación', async (driver) => {
    const token = await iniciarSesionApi('jefe');
    const nombre = nombreUnico(HERRAMIENTA, 'Destornillador estrella');
    const creado = await crearProductoApi(token, {
      nombre,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
    });
    const id = String(creado.id);
    const productos = await entrarComoJefe(driver);
    await productos.buscar(nombre);
    await productos.idDe(nombre);

    // Cancelar la confirmación no cambia el producto.
    await productos.pedirDesactivar(id);
    await esperarQueContenga(driver, PRODUCTOS.dialogoDesactivar, nombre);
    await productos.responderDesactivar(false);
    await esperarTexto(driver, PRODUCTOS.deProducto('estado', id), TEXTOS.es.estadoActivo);

    // Confirmar lo desactiva sin eliminarlo.
    await productos.pedirDesactivar(id);
    await productos.responderDesactivar(true);
    await esperarTexto(driver, PRODUCTOS.notificacion, TEXTOS.es.desactivado(nombre));
    await productos.esperarCarga();
    await esperarTexto(driver, PRODUCTOS.deProducto('estado', id), TEXTOS.es.estadoInactivo);
    await esperarAusente(driver, PRODUCTOS.deProducto('desactivar', id));
    await esperarVisible(driver, PRODUCTOS.deProducto('editar', id));
  });

  escenario(navegador, 'E2E-07 [CA-RF-PRO-001-3] producto duplicado', async (driver) => {
    const token = await iniciarSesionApi('jefe');
    const nombre = nombreUnico(HERRAMIENTA, 'Alicate universal');
    await crearProductoApi(token, {
      nombre,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
    });
    const productos = await entrarComoJefe(driver);

    const formulario = await productos.nuevo();
    await formulario.llenar({
      nombre,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
      unidad: CATALOGO.unidadUnd,
      iva: CATALOGO.ivaGeneral,
      porcentaje: '30',
    });
    await formulario.guardar();

    // El mismo nombre con la misma marca se rechaza indicando la causa.
    await esperarTexto(
      driver,
      FORMULARIO.error('nombre'),
      TEXTOS.es.duplicado(nombre, CATALOGO.marcas.stanley),
    );
    await esperarVisible(driver, FORMULARIO.formulario);

    // El mismo nombre con otra marca sí se registra.
    await formulario.llenar({ marca: CATALOGO.marcas.truper });
    await formulario.guardar();
    await esperarTexto(driver, PRODUCTOS.notificacion, TEXTOS.es.creado(nombre));
    await productos.buscar(nombre);
    await esperarCantidad(driver, PRODUCTOS.filas, 2);
  });
});
