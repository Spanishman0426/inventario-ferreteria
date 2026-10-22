/**
 * Nombre: TerceroFormDialog.test.jsx
 * Descripción: Pruebas del formulario dinámico de tercero: campos según el tipo de persona, dígito de
 *              verificación en vivo y corregible, casillas de rol según permisos, 409 bajo la
 *              identificación y modo solo lectura (RF-TER-001, RN-26, RNF-INT-002).
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProveedoresApp from '../../ProveedoresApp.jsx';
import useAuth from '../../hooks/useAuth.js';
import ApiError from '../../services/ApiError.js';
import { actualizarTercero, crearTercero } from '../../services/tercerosService.js';
import TerceroFormDialog from './TerceroFormDialog.jsx';

vi.mock('../../hooks/useAuth.js', () => ({ default: vi.fn() }));

vi.mock('../../services/tercerosService.js', () => ({
  PERMISO_CLIENTES: 'TERCERO_CLIENTE_EDITAR',
  PERMISO_PROVEEDORES: 'TERCERO_PROVEEDOR_EDITAR',
  crearTercero: vi.fn(),
  actualizarTercero: vi.fn(),
}));

/** Permisos de cada rol de la matriz 2.2 relevantes para terceros. */
const PERMISOS = {
  admin: ['TERCERO_CLIENTE_EDITAR', 'TERCERO_PROVEEDOR_EDITAR'],
  vendedor: ['TERCERO_CLIENTE_EDITAR'],
  comprador: ['TERCERO_PROVEEDOR_EDITAR'],
};

/** Proveedor jurídico como lo devuelve la API. */
const PROVEEDOR = {
  id: 2,
  tipoPersona: 'JURIDICA',
  tipoIdentificacion: 'NIT',
  numeroIdentificacion: '900123456',
  digitoVerificacion: 8,
  nombre: null,
  apellido: null,
  razonSocial: 'Distribuidora Pavco Neiva S.A.S.',
  nombreMostrado: 'Distribuidora Pavco Neiva S.A.S.',
  direccion: 'Neiva',
  telefono: '3001234567',
  correo: null,
  esCliente: false,
  esProveedor: true,
  esConsumidorFinal: false,
  estado: 'ACTIVO',
};

/**
 * Monta el diálogo con los permisos del rol indicado.
 *
 * @param {keyof typeof PERMISOS} rol rol del usuario
 * @param {object} [props] propiedades del diálogo
 * @returns {{usuario: ReturnType<typeof userEvent.setup>, onGuardado: import('vitest').Mock}} usuario simulado y callback
 */
function montar(rol, props = {}) {
  vi.mocked(useAuth).mockReturnValue({ tienePermiso: (p) => PERMISOS[rol].includes(p) });
  const usuario = userEvent.setup({ delay: null });
  const onGuardado = vi.fn();
  render(
    <ProveedoresApp>
      <TerceroFormDialog abierto onGuardado={onGuardado} onCerrar={vi.fn()} {...props} />
    </ProveedoresApp>,
  );
  return { usuario, onGuardado };
}

/**
 * Elige una opción de un selector de MUI.
 *
 * @param {ReturnType<typeof userEvent.setup>} usuario usuario simulado
 * @param {string} testId data-testid del selector
 * @param {string} opcion texto de la opción
 */
async function elegir(usuario, testId, opcion) {
  await usuario.click(screen.getByTestId(testId));
  await usuario.click(within(await screen.findByRole('listbox')).getByText(opcion));
}

/**
 * Escribe en un campo pegando el texto de una vez: una sola validación de Formik en lugar de una por
 * tecla, para que la prueba no se alargue cuando corren todas en paralelo.
 *
 * @param {ReturnType<typeof userEvent.setup>} usuario usuario simulado
 * @param {string} testId data-testid del campo
 * @param {string} texto texto a escribir
 */
async function escribir(usuario, testId, texto) {
  await usuario.click(screen.getByTestId(testId));
  await usuario.paste(texto);
}

/**
 * Llena los datos de contacto comunes.
 *
 * @param {ReturnType<typeof userEvent.setup>} usuario usuario simulado
 */
async function llenarContacto(usuario) {
  await escribir(usuario, 'tercero-campo-direccion', 'Carrera 5 # 10-30');
  await escribir(usuario, 'tercero-campo-telefono', '3001234567');
}

