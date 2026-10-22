/**
 * Nombre: CatalogosPage.test.jsx
 * Descripción: Pruebas de la administración de marcas y categorías con el servicio simulado: pestañas,
 *              búsqueda y filtro de estado, alta, desactivación y activación con confirmación, y acceso
 *              restringido a CONFIG_GESTIONAR (RF-CFG-001, RN-27).
 * Fecha de creación: 2026-10-21
 * Última actualización: 2026-10-21
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { obtenerUsuarioActual } from '../services/authService.js';
import {
  buscarElementosCatalogo,
  cambiarEstadoElementoCatalogo,
  crearElementoCatalogo,
} from '../services/catalogosService.js';
import { renderizarApp } from '../test/renderizar.jsx';
import { guardarSesion } from '../utils/sesionStorage.js';

vi.mock('../services/authService.js', () => ({
  iniciarSesion: vi.fn(),
  obtenerUsuarioActual: vi.fn(),
}));
vi.mock('../services/catalogosService.js', async (importOriginal) => ({
  ...(await importOriginal()),
  buscarElementosCatalogo: vi.fn(),
  crearElementoCatalogo: vi.fn(),
  actualizarElementoCatalogo: vi.fn(),
  cambiarEstadoElementoCatalogo: vi.fn(),
}));

/** Administrador: único rol con CONFIG_GESTIONAR (matriz 2.2). */
const ADMIN = {
  id: 2,
  nombreCompleto: 'Ana Administradora',
  roles: ['ADMIN'],
  permisos: ['CATALOGO_CONSULTAR', 'CATALOGO_EDITAR', 'CONFIG_GESTIONAR'],
};

/** Jefe de inventario: edita productos pero no administra catálogos. */
const JEFE = {
  id: 4,
  nombreCompleto: 'Julia Jefe Inventario',
  roles: ['JEFE_INVENTARIO'],
  permisos: ['CATALOGO_CONSULTAR', 'CATALOGO_EDITAR'],
};

/** Marcas de la página simulada: una activa y una inactiva. */
const MARCAS = [
  { id: 1, nombre: 'Stanley', estado: 'ACTIVO' },
  { id: 7, nombre: 'Descontinuada', estado: 'INACTIVO' },
];

/**
 * Página de resultados simulada.
 *
 * @param {object[]} contenido elementos de la página
 * @returns {object} página
 */
function pagina(contenido) {
  return { contenido, pagina: 0, tamano: 20, totalElementos: contenido.length, totalPaginas: 1 };
}

/**
 * Inicia una sesión simulada y abre una ruta.
 *
 * @param {object} usuarioSesion usuario devuelto por /auth/me
 * @param {string} [ruta] ruta inicial
 * @returns {ReturnType<typeof renderizarApp>} resultado del render
 */
function abrirComo(usuarioSesion, ruta = '/catalogos') {
  guardarSesion('token-prueba', 1800);
  vi.mocked(obtenerUsuarioActual).mockResolvedValue(usuarioSesion);
  return renderizarApp(ruta);
}

/**
 * Última consulta enviada al servicio de búsqueda.
 *
 * @returns {[string, object]} tipo y filtro de la última llamada
 */
function ultimaConsulta() {
  return vi.mocked(buscarElementosCatalogo).mock.lastCall;
}

/**
 * Espera la fila de una marca. La primera carga de la página puede tardar más que el tiempo de espera
 * general cuando todas las pruebas corren en paralelo.
 *
 * @param {number} id id de la marca
 * @returns {Promise<HTMLElement>} fila
 */
function filaMarca(id) {
  return screen.findByTestId(`catalogo-marca-fila-${id}`, {}, { timeout: 20000 });
}

