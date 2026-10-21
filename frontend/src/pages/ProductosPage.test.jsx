/**
 * Nombre: ProductosPage.test.jsx
 * Descripción: Pruebas de la gestión de productos con el servicio simulado: listado, búsqueda,
 *              filtros, paginación del servidor, estado como chip, desactivación con confirmación
 *              y acciones ocultas sin CATALOGO_EDITAR (RF-PRO-001, RF-PRO-003, RN-27).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { obtenerUsuarioActual } from '../services/authService.js';
import { listarCategorias } from '../services/catalogosService.js';
import { inactivarProducto, listarProductos } from '../services/productosService.js';
import { renderizarApp } from '../test/renderizar.jsx';
import { guardarSesion } from '../utils/sesionStorage.js';

vi.mock('../services/authService.js', () => ({
  iniciarSesion: vi.fn(),
  obtenerUsuarioActual: vi.fn(),
}));
vi.mock('../services/productosService.js', () => ({
  listarProductos: vi.fn(),
  inactivarProducto: vi.fn(),
  crearProducto: vi.fn(),
  actualizarProducto: vi.fn(),
}));
vi.mock('../services/catalogosService.js', () => ({
  listarCategorias: vi.fn(),
  listarMarcas: () => Promise.resolve([]),
  listarUnidadesMedida: () => Promise.resolve([]),
  listarTarifasIva: () => Promise.resolve([]),
}));

/** Jefe de inventario: consulta, edita catálogo y ve costos. */
const JEFE = {
  id: 3,
  nombreCompleto: 'Juliana Jefe',
  roles: ['JEFE_INVENTARIO'],
  permisos: ['CATALOGO_CONSULTAR', 'CATALOGO_EDITAR', 'COSTOS_VER'],
};

/** Vendedor: solo consulta, sin costos. */
const VENDEDOR = {
  id: 5,
  nombreCompleto: 'Valeria Vendedora',
  roles: ['VENDEDOR'],
  permisos: ['CATALOGO_CONSULTAR'],
};

/** Productos de la página simulada: uno activo y uno inactivo. */
const PRODUCTOS = [
  {
    id: 1,
    nombre: 'Martillo de uña 16 oz',
    estado: 'ACTIVO',
    categoria: { id: 1, nombre: 'Herramientas' },
    marca: { id: 10, nombre: 'Stanley' },
    unidadBase: { codigo: 'UND', nombre: 'Unidad' },
    tarifaIva: { id: 3, nombre: 'IVA 19 %', porcentaje: 19 },
    porcentajeIncremento: 30,
    control: 'GARANTIA',
    plazoGarantiaMeses: 12,
    ultimoCostoBase: 18000,
  },
  {
    id: 2,
    nombre: 'Cemento gris 50 kg',
    estado: 'INACTIVO',
    categoria: { id: 2, nombre: 'Construcción' },
    marca: { id: 11, nombre: 'Argos' },
    unidadBase: { codigo: 'KG', nombre: 'Kilogramo' },
    tarifaIva: { id: 3, nombre: 'IVA 19 %', porcentaje: 19 },
    porcentajeIncremento: 20,
    control: 'NINGUNO',
    plazoGarantiaMeses: null,
    ultimoCostoBase: 650,
  },
];

/**
 * Página de resultados simulada.
 *
 * @param {object[]} contenido productos de la página
 * @param {number} [totalElementos] total informado por el servidor
 * @returns {object} página
 */
function pagina(contenido, totalElementos = contenido.length) {
  return { contenido, pagina: 0, tamano: 20, totalElementos, totalPaginas: 1 };
}

/**
 * Inicia una sesión simulada y abre la página de productos.
 *
 * @param {object} usuarioSesion usuario devuelto por /auth/me
 * @returns {Promise<ReturnType<typeof renderizarApp>>} resultado del render con la tabla cargada
 */
async function abrirComo(usuarioSesion) {
  guardarSesion('token-prueba', 1800);
  vi.mocked(obtenerUsuarioActual).mockResolvedValue(usuarioSesion);
  const resultado = renderizarApp('/productos');
  await screen.findByTestId('producto-fila-1');
  return resultado;
}

/**
 * Última consulta enviada al servicio de productos.
 *
 * @returns {object} filtro de la última llamada
 */
function ultimaConsulta() {
  return vi.mocked(listarProductos).mock.lastCall[0];
}