describe('TerceroFormDialog', () => {
  beforeEach(() => {
    vi.mocked(crearTercero).mockReset();
    vi.mocked(actualizarTercero).mockReset();
  });

  it('[CA-RF-TER-001-1] una persona natural pide cédula, nombre y apellido y se envía sin razón social ni dígito', async () => {
    vi.mocked(crearTercero).mockResolvedValue({ ...PROVEEDOR, id: 9, nombreMostrado: 'Ana Rojas' });
    const { usuario, onGuardado } = montar('vendedor');

    expect(screen.queryByTestId('tercero-campo-razon-social')).not.toBeInTheDocument();
    expect(screen.queryByTestId('tercero-campo-dv')).not.toBeInTheDocument();
    await escribir(usuario, 'tercero-campo-numero', '1075234567');
    await escribir(usuario, 'tercero-campo-nombre', ' Ana ');
    await escribir(usuario, 'tercero-campo-apellido', 'Rojas');
    await llenarContacto(usuario);
    await usuario.click(screen.getByTestId('tercero-guardar'));

    await waitFor(() => expect(onGuardado).toHaveBeenCalled());
    expect(crearTercero).toHaveBeenCalledWith({
      tipoPersona: 'NATURAL',
      tipoIdentificacion: 'CC',
      numeroIdentificacion: '1075234567',
      digitoVerificacion: null,
      nombre: 'Ana',
      apellido: 'Rojas',
      razonSocial: null,
      direccion: 'Carrera 5 # 10-30',
      telefono: '3001234567',
      correo: null,
      esCliente: true,
      esProveedor: false,
    });
  });

  it('[CA-RF-TER-001-1] una persona jurídica cambia a NIT y razón social, y calcula el dígito en vivo', async () => {
    vi.mocked(crearTercero).mockResolvedValue({ ...PROVEEDOR, id: 10 });
    const { usuario } = montar('admin');

    await elegir(usuario, 'tercero-campo-tipo-persona', 'Persona jurídica');
    expect(screen.queryByTestId('tercero-campo-nombre')).not.toBeInTheDocument();
    expect(screen.getByTestId('tercero-campo-razon-social')).toBeInTheDocument();
    expect(screen.getByTestId('tercero-campo-tipo-identificacion')).toHaveTextContent('NIT');

    await escribir(usuario, 'tercero-campo-numero', '890903938');
    expect(screen.getByTestId('tercero-campo-dv')).toHaveValue('8');

    await escribir(usuario, 'tercero-campo-razon-social', 'Bancolombia S.A.');
    await llenarContacto(usuario);
    await usuario.click(screen.getByTestId('tercero-campo-es-proveedor'));
    await usuario.click(screen.getByTestId('tercero-guardar'));

    await waitFor(() => expect(crearTercero).toHaveBeenCalled());
    expect(crearTercero.mock.calls[0][0]).toMatchObject({
      tipoPersona: 'JURIDICA',
      tipoIdentificacion: 'NIT',
      digitoVerificacion: 8,
      razonSocial: 'Bancolombia S.A.',
      nombre: null,
      esCliente: true,
      esProveedor: true,
    });
  });

  it('[CA-RF-TER-001-2] con los dos permisos se puede marcar cliente y proveedor a la vez', async () => {
    const { usuario } = montar('admin');

    await usuario.click(screen.getByTestId('tercero-campo-es-proveedor'));

    expect(screen.getByTestId('tercero-campo-es-cliente')).toBeChecked();
    expect(screen.getByTestId('tercero-campo-es-proveedor')).toBeChecked();
  });

  it('[CA-RF-TER-001-3] un dígito de verificación corregido a mano y erróneo se rechaza sin enviar', async () => {
    const { usuario } = montar('admin');

    await elegir(usuario, 'tercero-campo-tipo-persona', 'Persona jurídica');
    await escribir(usuario, 'tercero-campo-numero', '899999068');
    expect(screen.getByTestId('tercero-campo-dv')).toHaveValue('1');
    await usuario.clear(screen.getByTestId('tercero-campo-dv'));
    await escribir(usuario, 'tercero-campo-dv', '5');
    await escribir(usuario, 'tercero-campo-razon-social', 'Ecopetrol S.A.');
    await llenarContacto(usuario);
    await usuario.click(screen.getByTestId('tercero-guardar'));

    expect(await screen.findByTestId('tercero-error-dv')).toHaveTextContent(
      'No corresponde al NIT; el dígito correcto es 1.',
    );
    expect(crearTercero).not.toHaveBeenCalled();
  });

  it('[CA-RF-TER-001-3] un 409 por identificación duplicada se muestra bajo el número', async () => {
    vi.mocked(crearTercero).mockRejectedValue(
      new ApiError({
        status: 409,
        codigo: 'CONFLICTO_UNICIDAD',
        titulo: 'Registro duplicado',
        detalle: 'Ya existe un tercero con identificación CC 1075234567.',
      }),
    );
    const { usuario, onGuardado } = montar('vendedor');

    await escribir(usuario, 'tercero-campo-numero', '1075234567');
    await escribir(usuario, 'tercero-campo-nombre', 'Juan');
    await escribir(usuario, 'tercero-campo-apellido', 'Pérez');
    await llenarContacto(usuario);
    await usuario.click(screen.getByTestId('tercero-guardar'));

    expect(await screen.findByTestId('tercero-error-numero')).toHaveTextContent(
      'Ya existe un tercero con identificación CC 1075234567.',
    );
    expect(onGuardado).not.toHaveBeenCalled();
  });

  it('[CA-RNF-INT-002-1] enviar vacío muestra la causa en cada campo obligatorio', async () => {
    const { usuario } = montar('admin');

    await usuario.click(screen.getByTestId('tercero-campo-es-cliente'));
    await escribir(usuario, 'tercero-campo-telefono', '123');
    await usuario.click(screen.getByTestId('tercero-guardar'));

    expect(await screen.findByTestId('tercero-error-numero')).toHaveTextContent(
      'Este campo es obligatorio.',
    );
    expect(screen.getByTestId('tercero-error-nombre')).toHaveTextContent(
      'Este campo es obligatorio.',
    );
    expect(screen.getByTestId('tercero-error-telefono')).toHaveTextContent(
      'Debe tener entre 7 y 10 dígitos.',
    );
    expect(screen.getByTestId('tercero-error-roles')).toHaveTextContent(
      'Marque al menos un rol: cliente o proveedor.',
    );
    expect(crearTercero).not.toHaveBeenCalled();
  });

  it('[CA-RF-SEG-001-2] un vendedor no puede marcar Proveedor; un comprador no puede marcar Cliente', () => {
    montar('vendedor');
    expect(screen.getByTestId('tercero-campo-es-proveedor')).toBeDisabled();
    expect(screen.getByTestId('tercero-campo-es-cliente')).toBeEnabled();
    expect(screen.getByTestId('tercero-aviso-rol')).toHaveTextContent(
      'Su rol no puede asignar el rol Proveedor.',
    );
  });

  it('[CA-RF-SEG-001-2] un comprador crea proveedores: la casilla Proveedor viene marcada y Cliente deshabilitada', () => {
    montar('comprador');
    expect(screen.getByTestId('tercero-campo-es-proveedor')).toBeChecked();
    expect(screen.getByTestId('tercero-campo-es-cliente')).toBeDisabled();
  });

  it('[CA-RF-SEG-001-2] un vendedor abre un proveedor en solo lectura, con el motivo y sin botón Guardar', () => {
    montar('vendedor', { tercero: PROVEEDOR });

    expect(screen.getByTestId('tercero-solo-lectura')).toHaveTextContent(
      'Solo puede consultar este tercero: es proveedor y su rol no tiene permiso para editar terceros de ese tipo.',
    );
    expect(screen.getByTestId('tercero-campo-razon-social')).toBeDisabled();
    expect(screen.queryByTestId('tercero-guardar')).not.toBeInTheDocument();
  });

  it('[CA-RF-TER-001-4] el Consumidor final se abre en solo lectura aun para el administrador', () => {
    montar('admin', {
      tercero: {
        ...PROVEEDOR,
        id: 1,
        tipoPersona: 'NATURAL',
        tipoIdentificacion: 'CC',
        numeroIdentificacion: '222222222222',
        digitoVerificacion: null,
        nombre: 'Consumidor',
        apellido: 'final',
        razonSocial: null,
        esCliente: true,
        esProveedor: false,
        esConsumidorFinal: true,
      },
    });

    expect(screen.getByTestId('tercero-solo-lectura')).toHaveTextContent(
      'El Consumidor final viene predefinido en el sistema y no se puede editar ni desactivar.',
    );
    expect(screen.getByTestId('tercero-campo-nombre')).toBeDisabled();
    expect(screen.queryByTestId('tercero-guardar')).not.toBeInTheDocument();
  });

  it('editar un proveedor como comprador envía PUT con su id', async () => {
    vi.mocked(actualizarTercero).mockResolvedValue(PROVEEDOR);
    const { usuario, onGuardado } = montar('comprador', { tercero: PROVEEDOR });

    expect(screen.getByTestId('tercero-campo-dv')).toHaveValue('8');
    await usuario.clear(screen.getByTestId('tercero-campo-telefono'));
    await escribir(usuario, 'tercero-campo-telefono', '6088712345');
    await usuario.click(screen.getByTestId('tercero-guardar'));

    await waitFor(() => expect(onGuardado).toHaveBeenCalled());
    expect(actualizarTercero).toHaveBeenCalledWith(
      2,
      expect.objectContaining({ telefono: '6088712345' }),
    );
  });
});
