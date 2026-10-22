/**
 * Nombre: TercerosPage.test.jsx
 * Descripción: Pruebas de la página de clientes y proveedores: acceso y menú con cualquiera de los dos
 *              permisos de terceros, filtros, acciones según los roles del tercero, Consumidor final sin
 *              acciones y confirmación al desactivar (RF-TER-001, RF-SEG-001).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { obtenerUsuarioActual } from '../services/authService.js';
import { buscarTerceros, cambiarEstadoTercero } from '../services/tercerosService.js';
import { renderizarApp } from '../test/renderizar.jsx';
import { guardarSesion } from '../utils/sesionStorage.js';

vi.mock('../services/authService.js', () => ({
  iniciarSesion: vi.fn(),
  obtenerUsuarioActual: vi.fn(),
}));
vi.mock('../services/tercerosService.js', async (importOriginal) => ({
  ...(await importOriginal()),
  buscarTerceros: vi.fn(),
  cambiarEstadoTercero: vi.fn(),
  crearTercero: vi.fn(),
  actualizarTercero: vi.fn(),
}));

/** Consumidor final sembrado por el esquema. */
const CONSUMIDOR_FINAL = {
  id: 1,
  tipoPersona: 'NATURAL',
  tipoIdentificacion: 'CC',
  numeroIdentificacion: '222222222222',
  digitoVerificacion: null,
  nombre: 'Consumidor',
  apellido: 'final',
  razonSocial: null,
  nombreMostrado: 'Consumidor final',
  direccion: 'No aplica',
  telefono: '0000000',
  correo: null,
  esCliente: true,
  esProveedor: false,
  esConsumidorFinal: true,
  estado: 'ACTIVO',
};

/** Proveedor jurídico. */
const PAVCO = {
  ...CONSUMIDOR_FINAL,
  id: 2,
  tipoPersona: 'JURIDICA',
  tipoIdentificacion: 'NIT',
  numeroIdentificacion: '900123456',
  digitoVerificacion: 8,
  nombre: null,
  apellido: null,
  razonSocial: 'Distribuidora Pavco Neiva S.A.S.',
  nombreMostrado: 'Distribuidora Pavco Neiva S.A.S.',
  telefono: '3001234567',
  esCliente: false,
  esProveedor: true,
  esConsumidorFinal: false,
};

/** Cliente persona natural. */
const JUAN = {
  ...CONSUMIDOR_FINAL,
  id: 5,
  numeroIdentificacion: '1075234567',
  nombre: 'Juan',
  apellido: 'Pérez Gómez',
  nombreMostrado: 'Juan Pérez Gómez',
  telefono: '3157654321',
  esConsumidorFinal: false,
};

/**
 * Inicia la sesión simulada con los permisos indicados y abre /terceros.
 *
 * @param {string[]} permisos permisos del usuario
 * @returns {ReturnType<typeof renderizarApp>} resultado del render
 */
function abrirComo(permisos) {
  guardarSesion('token', 1800);
  vi.mocked(obtenerUsuarioActual).mockResolvedValue({
    id: 5,
    nombreCompleto: 'Usuario de prueba',
    roles: [],
    permisos: ['CATALOGO_CONSULTAR', ...permisos],
  });
  return renderizarApp('/terceros');
}