describe('ProductosPage', () => {
  beforeEach(() => {
    vi.mocked(obtenerUsuarioActual).mockReset();
    vi.mocked(listarProductos).mockReset();
    vi.mocked(listarProductos).mockResolvedValue(pagina(PRODUCTOS));
    vi.mocked(inactivarProducto).mockReset();
    vi.mocked(listarCategorias).mockReset();
    vi.mocked(listarCategorias).mockResolvedValue([
      { id: 1, nombre: 'Herramientas' },
      { id: 2, nombre: 'Construcción' },
    ]);
  });

  it('[CA-RF-PRO-001-1] lista los productos del servicio con el estado como chip', async () => {
    await abrirComo(JEFE);

    const fila = screen.getByTestId('producto-fila-1');
    expect(within(fila).getByText('Martillo de uña 16 oz')).toBeInTheDocument();
    expect(within(fila).getByText('Stanley')).toBeInTheDocument();
    expect(screen.getByTestId('producto-estado-1')).toHaveTextContent('Activo');
    expect(screen.getByTestId('producto-estado-2')).toHaveTextContent('Inactivo');
    expect(listarProductos).toHaveBeenCalledWith(
      expect.objectContaining({ q: '', page: 0, size: 20 }),
    );
  });

  it('busca por nombre desde la primera página', async () => {
    const { usuario } = await abrirComo(JEFE);

    await usuario.type(screen.getByTestId('productos-buscar'), '  martillo ');
    await usuario.click(screen.getByTestId('productos-buscar-boton'));

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ q: 'martillo', page: 0 }));
  });

  it('filtra por categoría y por estado', async () => {
    const { usuario } = await abrirComo(JEFE);

    await usuario.click(screen.getByTestId('productos-filtro-categoria'));
    await usuario.click(await screen.findByRole('option', { name: 'Construcción' }));
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ categoriaId: 2, page: 0 }));

    await usuario.click(screen.getByTestId('productos-filtro-estado'));
    await usuario.click(await screen.findByRole('option', { name: 'Inactivo' }));
    await waitFor(() =>
      expect(ultimaConsulta()).toMatchObject({ categoriaId: 2, estado: 'INACTIVO', page: 0 }),
    );
  });

  it('pide al servidor la página siguiente y cambia el tamaño de página', async () => {
    vi.mocked(listarProductos).mockResolvedValue(pagina(PRODUCTOS, 45));
    const { usuario } = await abrirComo(JEFE);

    await usuario.click(screen.getByRole('button', { name: /siguiente|next/i }));
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ page: 1, size: 20 }));

    await usuario.click(screen.getByRole('combobox', { name: /filas|rows/i }));
    await usuario.click(await screen.findByRole('option', { name: '50' }));
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ page: 0, size: 50 }));
  });

  it('muestra un aviso cuando no hay resultados', async () => {
    vi.mocked(listarProductos).mockResolvedValue(pagina([]));
    guardarSesion('token-prueba', 1800);
    vi.mocked(obtenerUsuarioActual).mockResolvedValue(JEFE);

    renderizarApp('/productos');

    expect(await screen.findByTestId('productos-sin-resultados')).toHaveTextContent(
      'No hay productos que coincidan con la búsqueda.',
    );
  });

  it('desactivar pide confirmación y solo al confirmar llama al servicio', async () => {
    vi.mocked(inactivarProducto).mockResolvedValue(undefined);
    const { usuario } = await abrirComo(JEFE);

    await usuario.click(screen.getByTestId('producto-desactivar-1'));
    const dialogo = await screen.findByTestId('producto-desactivar-confirmacion-dialogo');
    expect(dialogo).toHaveTextContent('¿Desactivar el producto?');
    expect(dialogo).toHaveTextContent('Martillo de uña 16 oz');

    await usuario.click(screen.getByTestId('producto-desactivar-confirmacion-cancelar'));
    expect(inactivarProducto).not.toHaveBeenCalled();

    const consultasAntes = vi.mocked(listarProductos).mock.calls.length;
    await usuario.click(screen.getByTestId('producto-desactivar-1'));
    await usuario.click(await screen.findByTestId('producto-desactivar-confirmacion-aceptar'));

    await waitFor(() => expect(inactivarProducto).toHaveBeenCalledWith(1));
    expect(await screen.findByText('Producto «Martillo de uña 16 oz» desactivado.')).toBeVisible();
    await waitFor(() =>
      expect(vi.mocked(listarProductos).mock.calls.length).toBeGreaterThan(consultasAntes),
    );
  });

  it('un producto inactivo no ofrece desactivar, pero sí editar', async () => {
    await abrirComo(JEFE);

    expect(screen.queryByTestId('producto-desactivar-2')).not.toBeInTheDocument();
    expect(screen.getByTestId('producto-editar-2')).toBeInTheDocument();
  });

  it('crear y editar abren el formulario', async () => {
    const { usuario } = await abrirComo(JEFE);

    await usuario.click(screen.getByTestId('productos-nuevo'));
    expect(await screen.findByRole('heading', { name: 'Nuevo producto' })).toBeInTheDocument();
    await usuario.click(screen.getByTestId('producto-cancelar'));
    await waitFor(() => expect(screen.queryByTestId('producto-form')).not.toBeInTheDocument());

    await usuario.click(screen.getByTestId('producto-editar-1'));
    expect(await screen.findByRole('heading', { name: 'Editar producto' })).toBeInTheDocument();
    expect(screen.getByTestId('producto-campo-nombre')).toHaveValue('Martillo de uña 16 oz');
  });

  it('[CA-RF-SEG-001-2] sin CATALOGO_EDITAR no se muestran crear, editar ni desactivar', async () => {
    await abrirComo(VENDEDOR);

    expect(screen.queryByTestId('productos-nuevo')).not.toBeInTheDocument();
    expect(screen.queryByTestId('producto-editar-1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('producto-desactivar-1')).not.toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: 'Acciones' })).not.toBeInTheDocument();
  });

  it('[CA-RF-PRO-003-3] un vendedor no ve la columna de costo', async () => {
    await abrirComo(VENDEDOR);
    expect(
      screen.queryByRole('columnheader', { name: 'Último costo (unidad base)' }),
    ).not.toBeInTheDocument();
  });

  it('[CA-RF-PRO-003-3] con COSTOS_VER se muestra la columna de costo', async () => {
    await abrirComo(JEFE);
    expect(
      screen.getByRole('columnheader', { name: 'Último costo (unidad base)' }),
    ).toBeInTheDocument();
  });
});