describe('CatalogosPage', () => {
  beforeEach(() => {
    vi.mocked(obtenerUsuarioActual).mockReset();
    vi.mocked(buscarElementosCatalogo).mockReset();
    vi.mocked(buscarElementosCatalogo).mockImplementation((tipo) =>
      Promise.resolve(
        pagina(tipo === 'marcas' ? MARCAS : [{ id: 3, nombre: 'Plomería', estado: 'ACTIVO' }]),
      ),
    );
    vi.mocked(crearElementoCatalogo).mockReset();
    vi.mocked(cambiarEstadoElementoCatalogo).mockReset();
  });

  it('muestra las marcas con su estado y cambia a categorías con la pestaña', async () => {
    const { usuario } = abrirComo(ADMIN);

    expect(await filaMarca(1)).toHaveTextContent('Stanley');
    expect(screen.getByTestId('catalogo-marca-estado-1')).toHaveTextContent('Activo');
    expect(screen.getByTestId('catalogo-marca-estado-7')).toHaveTextContent('Inactivo');
    expect(ultimaConsulta()).toEqual(['marcas', { q: '', estado: '', page: 0, size: 20 }]);

    await usuario.click(screen.getByTestId('catalogos-pestana-categorias'));

    expect(await screen.findByTestId('catalogo-categoria-fila-3')).toHaveTextContent('Plomería');
    expect(ultimaConsulta()[0]).toBe('categorias');
  });

  it('busca por nombre recortado y filtra por estado desde la primera página', async () => {
    const { usuario } = abrirComo(ADMIN);
    await filaMarca(1);

    await usuario.type(screen.getByTestId('catalogo-marcas-buscar'), '  stan  ');
    await usuario.click(screen.getByTestId('catalogo-marcas-buscar-boton'));
    await waitFor(() => expect(ultimaConsulta()[1]).toMatchObject({ q: 'stan', page: 0 }));

    await usuario.click(screen.getByTestId('catalogo-marcas-filtro-estado'));
    await usuario.click(
      within(await screen.findByRole('listbox')).getByRole('option', { name: 'Inactivo' }),
    );
    await waitFor(() =>
      expect(ultimaConsulta()[1]).toMatchObject({ q: 'stan', estado: 'INACTIVO', page: 0 }),
    );
  });

  it('sin resultados muestra el aviso', async () => {
    vi.mocked(buscarElementosCatalogo).mockResolvedValue(pagina([]));
    abrirComo(ADMIN);

    expect(await screen.findByTestId('catalogo-marcas-sin-resultados')).toHaveTextContent(
      'No hay marcas que coincidan con la búsqueda.',
    );
  });

  it('crear una marca abre el formulario, la registra y recarga la lista', async () => {
    vi.mocked(crearElementoCatalogo).mockResolvedValue({
      id: 30,
      nombre: 'Bosch',
      estado: 'ACTIVO',
    });
    const { usuario } = abrirComo(ADMIN);
    await filaMarca(1);
    const consultasAntes = vi.mocked(buscarElementosCatalogo).mock.calls.length;

    await usuario.click(screen.getByTestId('catalogo-marcas-nuevo'));
    await usuario.type(await screen.findByTestId('catalogo-campo-nombre'), 'Bosch');
    await usuario.click(screen.getByTestId('catalogo-guardar'));

    await waitFor(() =>
      expect(crearElementoCatalogo).toHaveBeenCalledWith('marcas', { nombre: 'Bosch' }),
    );
    await waitFor(() =>
      expect(vi.mocked(buscarElementosCatalogo).mock.calls.length).toBeGreaterThan(consultasAntes),
    );
    expect(screen.queryByTestId('catalogo-form')).not.toBeInTheDocument();
  });

  it('[CA-RF-CFG-001-3] no ofrece eliminar; desactivar pide confirmación y cancelar no cambia nada', async () => {
    const { usuario } = abrirComo(ADMIN);
    const fila = await filaMarca(1);
    expect(
      within(fila).queryByRole('button', { name: /eliminar|borrar/i }),
    ).not.toBeInTheDocument();

    await usuario.click(screen.getByTestId('catalogo-marca-desactivar-1'));
    const dialogo = await screen.findByTestId('catalogo-marca-estado-confirmacion-dialogo');
    expect(dialogo).toHaveTextContent('¿Desactivar la marca?');
    expect(dialogo).toHaveTextContent('Los productos que ya la tienen la conservan.');
    await usuario.click(screen.getByTestId('catalogo-marca-estado-confirmacion-cancelar'));

    expect(cambiarEstadoElementoCatalogo).not.toHaveBeenCalled();
  });

  it('[CA-RF-CFG-001-3] confirmar la desactivación la envía como INACTIVO y notifica', async () => {
    vi.mocked(cambiarEstadoElementoCatalogo).mockResolvedValue({
      ...MARCAS[0],
      estado: 'INACTIVO',
    });
    const { usuario } = abrirComo(ADMIN);
    await filaMarca(1);

    await usuario.click(screen.getByTestId('catalogo-marca-desactivar-1'));
    await usuario.click(await screen.findByTestId('catalogo-marca-estado-confirmacion-aceptar'));

    await waitFor(() =>
      expect(cambiarEstadoElementoCatalogo).toHaveBeenCalledWith('marcas', 1, 'INACTIVO'),
    );
    expect(await screen.findByText('Marca «Stanley» desactivada.')).toBeVisible();
  });

  it('una marca inactiva se activa con confirmación', async () => {
    vi.mocked(cambiarEstadoElementoCatalogo).mockResolvedValue({ ...MARCAS[1], estado: 'ACTIVO' });
    const { usuario } = abrirComo(ADMIN);
    await filaMarca(7);
    expect(screen.queryByTestId('catalogo-marca-desactivar-7')).not.toBeInTheDocument();

    await usuario.click(screen.getByTestId('catalogo-marca-activar-7'));
    expect(
      await screen.findByTestId('catalogo-marca-estado-confirmacion-dialogo'),
    ).toHaveTextContent('¿Activar la marca?');
    await usuario.click(screen.getByTestId('catalogo-marca-estado-confirmacion-aceptar'));

    await waitFor(() =>
      expect(cambiarEstadoElementoCatalogo).toHaveBeenCalledWith('marcas', 7, 'ACTIVO'),
    );
  });

  it('[CA-RF-SEG-001-2] sin CONFIG_GESTIONAR la ruta muestra acceso denegado y no consulta', async () => {
    abrirComo(JEFE);

    expect(await screen.findByTestId('acceso-denegado')).toBeVisible();
    expect(screen.queryByTestId('catalogos-titulo')).not.toBeInTheDocument();
    expect(buscarElementosCatalogo).not.toHaveBeenCalled();
  });

  it('[CA-RF-SEG-001-2] el menú muestra Catálogos solo con CONFIG_GESTIONAR', async () => {
    const { unmount } = abrirComo(ADMIN);
    expect(await screen.findByTestId('menu-catalogos')).toHaveTextContent('Catálogos');
    expect(screen.getByTestId('menu-productos')).toHaveTextContent('Productos');
    unmount();

    abrirComo(JEFE);
    expect(await screen.findByTestId('menu-productos')).toBeVisible();
    expect(screen.queryByTestId('menu-catalogos')).not.toBeInTheDocument();
  });
});