describe('TercerosPage', () => {
  beforeEach(() => {
    vi.mocked(obtenerUsuarioActual).mockReset();
    vi.mocked(buscarTerceros).mockReset();
    vi.mocked(cambiarEstadoTercero).mockReset();
    vi.mocked(buscarTerceros).mockResolvedValue({
      contenido: [CONSUMIDOR_FINAL, PAVCO, JUAN],
      totalElementos: 3,
    });
  });

  it('[CA-RF-SEG-001-2] con un solo permiso de terceros (comprador) se ve el menú y la página', async () => {
    abrirComo(['TERCERO_PROVEEDOR_EDITAR']);

    expect(await screen.findByTestId('terceros-titulo')).toHaveTextContent(
      'Clientes y proveedores',
    );
    expect(screen.getByTestId('menu-terceros')).toBeInTheDocument();
    expect(await screen.findByTestId('tercero-identificacion-2')).toHaveTextContent(
      'NIT 900123456-8',
    );
    expect(buscarTerceros).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20 }));
  });

  it('[CA-RF-SEG-001-2] sin permisos de terceros (jefe de inventario) no hay menú ni consulta a la API', async () => {
    abrirComo(['CATALOGO_EDITAR']);

    expect(await screen.findByTestId('acceso-denegado')).toBeInTheDocument();
    expect(screen.queryByTestId('menu-terceros')).not.toBeInTheDocument();
    expect(buscarTerceros).not.toHaveBeenCalled();
  });

  it('[CA-RF-TER-001-4] el Consumidor final se lista como predefinido, solo con la acción de ver', async () => {
    abrirComo(['TERCERO_CLIENTE_EDITAR', 'TERCERO_PROVEEDOR_EDITAR']);

    expect(await screen.findByTestId('tercero-predefinido-1')).toHaveTextContent('Predefinido');
    expect(screen.getByTestId('tercero-ver-1')).toBeInTheDocument();
    expect(screen.queryByTestId('tercero-editar-1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('tercero-desactivar-1')).not.toBeInTheDocument();
  });

  it('[CA-RF-SEG-001-2] un vendedor edita clientes, pero un proveedor solo lo puede ver', async () => {
    const { usuario } = abrirComo(['TERCERO_CLIENTE_EDITAR']);

    expect(await screen.findByTestId('tercero-editar-5')).toBeInTheDocument();
    expect(screen.getByTestId('tercero-desactivar-5')).toBeInTheDocument();
    expect(screen.queryByTestId('tercero-editar-2')).not.toBeInTheDocument();
    expect(screen.queryByTestId('tercero-desactivar-2')).not.toBeInTheDocument();

    await usuario.click(screen.getByTestId('tercero-ver-2'));
    expect(await screen.findByTestId('tercero-solo-lectura')).toBeInTheDocument();
  });

  it('los filtros de rol, tipo de persona y búsqueda llegan al servicio desde la primera página', async () => {
    const { usuario } = abrirComo(['TERCERO_CLIENTE_EDITAR']);
    await screen.findByTestId('tercero-fila-5');

    await usuario.click(screen.getByTestId('terceros-filtro-rol'));
    await usuario.click(within(await screen.findByRole('listbox')).getByText('Proveedor'));
    await usuario.click(screen.getByTestId('terceros-filtro-tipo-persona'));
    await usuario.click(within(await screen.findByRole('listbox')).getByText('Persona jurídica'));
    await usuario.type(screen.getByTestId('terceros-buscar'), '  pavco ');
    await usuario.click(screen.getByTestId('terceros-buscar-boton'));

    await waitFor(() =>
      expect(buscarTerceros).toHaveBeenLastCalledWith(
        expect.objectContaining({ q: 'pavco', rol: 'PROVEEDOR', tipoPersona: 'JURIDICA', page: 0 }),
      ),
    );
  });

  it('desactivar pide confirmación: cancelar no llama al servicio y confirmar envía INACTIVO', async () => {
    vi.mocked(cambiarEstadoTercero).mockResolvedValue({ ...JUAN, estado: 'INACTIVO' });
    const { usuario } = abrirComo(['TERCERO_CLIENTE_EDITAR']);

    await usuario.click(await screen.findByTestId('tercero-desactivar-5'));
    await usuario.click(screen.getByTestId('tercero-estado-confirmacion-cancelar'));
    expect(cambiarEstadoTercero).not.toHaveBeenCalled();

    await usuario.click(screen.getByTestId('tercero-desactivar-5'));
    await usuario.click(screen.getByTestId('tercero-estado-confirmacion-aceptar'));
    await waitFor(() => expect(cambiarEstadoTercero).toHaveBeenCalledWith(5, 'INACTIVO'));
  });
});
