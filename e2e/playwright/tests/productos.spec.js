/**
 * Nombre: productos.spec.js
 * Descripción: Escenarios E2E-02 a E2E-07 de la gestión de productos con Playwright: crear, validar,
 *              editar, buscar y filtrar, desactivar con confirmación y rechazar duplicados.
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { crearProductoApi, iniciarSesionApi, inactivarProductoApi } from '../../comun/api.js';
import { CATALOGO, LIMITES, TEXTOS, nombreUnico, prefijoUnico } from '../../comun/datos.js';
import { expect, test } from './fixtures.js';

/** Prefijo de los productos que crea esta herramienta. */
const HERRAMIENTA = 'PW';

test.describe('Gestión de productos (jefe de inventario)', () => {
  test.beforeEach(async ({ paginaLogin, paginaProductos }) => {
    await paginaLogin.iniciarSesionComo('jefe');
    await paginaProductos.esperarCarga();
  });

  test('E2E-02 [CA-RF-PRO-001-1] crear producto', async ({ paginaProductos }) => {
    const nombre = nombreUnico(HERRAMIENTA, 'Taladro percutor');

    const formulario = await paginaProductos.nuevo();
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

    await expect(paginaProductos.notificacion).toHaveText(TEXTOS.es.creado(nombre));
    await expect(formulario.formulario).toBeHidden();

    await test.step('el producto registrado puede consultarse', async () => {
      await paginaProductos.buscar(nombre);
      const fila = paginaProductos.fila(nombre);
      await expect(paginaProductos.filas).toHaveCount(1);
      await expect(fila).toContainText(CATALOGO.marcas.stanley);
      await expect(fila).toContainText(CATALOGO.categorias.herramientas);
      await expect(fila).toContainText('UND');
      await expect(fila).toContainText('19 %');
      await expect(paginaProductos.estado(nombre)).toHaveText(TEXTOS.es.estadoActivo);
    });
  });

  test('E2E-03 [CA-RF-PRO-001-2] [CA-RNF-INT-002-1] validaciones del formulario', async ({
    page,
    paginaProductos,
  }) => {
    // Ninguna validación del formulario debe llegar al servidor.
    const envios = [];
    page.on('request', (peticion) => {
      if (peticion.method() === 'POST' && peticion.url().includes('/api/v1/productos')) {
        envios.push(peticion.url());
      }
    });
    const formulario = await paginaProductos.nuevo();

    await test.step('campos vacíos: cada obligatorio muestra su campo y su causa', async () => {
      await formulario.guardar();
      for (const [campo, etiqueta] of Object.entries(TEXTOS.es.etiquetas)) {
        await expect(formulario.etiqueta(campo), campo).toContainText(etiqueta);
        await expect(formulario.error(campo), campo).toHaveText(TEXTOS.es.requerido);
      }
      await expect(formulario.formulario).toBeVisible();
    });

    await test.step('longitudes: nombre corto, nombre y descripción largos, porcentaje fuera de rango', async () => {
      await formulario.llenar({ nombre: 'ab', porcentaje: '501' });
      await formulario.guardar();
      await expect(formulario.error('nombre')).toHaveText(TEXTOS.es.minimo3);
      await expect(formulario.error('porcentaje')).toHaveText(TEXTOS.es.rangoPorcentaje);

      await formulario.llenar({ nombre: 'N'.repeat(LIMITES.nombreMax + 10) });
      await expect(formulario.nombre).toHaveValue('N'.repeat(LIMITES.nombreMax));
      await formulario.llenar({ descripcion: 'D'.repeat(LIMITES.descripcionMax + 10) });
      await expect(formulario.descripcion).toHaveValue('D'.repeat(LIMITES.descripcionMax));
      // Sin error, MUI no pinta el texto de ayuda del campo.
      await expect(formulario.error('nombre')).toHaveCount(0);
    });

    await test.step('garantía sin plazo: el plazo es obligatorio', async () => {
      await formulario.llenar({
        categoria: CATALOGO.categorias.herramientas,
        marca: CATALOGO.marcas.stanley,
        unidad: CATALOGO.unidadUnd,
        iva: CATALOGO.ivaGeneral,
        porcentaje: '30',
        control: 'Garantía',
      });
      await formulario.guardar();
      await expect(formulario.error('plazo')).toHaveText(TEXTOS.es.requerido);
      await expect(formulario.formulario).toBeVisible();
    });

    expect(envios, 'el formulario inválido no se envía al servidor').toHaveLength(0);
  });

  test('E2E-04 [CA-RF-PRO-001-D1] editar producto', async ({ paginaProductos }) => {
    const token = await iniciarSesionApi('jefe');
    const original = nombreUnico(HERRAMIENTA, 'Llave inglesa');
    await crearProductoApi(token, {
      nombre: original,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
    });
    const editado = nombreUnico(HERRAMIENTA, 'Llave ajustable 10 pulgadas');

    await paginaProductos.buscar(original);
    const formulario = await paginaProductos.editar(original);
    await expect(formulario.nombre).toHaveValue(original);
    await formulario.llenar({ nombre: editado, marca: CATALOGO.marcas.truper, porcentaje: '45' });
    await formulario.guardar();

    await expect(paginaProductos.notificacion).toHaveText(TEXTOS.es.actualizado(editado));
    await paginaProductos.buscar(editado);
    await expect(paginaProductos.filas).toHaveCount(1);
    await expect(paginaProductos.fila(editado)).toContainText(CATALOGO.marcas.truper);
    await paginaProductos.buscar(original);
    await expect(paginaProductos.sinResultados).toBeVisible();
  });

  test('E2E-05 [CA-RF-PRO-003-D1] buscar por nombre y filtrar por categoría y estado', async ({
    paginaProductos,
  }) => {
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

    await test.step('la búsqueda por nombre, sin distinguir mayúsculas, trae los tres', async () => {
      await paginaProductos.buscar(prefijo.toLowerCase());
      await expect(paginaProductos.nombres).toHaveText([alfa, beta, gamma]);
    });

    await test.step('filtro por categoría Herramientas', async () => {
      await paginaProductos.filtrar('categoria', CATALOGO.categorias.herramientas);
      await expect(paginaProductos.nombres).toHaveText([alfa, gamma]);
    });

    await test.step('categoría Herramientas y estado Activo', async () => {
      await paginaProductos.filtrar('estado', TEXTOS.es.estadoActivo);
      await expect(paginaProductos.nombres).toHaveText([alfa]);
    });

    await test.step('categoría Herramientas y estado Inactivo', async () => {
      await paginaProductos.filtrar('estado', TEXTOS.es.estadoInactivo);
      await expect(paginaProductos.nombres).toHaveText([gamma]);
      await expect(paginaProductos.estado(gamma)).toHaveText(TEXTOS.es.estadoInactivo);
    });

    await test.step('categoría Plomería y estado Activo', async () => {
      await paginaProductos.filtrar('categoria', CATALOGO.categorias.plomeria);
      await paginaProductos.filtrar('estado', TEXTOS.es.estadoActivo);
      await expect(paginaProductos.nombres).toHaveText([beta]);
    });

    await test.step('un texto sin coincidencias muestra el aviso de sin resultados', async () => {
      await paginaProductos.buscar(`${prefijo} inexistente`);
      await expect(paginaProductos.filas).toHaveCount(0);
      await expect(paginaProductos.sinResultados).toBeVisible();
    });
  });

  test('E2E-06 [CA-RF-CFG-001-3] desactivar con confirmación', async ({ paginaProductos }) => {
    const token = await iniciarSesionApi('jefe');
    const nombre = nombreUnico(HERRAMIENTA, 'Destornillador estrella');
    await crearProductoApi(token, {
      nombre,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
    });
    await paginaProductos.buscar(nombre);

    await test.step('cancelar la confirmación no cambia el producto', async () => {
      await paginaProductos.pedirDesactivar(nombre);
      await expect(paginaProductos.dialogoDesactivar).toContainText(nombre);
      await paginaProductos.responderDesactivar(false);
      await expect(paginaProductos.estado(nombre)).toHaveText(TEXTOS.es.estadoActivo);
    });

    await test.step('confirmar lo desactiva sin eliminarlo', async () => {
      await paginaProductos.pedirDesactivar(nombre);
      await paginaProductos.responderDesactivar(true);
      await expect(paginaProductos.notificacion).toHaveText(TEXTOS.es.desactivado(nombre));
      await paginaProductos.esperarCarga();
      await expect(paginaProductos.estado(nombre)).toHaveText(TEXTOS.es.estadoInactivo);
      const fila = paginaProductos.fila(nombre);
      await expect(fila.getByTestId(/^producto-desactivar-\d+$/)).toHaveCount(0);
      await expect(fila.getByTestId(/^producto-editar-/)).toBeVisible();
    });
  });

  test('E2E-07 [CA-RF-PRO-001-3] producto duplicado', async ({ paginaProductos }) => {
    const token = await iniciarSesionApi('jefe');
    const nombre = nombreUnico(HERRAMIENTA, 'Alicate universal');
    await crearProductoApi(token, {
      nombre,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
    });

    const formulario = await paginaProductos.nuevo();
    await formulario.llenar({
      nombre,
      categoria: CATALOGO.categorias.herramientas,
      marca: CATALOGO.marcas.stanley,
      unidad: CATALOGO.unidadUnd,
      iva: CATALOGO.ivaGeneral,
      porcentaje: '30',
    });
    await formulario.guardar();

    await test.step('el mismo nombre con la misma marca se rechaza indicando la causa', async () => {
      await expect(formulario.error('nombre')).toHaveText(
        TEXTOS.es.duplicado(nombre, CATALOGO.marcas.stanley),
      );
      await expect(formulario.formulario).toBeVisible();
    });

    await test.step('el mismo nombre con otra marca sí se registra', async () => {
      await formulario.llenar({ marca: CATALOGO.marcas.truper });
      await formulario.guardar();
      await expect(paginaProductos.notificacion).toHaveText(TEXTOS.es.creado(nombre));
      await paginaProductos.buscar(nombre);
      await expect(paginaProductos.filas).toHaveCount(2);
    });
  });
});
